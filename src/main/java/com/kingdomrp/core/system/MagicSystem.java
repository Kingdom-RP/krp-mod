package com.kingdomrp.core.system;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.registry.KRPAttachments;
import com.kingdomrp.core.config.KRPConfig;
import com.kingdomrp.core.data.map.xp.BrewXPMap;
import com.kingdomrp.core.data.type.Path;
import com.kingdomrp.core.data.map.tier.PotionTierMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Алхимик (path {@link Path#MAGIC}). Гейт варки «не запускать» + XP по тиру
 * зелья + бонус «экономия ингредиента». Игрока в процессе варки не существует
 * (block entity автономен), поэтому owner-а отслеживаем сами.
 * <p>
 * Простой путь (без физической блокировки слотов): owner ловим по клику
 * (кандидат), на старте варки замораживаем — чужой клик во время варки уже не
 * угоняет атрибуцию. Чужой может физически менять содержимое стойки — это
 * ванильное поведение, в простом пути не запрещаем.
 */
@EventBusSubscriber(modid = KingdomRPCore.MODID)
public class MagicSystem {

    private static final int[] BREW_INPUT_SLOTS = {0, 1, 2};

    /** Кандидат в owner-ы — кто последним кликнул по стойке. */
    private static final Map<BlockPos, UUID> interactorMap = new HashMap<>();
    /** Замороженный owner текущей варки (для гейта и XP). */
    private static final Map<BlockPos, UUID> activeBrewerMap = new HashMap<>();
    /** Дедуп сообщения о недоступности (хэш содержимого слотов). */
    private static final Map<BlockPos, Integer> warnedHashMap = new HashMap<>();

    // ============================================================
    // Захват кандидата в owner-ы
    // ============================================================

    @SubscribeEvent
    public static void onBrewingStandClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        var blockEntity = player.level().getBlockEntity(event.getPos());
        if (!(blockEntity instanceof BrewingStandBlockEntity)) return;

        interactorMap.put(event.getPos(), player.getUUID());
    }

    // ============================================================
    // Гейт «не запускать» (вызывается из BrewingStandMixin)
    // ============================================================

    /** Реплика {@code BrewingStandBlockEntity.isBrewable} через {@link PotionBrewing}. */
    public static boolean isVanillaBrewable(PotionBrewing brewing, NonNullList<ItemStack> items) {
        ItemStack ingredient = items.get(3);
        if (ingredient.isEmpty() || !brewing.isIngredient(ingredient)) return false;
        for (int i = 0; i < 3; i++) {
            if (brewing.hasMix(items.get(i), ingredient)) return true;
        }
        return false;
    }

    /**
     * @return true — варку можно запускать/продолжать; false — недоступно по
     *         уровню владельца (варка не стартует).
     */
    public static boolean canBrewGate(Level level, BlockPos pos,
                                      NonNullList<ItemStack> items, int brewTime) {
        if (!KRPConfig.RESTRICTIONS_ENABLED.get()) return true;

        // Варка уже идёт — не пере-гейтим (была разрешена на старте) и не трогаем
        // замороженного owner-а. Иначе чужой клик низкого уровня сбросил бы варку.
        if (brewTime > 0) return true;

        UUID ownerUUID = interactorMap.get(pos);
        if (ownerUUID == null) return true; // нет игрока (напр. хоппер) — не гейтим

        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerUUID);
        if (owner == null) return true; // владелец оффлайн — не гейтим

        int required = requiredLevelForResult(level.potionBrewing(), items);
        int specLevel = getAlchemistLevel(owner);

        if (specLevel < required) {
            warnGated(owner, pos, items, required);
            return false;
        }

        // Доступно: фиксируем owner-а к моменту старта
        activeBrewerMap.put(pos, ownerUUID);
        return true;
    }

    private static int requiredLevelForResult(PotionBrewing brewing, NonNullList<ItemStack> items) {
        ItemStack reagent = items.get(3);
        int required = 0;
        for (int i = 0; i < 3; i++) {
            ItemStack bottle = items.get(i);
            if (bottle.isEmpty()) continue;
            ItemStack result = brewing.mix(reagent, bottle);
            required = Math.max(required, PotionTierMap.requiredLevel(result));
        }
        return required;
    }

    private static void warnGated(ServerPlayer owner, BlockPos pos, NonNullList<ItemStack> items,
                                  int required) {
        int hash = contentHash(items);
        Integer prev = warnedHashMap.get(pos);
        if (prev != null && prev == hash) return; // уже предупреждали об этом наборе
        warnedHashMap.put(pos, hash);
        owner.sendSystemMessage(Component.literal(
                "§c[Kingdom RP] Это зелье недоступно — нужен «Алхимик» ур. " + required + "."
        ));
    }

    private static int contentHash(NonNullList<ItemStack> items) {
        return Objects.hash(
                items.get(3).getItem(),
                items.get(0).get(DataComponents.POTION_CONTENTS),
                items.get(1).get(DataComponents.POTION_CONTENTS),
                items.get(2).get(DataComponents.POTION_CONTENTS)
        );
    }

    // ============================================================
    // Завершение варки: XP по тиру + экономия ингредиента
    // ============================================================

    public static void onBrewComplete(Level level, BlockPos pos,
                                      BrewingStandBlockEntity stand) {
        if (level.isClientSide()) return;

        UUID ownerUUID = activeBrewerMap.get(pos);
        if (ownerUUID == null) return; // нет owner-а (хоппер) — без XP

        ServerPlayer brewer = level.getServer().getPlayerList().getPlayer(ownerUUID);
        if (brewer == null) return;

        // Варка гарантированная — гейт по уровню уже отработал на старте (canBrewGate).
        // RNG успеха/провала и экономии реагента убраны: доступное по уровню зелье
        // варится всегда. XP за штуку по тиру зелья.
        float xp = 0f;
        for (int i = 0; i < 3; i++) {
            ItemStack result = stand.getItem(i);
            if (!result.isEmpty()) xp += BrewXPMap.get(result);
        }
        if (xp > 0f) XPSystem.giveXP(brewer, Path.MAGIC, xp);

        cleanup(pos);
    }

    private static void cleanup(BlockPos pos) {
        activeBrewerMap.remove(pos);
        warnedHashMap.remove(pos);
    }

    // ============================================================
    // Хелперы
    // ============================================================

    private static int getAlchemistLevel(ServerPlayer player) {
        return player.getData(KRPAttachments.PLAYER_DATA).getSpecializationLevel("alchemist");
    }

}
