package com.kingdomrp.core.system;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.capability.PlayerData;
import com.kingdomrp.core.registry.KRPAttachments;
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
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = KingdomRPCore.MODID)
public class XPSystem {

    @SubscribeEvent
    public static void onLivingHurt(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
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
    public static void onArrowHit(net.neoforged.neoforge.event.entity.ProjectileImpactEvent event) {
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

        // Гейт тира (SpecializationEffects, приоритет HIGH) отменяет событие при
        // нехватке уровня — за запрещённую добычу XP не даём.
        if (event.isCanceled()) return;

        // Не даём XP за поставленные игроком блоки
        if (PlacedBlockTracker.isPlacedByPlayer(event.getPos())) {
            PlacedBlockTracker.onBroken(event.getPos());
            return;
        }

        // Dynamic Trees: рубка DT-деревьев обрабатывается отдельно по фактическому
        // объёму сваленного дерева — compat/mixin/DTBranchBlockMixin (не здесь, т.к.
        // BreakEvent для ветки не знает объём и повторяется при доломе остатка).
        BlockEntry entry = BlockXPMap.get(event.getState().getBlock());
        if (entry == null) return;

        // Недозрелый урожай XP не даёт (как и двойной дроп Фермера, см.
        // SpecializationEffects.checkFarmer). Иначе можно фармить опыт, ломая
        // ростки сразу после посадки.
        if (isImmatureCrop(event.getState())) return;

        giveXP(player, entry.path(), entry.xpReward());
    }

    /**
     * Незрелая ли культура. Ванильные {@code CropBlock} — по {@code isMaxAge};
     * модовые культуры (Farmer's Delight и др.) — обобщённо по свойству-целому
     * {@code age} (не на максимуме). Если свойства возраста нет — считаем зрелым.
     */
    public static boolean isImmatureCrop(net.minecraft.world.level.block.state.BlockState state) {
        if (state.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop) {
            return !crop.isMaxAge(state);
        }
        for (var prop : state.getProperties()) {
            if (prop instanceof net.minecraft.world.level.block.state.properties.IntegerProperty ageProp
                    && "age".equals(prop.getName())) {
                int value = state.getValue(ageProp);
                int max = ageProp.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(value);
                return value < max;
            }
        }
        return false;
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

    /** Дебафф к опыту + неполный голод при возрождении (не при выходе из Энда). */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Неполный голод после смерти (по умолч. 50%), без сатурации.
        var food = player.getFoodData();
        food.setFoodLevel(KRPConfig.RESPAWN_FOOD_LEVEL.get());
        food.setSaturation(0f);
        food.setExhaustion(0f);

        int duration = KRPConfig.DEATH_PENALTY_DURATION.get();
        if (duration <= 0) return;

        MobEffectInstance inst = new MobEffectInstance(
                KRPEffects.DEATH_XP_PENALTY, duration, 0, false, true, true);
        player.addEffect(inst);
    }

    /** Молоко ({@code EffectCures.MILK}) НЕ снимает штраф к опыту — отменяем удаление. */
    @SubscribeEvent
    public static void onEffectRemove(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Remove event) {
        if (event.getEffect() != KRPEffects.DEATH_XP_PENALTY) return;
        if (event.getCure() == net.neoforged.neoforge.common.EffectCures.MILK) {
            event.setCanceled(true);
        }
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
    public static void onFishing(net.neoforged.neoforge.event.entity.player.ItemFishedEvent event) {
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
    public static void onBabySpawn(net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent event) {
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
        if (event.getItemAbility() != net.neoforged.neoforge.common.ItemAbilities.HOE_TILL) return;
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
        PlayerData data = serverPlayer.getData(KRPAttachments.PLAYER_DATA);

        float multiplier = data.getXPMultiplier(path);
        // Дебафф смерти стакается с приоритетным штрафом (мультипликативно)
        if (player.hasEffect(KRPEffects.DEATH_XP_PENALTY)) {
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
                .map(com.kingdomrp.core.specialization.Specialization::name)
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