package com.kingdomrp.core.compat;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.config.KRPConfig;
import com.kingdomrp.core.data.map.xp.BeerBrewXPMap;
import com.kingdomrp.core.data.entry.CraftEntry;
import com.kingdomrp.core.data.map.tier.FoodTierMap;
import com.kingdomrp.core.data.map.xp.ItemCraftMap;
import com.kingdomrp.core.data.map.tier.ItemCraftTierMap;
import com.kingdomrp.core.data.type.Path;
import com.kingdomrp.core.data.type.Spec;
import com.kingdomrp.core.data.type.SpecRequirement;
import com.kingdomrp.core.system.CookSystem;
import com.kingdomrp.core.system.XPSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Мягкая интеграция мода AlcoCraft+ (пиво) — без хард-зависимости.
 * <ul>
 *   <li>КРАФТ деревянной утвари (Кег, пустая кружка) — Плотник ({@link Spec#CARPENTER}):
 *       XP ({@link ItemCraftMap}) + гейт изъятия результата ({@link ItemCraftTierMap},
 *       ловится ванильным {@code SlotMixin.mayPickup}).</li>
 *   <li>ВАРКА пива в Кеге — Повар ({@link Spec#COOK}): гейт по уровню
 *       ({@link FoodTierMap}, как еда) + XP на завершении варки ({@link BeerBrewXPMap}).
 *       Кег — блок мода со своим тиком (не ванильная стойка), поэтому гейт и XP заходят
 *       через {@code KegEntityMixin} (owner — из {@link #interactorMap}).</li>
 * </ul>
 * Хмель (hop) — крафт-сырьё, здесь не гейтим. Микстины активны только если мод загружен
 * (см. {@link AlcoCraftMixinPlugin}).
 */
@EventBusSubscriber(modid = KingdomRPCore.MODID)
public final class AlcoCraftCompat {

    public static final String MODID = "alcocraftplus";

    /** Кег: pos → UUID последнего игрока, взаимодействовавшего с блоком. */
    private static final Map<BlockPos, UUID> interactorMap = new HashMap<>();
    /** pos → сорт пива текущего рецепта (снимок в гейте, для XP на завершении). */
    private static final Map<BlockPos, Item> pendingResult = new HashMap<>();
    /** pos → gameTime последнего предупреждения о гейте (троттлинг спама). */
    private static final Map<BlockPos, Long> lastWarnMap = new HashMap<>();

    private AlcoCraftCompat() {}

    private static String id(String path) {
        return MODID + ":" + path;
    }

    // ===================== РЕГИСТРАЦИЯ МАППИНГОВ =====================

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!ModList.get().isLoaded(MODID)) return;
        event.enqueueWork(AlcoCraftCompat::register);
    }

    private static void register() {
        // --- Плотник: крафт утвари (XP + гейт доступа) ---
        carpenter("mug", 3f, 3);   // пустая кружка (доски) — базовая тара
        carpenter("keg", 6f, 3);   // Кег (плиты+железо+хмель) — станция варки

        // --- Повар: гейт доступа к сортам пива + XP за варку ---
        // Тир по редкости «спец»-ингредиента, старт с ур.3 (kvass — точка входа).
        beer("kvass", 3, 6f);
        beer("magnet_pilsner", 4, 7f);
        beer("night_rauch", 4, 7f);
        beer("ice_beer", 4, 7f);
        beer("sun_pale_ale", 5, 8f);
        beer("drowned_ale", 5, 8f);
        beer("nether_porter", 5, 8f);
        beer("chorus_ale", 6, 10f);
        beer("leprechaun_cider", 6, 10f);
        beer("digger_bitter", 7, 12f);
        beer("nether_star_lager", 8, 15f);
        beer("wither_stout", 8, 15f);
    }

    private static void carpenter(String name, float xp, int tier) {
        ItemCraftMap.addById(id(name), new CraftEntry(Path.CRAFT, Spec.CARPENTER, xp));
        ItemCraftTierMap.addById(id(name), new SpecRequirement(Spec.CARPENTER, tier));
    }

    private static void beer(String name, int tier, float xp) {
        if (tier > 0) FoodTierMap.addById(id(name), Spec.COOK, tier); // tier 0 — без гейта
        BeerBrewXPMap.addById(id(name), xp);
    }

    // ===================== OWNER-ТРЕКИНГ =====================

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        if (MODID.equals(key.getNamespace()) && key.getPath().equals("keg")) {
            interactorMap.put(event.getPos().immutable(), player.getUUID());
        }
    }

    // ===================== ХЕЛПЕРЫ ДЛЯ МИКСТИНА =====================

    /**
     * Гейт продвижения варки в Кеге (Повар). Вызывается из микстина на {@code canBrewRecipe}
     * с сортом результата рецепта. Заодно снимает сорт для XP на завершении.
     * @return true — варку можно продвигать; false — недоступно по уровню владельца
     *         (прогресс не растёт, ингредиенты не списываются — brew() их трогает лишь
     *         на завершении).
     */
    public static boolean gateBrew(Level level, BlockPos pos, Item beer) {
        pendingResult.put(pos.immutable(), beer);
        if (!KRPConfig.RESTRICTIONS_ENABLED.get()) return true;

        ServerPlayer owner = owner(level, pos);
        if (owner == null) return true; // нет игрока (оффлайн) — не гейтим

        if (CookSystem.canProduce(owner, beer)) return true;
        warnGated(level, pos, owner, beer);
        return false;
    }

    /** Завершение варки: разовое начисление XP владельцу по снятому сорту. */
    public static void onBrewComplete(Level level, BlockPos pos) {
        ServerPlayer owner = owner(level, pos);
        if (owner == null) return;
        Item beer = pendingResult.get(pos);
        if (beer == null) return;
        float xp = BeerBrewXPMap.get(beer);
        if (xp > 0f) XPSystem.giveXP(owner, Path.HARVEST, xp);
    }

    private static void warnGated(Level level, BlockPos pos, ServerPlayer owner, Item beer) {
        long now = level.getGameTime();
        Long prev = lastWarnMap.get(pos);
        if (prev != null && now - prev < 100L) return;
        lastWarnMap.put(pos, now);
        var tier = FoodTierMap.get(beer);
        int req = tier != null ? tier.level() : 0;
        owner.sendSystemMessage(Component.literal(
                "§c[Kingdom RP] Варка этого пива доступна с " + req + " уровня Повара."));
    }

    private static ServerPlayer owner(Level level, BlockPos pos) {
        UUID uuid = interactorMap.get(pos);
        if (uuid == null || level.getServer() == null) return null;
        return level.getServer().getPlayerList().getPlayer(uuid);
    }
}
