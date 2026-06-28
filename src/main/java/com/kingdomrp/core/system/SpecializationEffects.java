package com.kingdomrp.core.system;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.capability.PlayerData;
import com.kingdomrp.core.data.*;
import com.kingdomrp.core.registry.KRPAttachments;
import com.kingdomrp.core.specialization.Specialization;
import com.kingdomrp.core.util.ScalingFormula;
import com.kingdomrp.core.world.PlacedBlockTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

@EventBusSubscriber(modid = KingdomRPCore.MODID)
public class SpecializationEffects {

    // Доступ к данным игрока (Data Attachment) с сохранением лямбда-стиля старого кода.
    private static void withData(Player player, java.util.function.Consumer<PlayerData> action) {
        action.accept(player.getData(KRPAttachments.PLAYER_DATA));
    }

    private static final int MAX_TREE_BLOCKS = 64;

    // ================================================================
    // СОБЫТИЯ
    // ================================================================

    // HIGH: гейт тира (checkTierRestriction) должен отменить событие ДО того,
    // как XPSystem.onBlockBreak начислит опыт (за запрещённую добычу XP не даём).
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGH)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

        Block block = event.getState().getBlock();
        BlockPos pos = event.getPos();

        if (checkTierRestriction(player, block, event)) return;
        if (PlacedBlockTracker.isPlacedByPlayer(pos)) return;

        checkMiner(player, block, pos, event);
        checkLumberjack(player, block, pos, event);
        checkFarmer(player, block, pos, event);
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Block block = event.getState().getBlock();

        applyMinerSpeed(player, block, event);
        applyLumberjackSpeed(player, block, event);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Player attacker) {
            if (attacker.level().isClientSide()) return;
            boolean isProjectile = event.getSource().getDirectEntity()
                    instanceof net.minecraft.world.entity.projectile.Projectile;

            if (!isProjectile) {
                applyWarriorDamage(attacker, event);
            } else {
                applyArcherDamage(attacker, event);
                applyArmorPierce(attacker, event);
            }
        }

        if (event.getEntity() instanceof Player victim) {
            if (victim.level().isClientSide()) return;
            applyWarriorDefense(victim, event);
        }
    }

    @SubscribeEvent
    public static void onCraftResult(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        Player player = event.getEntity();
        ItemStack result = event.getCrafting();
        if (result.isEmpty()) return;

        checkCraftBonus(player, result);
        checkCarpenterEconomy(player, result, event.getInventory());
        checkCarpenterBatch(player, result);
        checkCraftsmanEconomy(player, result, event.getInventory());
        checkCraftsmanBatch(player, result);
        // Закалка — не здесь, а при сборке результата (CraftingResultMixin):
        // ItemCraftedEvent при shift-click срабатывает уже после перемещения стака.
    }

    @SubscribeEvent
    public static void onArrowLoose(net.neoforged.neoforge.event.entity.player.ArrowLooseEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;

        withData(player, data -> {
            int archerLevel = data.getSpecializationLevel(Spec.ARCHER.id);
            if (archerLevel <= 0) return;

            // Дальность полёта стрелы масштабируется в onArrowSpawn (ур.0=50% …
            // ур.10=150%). Механический бонус скорости зарядки убран: он давал
            // рассинхрон (механика быстрее визуала) и абуз быстрого спама.

            // Двойная стрела с 5 уровня
            if (archerLevel < 5) return;
            float chance = 0.25f + (archerLevel - 5) * 0.05f;
            if (player.level().random.nextFloat() > chance) return;

            net.minecraft.world.item.ItemStack bow = event.getBow();
            net.minecraft.world.item.ItemStack ammo = player.getProjectile(bow);
            if (ammo.isEmpty()) return;

            // Замени блок создания стрелы в onArrowLoose:
            net.minecraft.world.item.ArrowItem arrowItem =
                    (ammo.getItem() instanceof net.minecraft.world.item.ArrowItem a)
                            ? a : (net.minecraft.world.item.ArrowItem) net.minecraft.world.item.Items.ARROW;

            net.minecraft.world.entity.projectile.AbstractArrow arrow =
                    arrowItem.createArrow(player.level(), ammo, player, bow);
            arrow.shootFromRotation(player,
                    player.getXRot(), player.getYRot(),
                    0f, event.getCharge() / 20f * 3f, 1f);

            ((net.minecraft.server.level.ServerLevel) player.level()).addFreshEntity(arrow);
        });
    }

    /**
     * Лучник — дальность полёта стрелы. Масштабируем скорость стрелы при спавне:
     * ур.0 = 50%, ур.5 = 100%, ур.10 = 150% от дефолтной (factor = 0.5 + 0.1·ур).
     * Скорость определяет и дальность, и урон при попадании, так что низкоуровневый
     * лучник стреляет слабее и ближе, прокачанный — дальше и сильнее. Применяется ко
     * всем стрелам игрока (обычная + двойная из {@link #onArrowLoose}).
     */
    @SubscribeEvent
    public static void onArrowSpawn(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof ServerPlayer player)) return;

        int archerLevel = player.getData(KRPAttachments.PLAYER_DATA)
                .getSpecializationLevel(Spec.ARCHER.id);
        if (archerLevel <= 0) return;

        float factor = 0.5f + archerLevel * 0.1f;
        arrow.setDeltaMovement(arrow.getDeltaMovement().scale(factor));
    }

    // Эффекты рыбалки перенесены на Tide: ускоренный клёв — TideFishingHookMixin
    // (бонус к lureSpeed по уровню Рыбака). Двойной улов / сохранение прочности /
    // luck-качество убраны (Tide постит ItemFishedEvent read-only — модификации
    // улова игнорируются; vanilla FishingHook не используется).

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;

        // Костёр гейтим для ОБЕИХ рук: сырьё можно класть и из off-hand,
        // иначе ограничение обходится. applyCampfireGate смотрит на
        // event.getItemStack() — стак именно той руки, что вызвала событие.
        applyCampfireGate(player, event);

        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;

        applyHoeDurability(player, event);
        applyCombineHarvest(player, event);
    }

    @SubscribeEvent
    public static void onBonemeal(net.neoforged.neoforge.event.entity.player.BonemealEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

        var state = event.getState();
        if (!(state.getBlock() instanceof net.minecraft.world.level.block.BonemealableBlock bm)) return;

        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = event.getPos();
        // Реагируем только на валидные цели (не даём XP за тык по камню)
        if (!bm.isValidBonemealTarget(level, pos, state)) return;

        // 1 XP за применение костной муки (путь Промысел, не зависит от спеца)
        XPSystem.giveXP(player, Path.HARVEST, 1f);

        // Усиленная костная мука: +level доп. применений за клик (ур.10 ≈ мгновенная
        // спелость почти любого растения, ур.1 ≈ удвоенный эффект).
        withData(player, data -> {
            int farmerLevel = data.getSpecializationLevel(Spec.FARMER.id);
            if (farmerLevel <= 0) return;

            int extra = farmerLevel;
            var rng = level.random;
            for (int i = 0; i < extra; i++) {
                var st = level.getBlockState(pos);
                if (!(st.getBlock() instanceof net.minecraft.world.level.block.BonemealableBlock b2)) break;
                if (!b2.isValidBonemealTarget(level, pos, st)) break;
                b2.performBonemeal(level, rng, pos, st);
            }
        });
    }

    // Дальность блоков (Плотник) сбрасывается у нового entity при возрождении/
    // смене измерения — переустанавливаем модификатор. На login/левелап/прокачку
    // обновляется через PacketHelper.syncPlayer.
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) refreshBlockReach(player);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) refreshBlockReach(player);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        if (!(event.getTarget() instanceof net.minecraft.world.entity.animal.Animal animal)) return;

        AnimalTierEntry tier = AnimalTierMap.get(animal.getType());
        if (tier == null) return;

        ItemStack stack = event.getItemStack();
        boolean milking = stack.getItem() == net.minecraft.world.item.Items.BUCKET
                && (animal instanceof net.minecraft.world.entity.animal.Cow
                    || animal instanceof net.minecraft.world.entity.animal.goat.Goat);
        boolean shearing = stack.getItem() instanceof net.minecraft.world.item.ShearsItem
                && (animal instanceof net.minecraft.world.entity.animal.Sheep
                    || animal instanceof net.minecraft.world.entity.animal.MushroomCow);
        // Кормление взрослого животного его кормом = попытка размножения
        boolean breedingFeed = animal.getAge() == 0 && animal.isFood(stack);

        if (!(milking || shearing || breedingFeed)) return;

        int farmerLevel = player.getData(KRPAttachments.PLAYER_DATA)
                .getSpecializationLevel(Spec.FARMER.id);

        if (farmerLevel < tier.level()) {
            event.setCanceled(true);
            // Клиент предсказывает взаимодействие (ведро→молоко, съеденный корм)
            // ещё до ответа сервера. Сервер отменил действие, но без пересинка
            // предсказание висит фантомом у игрока — принудительно ресинкаем инвентарь.
            player.containerMenu.sendAllDataToRemote();

            String specName = com.kingdomrp.core.specialization.SpecializationRegistry
                    .get(Spec.FARMER.id).map(Specialization::name).orElse("Фермер");
            String action = milking ? "доить это животное"
                    : shearing ? "стричь это животное"
                    : "разводить это животное";
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§c[Kingdom RP] Чтобы " + action + ", прокачайте навык «"
                            + specName + "» до " + tier.level() + " уровня."));
            return;
        }

        // XP за стрижку овцы (лимит — отрастание шерсти после поедания травы)
        if (shearing && animal instanceof net.minecraft.world.entity.animal.Sheep sheep
                && sheep.readyForShearing()) {
            XPSystem.giveXP(player, Path.HARVEST, 1f);
        }
    }

    // ================================================================
    // ПРОВЕРКИ ПРИ ЛОМКЕ БЛОКА
    // ================================================================

    // Возвращает true если блок заблокирован — дальше не идём
    private static boolean checkTierRestriction(ServerPlayer player,
                                                Block block,
                                                BlockEvent.BreakEvent event) {
        BlockTierEntry tierEntry = BlockTierMap.get(block);
        if (tierEntry == null) return false;

        int specLevel = player.getData(KRPAttachments.PLAYER_DATA)
                .getSpecializationLevel(tierEntry.spec().id);

        if (specLevel >= tierEntry.level()) return false;

        event.setCanceled(true);
        String specName = com.kingdomrp.core.specialization.SpecializationRegistry
                .get(tierEntry.spec().id)
                .map(Specialization::name)
                .orElse(tierEntry.spec().id);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§c[Kingdom RP] Для добычи этого блока прокачайте навык «"
                        + specName + "» до " + tierEntry.level() + " уровня."
        ));
        return true;
    }

    private static void checkMiner(ServerPlayer player, Block block,
                                   BlockPos pos, BlockEvent.BreakEvent event) {
        if (!OreDropMap.isOre(block)) return;

        withData(player, data -> {
            int specLevel = data.getSpecializationLevel(Spec.MINER.id);
            if (specLevel <= 0) return;

            float chance = specLevel * 0.025f;
            if (player.level().random.nextFloat() < chance) {
                block.playerDestroy(player.level(), player, pos,
                        event.getState(),
                        player.level().getBlockEntity(pos),
                        player.getMainHandItem());
            }
        });
    }

    private static void checkLumberjack(ServerPlayer player, Block block,
                                        BlockPos pos, BlockEvent.BreakEvent event) {
        if (!isLog(block)) return;

        withData(player, data -> {
            int specLevel = data.getSpecializationLevel(Spec.LUMBERJACK.id);
            if (specLevel <= 0) return;

            // Сохранение прочности топора: 5%/уровень. BreakEvent срабатывает ДО
            // нанесения урона инструменту, поэтому откатываем на следующий тик,
            // клампя к снимку «до» (Math.min) — не уходим ниже исходного значения.
            // Это корректно стакается с Unbreaking: если чара уже погасила урон,
            // откат ничего не добавит (иначе топор бы самочинился — был баг).
            ItemStack axe = player.getMainHandItem();
            float durabilityChance = specLevel * 0.05f;
            if (!axe.isEmpty() && axe.isDamageableItem()
                    && player.level().random.nextFloat() < durabilityChance) {
                int before = axe.getDamageValue();
                ((ServerLevel) player.level()).getServer().execute(() -> {
                    if (axe.isDamageableItem()) {
                        axe.setDamageValue(Math.min(before, axe.getDamageValue()));
                    }
                });
            }

            // Двойной дроп
            float dropChance = specLevel * 0.025f;
            if (player.level().random.nextFloat() < dropChance) {
                block.playerDestroy(player.level(), player, pos,
                        event.getState(),
                        player.level().getBlockEntity(pos),
                        player.getMainHandItem());
            }

            // Срубка дерева с 5 уровня
            if (specLevel >= 5) {
                float fellChance = 0.25f + (specLevel - 5) * 0.11f;
                if (player.level().random.nextFloat() < fellChance) {
                    ((ServerLevel) player.level()).getServer()
                            .execute(() -> fellTree(player, pos));
                }
            }
        });
    }

    private static void checkFarmer(ServerPlayer player, Block block,
                                    BlockPos pos, BlockEvent.BreakEvent event) {
        BlockEntry entry = BlockXPMap.get(block);
        if (entry == null || entry.path() != Path.HARVEST) return;
        if (XPSystem.isImmatureCrop(event.getState())) return;

        withData(player, data -> {
            int farmerLevel = data.getSpecializationLevel(Spec.FARMER.id);
            if (farmerLevel <= 0) return;

            // Двойной дроп спелого урожая: линейно 5%/уровень, макс 50% на ур.10
            float chance = farmerLevel * 0.05f;
            if (player.level().random.nextFloat() < chance) {
                block.playerDestroy(player.level(), player, pos,
                        event.getState(),
                        player.level().getBlockEntity(pos),
                        player.getMainHandItem());
            }
        });
    }

    // ================================================================
    // СКОРОСТЬ ДОБЫЧИ
    // ================================================================

    private static void applyMinerSpeed(ServerPlayer player, Block block,
                                        PlayerEvent.BreakSpeed event) {
        BlockEntry entry = BlockXPMap.get(block);
        if (entry == null || entry.path() != Path.MINING) return;
        if (isLog(block)) return; // логи — зона лесоруба

        withData(player, data -> {
            int specLevel = data.getSpecializationLevel(Spec.MINER.id);
            if (specLevel <= 0) return;
            event.setNewSpeed(event.getOriginalSpeed() * (1.0f + specLevel * 0.05f));
        });
    }

    private static void applyLumberjackSpeed(ServerPlayer player, Block block,
                                             PlayerEvent.BreakSpeed event) {
        // Брёвна + обработанное дерево (доски/мебель/двери и т.п. — то, что
        // крафтит Плотник): Лесоруб хорошо добывает дерево в любом виде.
        if (!isLog(block) && !isWorkedWood(block)) return;

        withData(player, data -> {
            int specLevel = data.getSpecializationLevel(Spec.LUMBERJACK.id);
            if (specLevel <= 0) return;
            event.setNewSpeed(event.getOriginalSpeed() * (1.0f + specLevel * 0.05f));
        });
    }

    // ================================================================
    // ВОЙНА
    // ================================================================

    private static void applyWarriorDamage(Player player, LivingIncomingDamageEvent event) {
        withData(player, data -> {
            int warriorLevel = data.getSpecializationLevel(Spec.WARRIOR.id);
            if (warriorLevel <= 0) return;
            // +2.5% урона за уровень
            float bonus = warriorLevel * 0.025f;
            event.setAmount(event.getAmount() * (1f + bonus));
        });
    }

    // Добавь новый метод — снижение получаемого урона
    private static void applyWarriorDefense(Player player, LivingIncomingDamageEvent event) {
        withData(player, data -> {
            int warriorLevel = data.getSpecializationLevel(Spec.WARRIOR.id);
            if (warriorLevel <= 0) return;
            // -2.5% урона за уровень
            float reduction = warriorLevel * 0.025f;
            event.setAmount(event.getAmount() * (1f - reduction));
        });
    }

    // ================================================================
    // ЛУЧНИК
    // ================================================================

    private static void applyArcherDamage(Player player, LivingIncomingDamageEvent event) {
        withData(player, data -> {
            int archerLevel = data.getSpecializationLevel(Spec.ARCHER.id);
            float multiplier = 0.75f + archerLevel * 0.05f;
            event.setAmount(event.getAmount() * multiplier);
        });
    }

    private static void applyArmorPierce(Player player, LivingIncomingDamageEvent event) {
        withData(player, data -> {
            int archerLevel = data.getSpecializationLevel(Spec.ARCHER.id);
            if (archerLevel < 5) return;

            float ignorePercent = 0.10f + (archerLevel - 5) * 0.04f;

            net.minecraft.world.entity.LivingEntity target = event.getEntity();
            double armor = target.getAttributeValue(
                    net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
            double toughness = target.getAttributeValue(
                    net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);

            // Считаем сколько урона поглощает броня
            float baseAmount = event.getAmount();
            float armorReduction = (float) Math.max(baseAmount / 5.0,
                    baseAmount - baseAmount / (2.0 + toughness / 4.0) * armor / 25.0);
            float absorbed = baseAmount - armorReduction;

            // Возвращаем часть поглощённого
            event.setAmount(baseAmount + absorbed * ignorePercent);
        });
    }

    // ================================================================
    // РЕМЕСЛО
    // ================================================================

    private static void checkCraftBonus(Player player, ItemStack result) {
        var entry = ItemCraftMap.get(result.getItem());
        if (entry == null) return;
        // Двойной выход крафта НИ У КОГО из ремесленников пути Ремесло нет:
        // Повар — только лестница доступа + XP; Плотник — экономия древесины
        // (checkCarpenterEconomy); Кузнец — закалка (applyBlacksmithTempering);
        // Мастеровой — экономия материала (checkCraftsmanEconomy). Двойной крафт
        // у Мастерового убран по требованию (был унаследован от Инженера).
        if (entry.spec() == Spec.COOK || entry.spec() == Spec.CARPENTER
                || entry.spec() == Spec.BLACKSMITH || entry.spec() == Spec.CRAFTSMAN) return;

        withData(player, data -> {
            int specLevel = data.getSpecializationLevel(entry.spec().id);
            if (specLevel <= 0) return;

            float chance = ScalingFormula.compute(specLevel, 0.4f, 0.5f);
            if (player.level().random.nextFloat() < chance) {
                player.getInventory().add(result.copy());
            }
        });
    }

    // Вызывается при сборке результата крафта (CraftingResultMixin), до изъятия.
    // Закаляет металл (Кузнец) и натуральную броню (Мастеровой).
    public static void applyTemperingToCraftResult(Player player, ItemStack result) {
        if (player == null || result == null || result.isEmpty()) return;
        applyBlacksmithTempering(player, result);
        applyCraftsmanTempering(player, result);
    }

    // Закалка Кузнеца: свежескованное металлическое изделие получает прочность,
    // зависящую от уровня. Свежеоткрытый тир → 50%, к открытию следующего тира →
    // 100% (см. BlacksmithTemperMap). Заменяет шанс провала крафта — низкоуровневый
    // Кузнец делает рабочий, но менее долговечный предмет. Незерит — в SmithingMenuMixin.
    private static void applyBlacksmithTempering(Player player, ItemStack result) {
        if (!result.isDamageableItem()) return;
        BlacksmithTemperMap.TemperTier tier = BlacksmithTemperMap.get(result.getItem());
        if (tier == null) return;

        withData(player, data -> {
            int level = data.getSpecializationLevel(Spec.BLACKSMITH.id);
            float quality = BlacksmithTemperMap.quality(tier, level);
            if (quality >= 1.0f) return;
            int maxDur = result.getMaxDamage();
            int damage = Math.round((1f - quality) * maxDur);
            damage = Math.min(damage, maxDur - 1); // не отдаём «сломанный» предмет
            result.setDamageValue(damage);
        });
    }

    // Экономия древесины: при крафте предмета Плотника шанс вернуть 1 ед.
    // деревянного ингредиента обратно в инвентарь (как экономия реагента у
    // Алхимика). Шанс линейный: level*0.05, макс 50% на ур.10.
    //
    // Что вернуть определяем точно: сначала по фактически использованной сетке
    // (точный сорт дерева, если в слоте остался остаток стопки), затем —
    // запасным путём — по рецепту (если сырьё израсходовано подчистую и сетка
    // пуста, всё равно знаем, какой ингредиент брался).
    private static void checkCarpenterEconomy(Player player, ItemStack result,
                                              net.minecraft.world.Container craftMatrix) {
        var entry = ItemCraftMap.get(result.getItem());
        if (entry == null || entry.spec() != Spec.CARPENTER) return;
        if (!(craftMatrix instanceof net.minecraft.world.inventory.CraftingContainer cc)) return;

        withData(player, data -> {
            int level = data.getSpecializationLevel(Spec.CARPENTER.id);
            if (level <= 0) return;

            float chance = Math.min(0.5f, level * 0.05f);
            if (player.level().random.nextFloat() >= chance) return;

            net.minecraft.world.item.Item refund = findWoodInGrid(cc);
            if (refund == null) refund = findWoodInRecipe(player, cc);
            if (refund != null) {
                player.getInventory().add(new ItemStack(refund, 1));
            }
        });
    }

    // Деревянный сорт по фактическим остаткам в крафт-сетке (точный вариант).
    private static net.minecraft.world.item.Item findWoodInGrid(
            net.minecraft.world.inventory.CraftingContainer cc) {
        for (int i = 0; i < cc.getContainerSize(); i++) {
            ItemStack s = cc.getItem(i);
            if (!s.isEmpty() && isWoodIngredient(s)) return s.getItem();
        }
        return null;
    }

    // Запасной путь: деревянный ингредиент из совпавшего рецепта (когда сетка
    // уже опустошена). Берём первый деревянный вариант первого подходящего
    // ингредиента.
    private static net.minecraft.world.item.Item findWoodInRecipe(
            Player player, net.minecraft.world.inventory.CraftingContainer cc) {
        net.minecraft.world.item.crafting.CraftingInput input =
                net.minecraft.world.item.crafting.CraftingInput.of(cc.getWidth(), cc.getHeight(), cc.getItems());
        var recipe = player.level().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, player.level());
        if (recipe.isEmpty()) return null;
        for (net.minecraft.world.item.crafting.Ingredient ing : recipe.get().value().getIngredients()) {
            if (ing.isEmpty()) continue;
            for (ItemStack candidate : ing.getItems()) {
                if (isWoodIngredient(candidate)) return candidate.getItem();
            }
        }
        return null;
    }

    // Партия сверх нормы: при крафте базовых заготовок (доски/палки) шанс
    // получить бонусную пачку — переработка бревна у мастера эффективнее.
    // Шанс level*0.04 (макс 40% на ур.10); доски +2, палки +1.
    private static void checkCarpenterBatch(Player player, ItemStack result) {
        if (!isBaseStock(result)) return;

        withData(player, data -> {
            int level = data.getSpecializationLevel(Spec.CARPENTER.id);
            if (level <= 0) return;

            float chance = Math.min(0.4f, level * 0.04f);
            if (player.level().random.nextFloat() >= chance) return;

            int extra = result.is(net.minecraft.world.item.Items.STICK) ? 1 : 2;
            ItemStack bonus = new ItemStack(result.getItem(), extra);
            if (!player.getInventory().add(bonus)) player.drop(bonus, false);
        });
    }

    private static boolean isBaseStock(ItemStack s) {
        return s.is(net.minecraft.tags.ItemTags.PLANKS)
                || s.is(net.minecraft.world.item.Items.STICK);
    }

    // Обработанное дерево (НЕ бревно) — блок, чей предмет крафтит Плотник.
    // Используется Лесорубом для бонуса скорости разборки деревянных построек.
    private static boolean isWorkedWood(Block block) {
        net.minecraft.world.item.Item item = block.asItem();
        if (item == net.minecraft.world.item.Items.AIR) return false;
        CraftEntry e = ItemCraftMap.get(item);
        return e != null && e.spec() == Spec.CARPENTER;
    }

    // Строительный размах: бонус дальности блоков. Ур.5 = +1, ур.10 = +2
    // (линейно +0.2/уровень от 5). Transient-модификатор — переустанавливается
    // при синке/респавне/смене измерения (атрибуты сбрасываются у нового entity).
    // Дальность блоков Плотника — модификатор атрибута BLOCK_INTERACTION_RANGE.
    private static final net.minecraft.resources.ResourceLocation CARPENTER_REACH_ID =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(KingdomRPCore.MODID, "carpenter_reach");

    public static void refreshBlockReach(ServerPlayer player) {
        var attr = player.getAttribute(
                net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE);
        if (attr == null) return;
        if (attr.getModifier(CARPENTER_REACH_ID) != null) {
            attr.removeModifier(CARPENTER_REACH_ID);
        }

        int level = player.getData(KRPAttachments.PLAYER_DATA).getSpecializationLevel(Spec.CARPENTER.id);
        if (level < 5) return;

        double bonus = 1.0 + (level - 5) * 0.2;
        attr.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                CARPENTER_REACH_ID, bonus,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
    }

    private static boolean isWoodIngredient(ItemStack stack) {
        if (stack.is(net.minecraft.tags.ItemTags.PLANKS)
                || stack.is(net.minecraft.tags.ItemTags.LOGS)) return true;
        var item = stack.getItem();
        return item == net.minecraft.world.item.Items.STICK
                || item == net.minecraft.world.item.Items.BAMBOO;
    }

    // Экономия материала Мастерового: при крафте натурального изделия шанс
    // вернуть 1 ед. натурального ингредиента (кожа/нить/шерсть/глина/крольчья
    // шкура) обратно в инвентарь. Аналог экономии древесины Плотника. Шанс
    // линейный: level*0.05, макс 50% на ур.10. Сорт определяем точно: сперва по
    // остатку в крафт-сетке, иначе по совпавшему рецепту.
    private static void checkCraftsmanEconomy(Player player, ItemStack result,
                                              net.minecraft.world.Container craftMatrix) {
        var entry = ItemCraftMap.get(result.getItem());
        if (entry == null || entry.spec() != Spec.CRAFTSMAN) return;
        if (!(craftMatrix instanceof net.minecraft.world.inventory.CraftingContainer cc)) return;

        withData(player, data -> {
            int level = data.getSpecializationLevel(Spec.CRAFTSMAN.id);
            if (level <= 0) return;

            float chance = Math.min(0.5f, level * 0.05f);
            if (player.level().random.nextFloat() >= chance) return;

            net.minecraft.world.item.Item refund = findNaturalInGrid(cc);
            if (refund == null) refund = findNaturalInRecipe(player, cc);
            if (refund != null) {
                player.getInventory().add(new ItemStack(refund, 1));
            }
        });
    }

    private static net.minecraft.world.item.Item findNaturalInGrid(
            net.minecraft.world.inventory.CraftingContainer cc) {
        for (int i = 0; i < cc.getContainerSize(); i++) {
            ItemStack s = cc.getItem(i);
            if (!s.isEmpty() && isNaturalIngredient(s)) return s.getItem();
        }
        return null;
    }

    private static net.minecraft.world.item.Item findNaturalInRecipe(
            Player player, net.minecraft.world.inventory.CraftingContainer cc) {
        net.minecraft.world.item.crafting.CraftingInput input =
                net.minecraft.world.item.crafting.CraftingInput.of(cc.getWidth(), cc.getHeight(), cc.getItems());
        var recipe = player.level().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, player.level());
        if (recipe.isEmpty()) return null;
        for (net.minecraft.world.item.crafting.Ingredient ing : recipe.get().value().getIngredients()) {
            if (ing.isEmpty()) continue;
            for (ItemStack candidate : ing.getItems()) {
                if (isNaturalIngredient(candidate)) return candidate.getItem();
            }
        }
        return null;
    }

    private static boolean isNaturalIngredient(ItemStack stack) {
        if (stack.is(net.minecraft.tags.ItemTags.WOOL)) return true;
        var item = stack.getItem();
        return item == net.minecraft.world.item.Items.LEATHER
                || item == net.minecraft.world.item.Items.STRING
                || item == net.minecraft.world.item.Items.CLAY_BALL
                || item == net.minecraft.world.item.Items.RABBIT_HIDE;
    }

    // Закалка Мастерового: свежесделанное изделие из натуральных материалов
    // (сейчас — кожаная броня) получает прочность по уровню (50% на открытии тира
    // → 100% к уровню полной прочности). Аналог закалки Кузнеца; применяется
    // только на крафте. Карта расширяема под модовую броню (CraftsmanTemperMap).
    private static void applyCraftsmanTempering(Player player, ItemStack result) {
        if (!result.isDamageableItem()) return;
        BlacksmithTemperMap.TemperTier tier = CraftsmanTemperMap.get(result.getItem());
        if (tier == null) return;

        withData(player, data -> {
            int level = data.getSpecializationLevel(Spec.CRAFTSMAN.id);
            float quality = BlacksmithTemperMap.quality(tier, level);
            if (quality >= 1.0f) return;
            int maxDur = result.getMaxDamage();
            int damage = Math.round((1f - quality) * maxDur);
            damage = Math.min(damage, maxDur - 1); // не отдаём «сломанный» предмет
            result.setDamageValue(damage);
        });
    }

    // Партия сверх нормы Мастерового: при крафте базового строительного блока
    // (массовая переработка сырья — стекло→панели, песок→песчаник, камень→кирпич
    // и т.п.) шанс получить бонусную пачку +2. Аналог «партии» Плотника (доски/палки).
    private static void checkCraftsmanBatch(Player player, ItemStack result) {
        if (!isCraftsmanBaseStock(result)) return;

        withData(player, data -> {
            int level = data.getSpecializationLevel(Spec.CRAFTSMAN.id);
            if (level <= 0) return;

            float chance = Math.min(0.4f, level * 0.04f);
            if (player.level().random.nextFloat() >= chance) return;

            ItemStack bonus = new ItemStack(result.getItem(), 2);
            if (!player.getInventory().add(bonus)) player.drop(bonus, false);
        });
    }

    // Базовые строительные блоки Мастерового — фундаментальная единица стройки,
    // получаемая из сырья в количестве (стекло/песок/камень/глина/кварц).
    private static boolean isCraftsmanBaseStock(ItemStack s) {
        var i = s.getItem();
        return i == net.minecraft.world.item.Items.GLASS_PANE
                || i == net.minecraft.world.item.Items.BRICKS
                || i == net.minecraft.world.item.Items.STONE_BRICKS
                || i == net.minecraft.world.item.Items.NETHER_BRICKS
                || i == net.minecraft.world.item.Items.MUD_BRICKS
                || i == net.minecraft.world.item.Items.SANDSTONE
                || i == net.minecraft.world.item.Items.RED_SANDSTONE
                || i == net.minecraft.world.item.Items.QUARTZ_BLOCK
                || i == net.minecraft.world.item.Items.CLAY;
    }

    // ================================================================
    // ФЕРМЕР
    // ================================================================

    // Сохранение прочности мотыги: 3%/уровень, макс 30% на ур.10.
    // Откатываем урон на следующий тик с клампом к снимку «до» (как у топора
    // Лесоруба) — корректно стакается с Unbreaking.
    private static void applyHoeDurability(ServerPlayer player,
                                           PlayerInteractEvent.RightClickBlock event) {
        ItemStack hoe = player.getMainHandItem();
        if (!(hoe.getItem() instanceof net.minecraft.world.item.HoeItem)) return;
        if (!hoe.isDamageableItem()) return;

        withData(player, data -> {
            int level = data.getSpecializationLevel(Spec.FARMER.id);
            if (level <= 0) return;

            float keepChance = Math.min(0.3f, level * 0.03f);
            if (player.level().random.nextFloat() < keepChance) {
                int before = hoe.getDamageValue();
                ((ServerLevel) player.level()).getServer().execute(() -> {
                    if (hoe.isDamageableItem()) {
                        hoe.setDamageValue(Math.min(before, hoe.getDamageValue()));
                    }
                });
            }
        });
    }

    // «Комбайн» с ур.5: сбор спелой культуры в ПКМ + авто-пересадка (без шанса).
    // Один сид из дропа уходит на пересадку. Двойной дроп применяется как обычно.
    private static void applyCombineHarvest(ServerPlayer player,
                                            PlayerInteractEvent.RightClickBlock event) {
        // Не мешаем костной муке
        if (player.getMainHandItem().getItem()
                instanceof net.minecraft.world.item.BoneMealItem) return;

        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = event.getPos();
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CropBlock crop)) return;
        if (!crop.isMaxAge(state)) return;

        net.minecraft.world.item.Item seed = combineSeed(state.getBlock());
        if (seed == null) return; // комбайн только для базовых культур

        withData(player, data -> {
            int farmerLevel = data.getSpecializationLevel(Spec.FARMER.id);
            if (farmerLevel < 5) return;

            event.setCanceled(true);

            java.util.List<ItemStack> drops = Block.getDrops(
                    state, level, pos, level.getBlockEntity(pos),
                    player, player.getMainHandItem());

            // Один сид уходит на пересадку
            removeOne(drops, seed);

            boolean doubled = level.random.nextFloat() < farmerLevel * 0.05f;
            for (ItemStack drop : drops) {
                giveToPlayer(player, drop.copy());
                if (doubled) giveToPlayer(player, drop.copy());
            }

            // Пересадка (age 0)
            level.setBlock(pos, crop.getStateForAge(0), 3);

            // XP за сбор
            BlockEntry entry = BlockXPMap.get(state.getBlock());
            if (entry != null) {
                XPSystem.giveXP(player, Path.HARVEST, entry.xpReward());
            }
        });
    }

    // Гейтинг готовки на костре: нельзя класть сырьё, чей готовый продукт
    // требует более высокого уровня Повара. Отмена ПКМ + ресинк инвентаря
    // (клиент мог предсказать укладку) + сообщение.
    private static void applyCampfireGate(ServerPlayer player,
                                          PlayerInteractEvent.RightClickBlock event) {
        if (!(player.level().getBlockEntity(event.getPos())
                instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity campfire)) return;

        net.minecraft.world.item.Item result =
                CookSystem.campfireResult(player.level(), campfire, event.getItemStack());
        if (result == null) return; // нет рецепта/свободного слота — не наше дело
        if (CookSystem.canProduce(player, result)) return;

        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        player.containerMenu.sendAllDataToRemote();
        CookSystem.sendRestriction(player, result);
    }

    private static net.minecraft.world.item.Item combineSeed(Block block) {
        if (block == Blocks.WHEAT) return net.minecraft.world.item.Items.WHEAT_SEEDS;
        if (block == Blocks.CARROTS) return net.minecraft.world.item.Items.CARROT;
        if (block == Blocks.POTATOES) return net.minecraft.world.item.Items.POTATO;
        if (block == Blocks.BEETROOTS) return net.minecraft.world.item.Items.BEETROOT_SEEDS;
        return null;
    }

    private static void removeOne(java.util.List<ItemStack> drops,
                                  net.minecraft.world.item.Item item) {
        for (ItemStack s : drops) {
            if (!s.isEmpty() && s.getItem() == item) {
                s.shrink(1);
                return;
            }
        }
    }

    private static void giveToPlayer(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    // ================================================================
    // УТИЛИТЫ
    // ================================================================

    private static void fellTree(ServerPlayer player, BlockPos startPos) {
        var level = (ServerLevel) player.level();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new java.util.LinkedList<>();
        queue.add(startPos);

        while (!queue.isEmpty() && visited.size() < MAX_TREE_BLOCKS) {
            BlockPos pos = queue.poll();
            if (visited.contains(pos)) continue;
            if (!isLog(level.getBlockState(pos).getBlock())) continue;
            if (PlacedBlockTracker.isPlacedByPlayer(pos)) continue;

            visited.add(pos);
            level.destroyBlock(pos, true, player);

            for (var offset : new BlockPos[]{
                    pos.above(), pos.below(),
                    pos.north(), pos.south(),
                    pos.east(), pos.west(),
                    pos.above().north(), pos.above().south(),
                    pos.above().east(), pos.above().west()
            }) {
                if (!visited.contains(offset)) queue.add(offset);
            }
        }
    }

    private static boolean isLog(Block block) {
        return block == Blocks.OAK_LOG || block == Blocks.BIRCH_LOG
                || block == Blocks.SPRUCE_LOG || block == Blocks.JUNGLE_LOG
                || block == Blocks.ACACIA_LOG || block == Blocks.DARK_OAK_LOG
                || block == Blocks.MANGROVE_LOG || block == Blocks.CHERRY_LOG
                || block == Blocks.CRIMSON_STEM || block == Blocks.WARPED_STEM
                || block == Blocks.BROWN_MUSHROOM_BLOCK
                || block == Blocks.RED_MUSHROOM_BLOCK
                || block == Blocks.MUSHROOM_STEM;
    }
}