package com.kingdomrp.core.system;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.capability.PlayerDataProvider;
import com.kingdomrp.core.config.KRPConfig;
import com.kingdomrp.core.data.*;
import com.kingdomrp.core.network.PacketHelper;
import com.kingdomrp.core.registry.KRPEffects;
import com.kingdomrp.core.world.PlacedBlockTracker;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KingdomRPCore.MODID)
public class XPSystem {

    @SubscribeEvent
    public static void onLivingHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        // XP за нанесение урона
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            if (!attacker.level().isClientSide()) {
                net.minecraft.world.entity.LivingEntity target = event.getEntity();
                float xp = MobDamageMap.get(target);
                if (xp > 0) giveXP(attacker, Path.WAR, xp);
            }
        }

        // XP за получение урона
        if (event.getEntity() instanceof ServerPlayer victim) {
            if (!victim.level().isClientSide()) {
                giveXP(victim, Path.WAR, 1f);
            }
        }
    }

    @SubscribeEvent
    public static void onArrowHit(net.minecraftforge.event.entity.ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        if (!(event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult hitResult)) return;
        if (!(hitResult.getEntity() instanceof net.minecraft.world.entity.LivingEntity)) return;

        giveXP(player, Path.WAR, 2f);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide()) return;

        // Не даём XP за поставленные игроком блоки
        if (PlacedBlockTracker.isPlacedByPlayer(event.getPos())) {
            PlacedBlockTracker.onBroken(event.getPos());
            return;
        }

        BlockEntry entry = BlockXPMap.get(event.getState().getBlock());
        if (entry == null) return;

        giveXP(player, entry.path(), entry.xpReward());
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        if (event.getEntity() instanceof Player) return;

        KillEntry entry = MobKillMap.get(event.getEntity().getType());
        if (entry == null) return;

        giveXP(player, entry.path(), entry.xpReward());
    }

    /**
     * Дебафф к опыту после смерти. Накладывается при возрождении (не при выходе
     * из Энда). Список curative-items очищается → молоко его не снимает, но все
     * ванильные эффекты молоко убирает как обычно.
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        int duration = KRPConfig.DEATH_PENALTY_DURATION.get();
        if (duration <= 0) return;

        MobEffectInstance inst = new MobEffectInstance(
                KRPEffects.DEATH_XP_PENALTY.get(), duration, 0, false, true, true);
        inst.getCurativeItems().clear();
        player.addEffect(inst);
    }

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        Player player = event.getEntity();
        ItemStack result = event.getCrafting();
        if (result.isEmpty()) return;

        var reqs = RestrictionSystem.getCraftRequirements(result);
        if (reqs != null) {
            for (var req : reqs) {
                if (!RestrictionSystem.meetsRequirement(player, req)) {
                    burnCraft(player, result);
                    RestrictionSystem.sendRestrictionMessage(player, req);
                    return;
                }
            }
        }

        // Повар: гейтинг крафта еды по уровню специализации (FoodTierMap)
        if (!CookSystem.canProduce(player, result.getItem())) {
            burnCraft(player, result);
            if (player instanceof ServerPlayer sp) CookSystem.sendRestriction(sp, result.getItem());
            return;
        }

        var entry = com.kingdomrp.core.data.ItemCraftMap.get(result.getItem());
        if (entry == null) return;
        // Крафт всегда успешен — прогрессия держится на лестницах доступа и
        // активных эффектах навыков, а не на шансе провала.
        giveXP(player, entry.path(), entry.xpReward());
    }

    @SubscribeEvent
    public static void onFishing(net.minecraftforge.event.entity.player.ItemFishedEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        // Маппинг XP: за самый ценный предмет улова
        float xp = FishingXPMap.JUNK_XP; // хлам по умолчанию
        for (var stack : event.getDrops()) {
            xp = Math.max(xp, FishingXPMap.get(stack.getItem()));
        }
        giveXP(player, Path.HARVEST, xp);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Повар: готовка на костре = XP пути Промысел в момент укладки сырья.
        // Считаем для ОБЕИХ рук (сырьё можно класть из off-hand) — берём стак
        // именно той руки, что вызвала событие. Гейтинг (отмена при нехватке
        // уровня) — в SpecializationEffects; здесь дублируем canProduce, чтобы не
        // дать XP под-уровневому повару независимо от порядка обработчиков.
        if (player.level().getBlockEntity(event.getPos())
                instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity campfire) {
            var result = CookSystem.campfireResult(player.level(), campfire, event.getItemStack());
            if (result != null && CookSystem.canProduce(player, result)) {
                CookSystem.onCooked(player, new ItemStack(result));
            }
        }

        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;

        var state = event.getLevel().getBlockState(event.getPos());

        // Лесоруб: обтёсывание бревна топором = 1 XP (путь Добыча)
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof net.minecraft.world.item.AxeItem
                && isStrippableLog(state.getBlock())) {
            giveXP(player, Path.MINING, 1f);
            return;
        }

        // Фермер: сбор ягод в ПКМ (спелый куст / светящиеся ягоды) = 1 XP (путь Промысел)
        if (isBerryReady(state)) {
            giveXP(player, Path.HARVEST, 1f);
            return;
        }
    }

    @SubscribeEvent
    public static void onBabySpawn(net.minecraftforge.event.entity.living.BabyEntitySpawnEvent event) {
        if (!(event.getCausedByPlayer() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;

        AnimalTierEntry tier = AnimalTierMap.get(event.getParentA().getType());
        if (tier == null) return;

        // XP за успешное разведение (путь Промысел). Ваниль-кулдаун 5 мин = анти-спам.
        giveXP(player, Path.HARVEST, tier.breedXP());
    }

    @SubscribeEvent
    public static void onToolModify(BlockEvent.BlockToolModificationEvent event) {
        if (event.isSimulated()) return;
        if (event.getToolAction() != net.minecraftforge.common.ToolActions.HOE_TILL) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;

        // Фермер: вспашка земли мотыгой = 0.25 XP (путь Промысел)
        giveXP(player, Path.HARVEST, 0.25f);
    }

    // Готов ли блок отдать ягоды по ПКМ (без ломки)
    private static boolean isBerryReady(net.minecraft.world.level.block.state.BlockState state) {
        var block = state.getBlock();
        if (block instanceof net.minecraft.world.level.block.SweetBerryBushBlock) {
            return state.getValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE) >= 2;
        }
        if (block instanceof net.minecraft.world.level.block.CaveVinesBlock
                || block instanceof net.minecraft.world.level.block.CaveVinesPlantBlock) {
            return state.getValue(net.minecraft.world.level.block.CaveVines.BERRIES);
        }
        return false;
    }

    private static boolean isStrippableLog(Block block) {
        return block == Blocks.OAK_LOG || block == Blocks.BIRCH_LOG
                || block == Blocks.SPRUCE_LOG || block == Blocks.JUNGLE_LOG
                || block == Blocks.ACACIA_LOG || block == Blocks.DARK_OAK_LOG
                || block == Blocks.MANGROVE_LOG || block == Blocks.CHERRY_LOG
                || block == Blocks.CRIMSON_STEM || block == Blocks.WARPED_STEM
                || block == Blocks.OAK_WOOD || block == Blocks.BIRCH_WOOD
                || block == Blocks.SPRUCE_WOOD || block == Blocks.JUNGLE_WOOD
                || block == Blocks.ACACIA_WOOD || block == Blocks.DARK_OAK_WOOD
                || block == Blocks.MANGROVE_WOOD || block == Blocks.CHERRY_WOOD
                || block == Blocks.CRIMSON_HYPHAE || block == Blocks.WARPED_HYPHAE;
    }

    /**
     * XP Кузнеца (path Ремесло) за выплавку металла в печи. Вызывается из
     * {@code FurnaceResultSlotMixin} при изъятии результата; XP за штуку ×
     * количество. Плавка не гейтится уровнем.
     */
    public static void onMetalSmelted(Player player, ItemStack result) {
        if (player.level().isClientSide()) return;
        if (result.isEmpty()) return;
        float per = MetalSmeltMap.get(result.getItem());
        if (per <= 0f) return;
        // Access-гейт по уровню: за переплавку выше своего тира XP не даём, даже
        // если сырьё положил другой игрок или хоппер (вход печи такое не ловит).
        if (RestrictionSystem.isSmeltBlocked(player, result.getItem())) return;
        giveXP(player, Path.CRAFT, per * result.getCount());
    }

    /**
     * XP Мастерового (path Ремесло) за обжиг натуральных материалов в печи
     * (стекло/камень/глина/керамика). Вызывается из {@code FurnaceResultSlotMixin}
     * при изъятии результата; XP за штуку × количество. Обжиг не гейтится уровнем.
     */
    public static void onNaturalSmelted(Player player, ItemStack result) {
        if (player.level().isClientSide()) return;
        if (result.isEmpty()) return;
        float per = NaturalSmeltMap.get(result.getItem());
        if (per <= 0f) return;
        giveXP(player, Path.CRAFT, per * result.getCount());
    }

    public static void giveXP(Player player, Path path, float amount) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            float multiplier = data.getXPMultiplier(path);
            // Дебафф смерти стакается с приоритетным штрафом (мультипликативно)
            if (player.hasEffect(KRPEffects.DEATH_XP_PENALTY.get())) {
                multiplier *= KRPConfig.DEATH_XP_MULTIPLIER.get().floatValue();
            }
            float finalAmount = amount * multiplier;

            boolean leveledUp = data.addXP(path, finalAmount);
            PacketHelper.syncPlayer(serverPlayer);

            // Полоска прогресса в HUD — текущий прогресс к следующему уровню
            PacketHelper.sendXPBar(serverPlayer, path, data.getXP(path),
                    data.getXPRequired(path), data.getLevel(path), leveledUp);

            if (leveledUp) {
                onLevelUp(player, path, data.getLevel(path));
            }
        });
    }

    private static void onLevelUp(Player player, Path path, int newLevel) {
        player.sendSystemMessage(Component.literal(
                "§6[Kingdom RP] §eПуть «" + getPathName(path) + "» — уровень " + newLevel
                        + "! §7Откройте меню (K) для выбора специализации."
        ));
    }

    /**
     * Бэкстоп отмены недоступного крафта: зануляет результат и возвращает
     * ингредиенты из крафт-сетки в инвентарь. Основной гейт — на ВЫЧИСЛЕНИИ
     * результата (`CraftingMenuMixin` не даёт результату появиться в слоте), так
     * что для gated-крафта это событие недостижимо. Оставлен на случай иных путей.
     */
    private static void burnCraft(Player player, ItemStack result) {
        result.setCount(0);
        var menu = player.containerMenu;
        for (int i = 0; i < menu.slots.size(); i++) {
            var slot = menu.slots.get(i);
            if (slot.container instanceof net.minecraft.world.inventory.CraftingContainer) {
                var ingredient = slot.getItem();
                if (!ingredient.isEmpty()) {
                    player.getInventory().add(ingredient.copy());
                    slot.set(ItemStack.EMPTY);
                }
            }
        }
    }

    public static String getSpecName(String specId) {
        return com.kingdomrp.core.specialization.SpecializationRegistry
                .get(specId)
                .map(com.kingdomrp.core.specialization.Specialization::getName)
                .orElse(specId);
    }

    public static String getPathName(Path path) {
        return switch (path) {
            case CRAFT   -> "Ремесло";
            case HARVEST -> "Промысел";
            case MINING  -> "Добыча";
            case WAR     -> "Война";
            case MAGIC   -> "Магия";
        };
    }
}