package com.kingdomrp.core.system;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.registry.KRPAttachments;
import com.kingdomrp.core.config.KRPConfig;
import com.kingdomrp.core.data.map.BannedCraftMap;
import com.kingdomrp.core.data.map.tier.ItemCraftTierMap;
import com.kingdomrp.core.data.map.tier.ItemUseTierMap;
import com.kingdomrp.core.data.type.SpecRequirement;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;

@EventBusSubscriber(modid = KingdomRPCore.MODID)
public class RestrictionSystem {

    // Стол зачарования на 0 уровне Зачарователя не предлагает ни одной чары (базовая
    // группа — с ур.1). Без подсказки игрок видит пустой стол. Сообщаем при открытии.
    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!KRPConfig.RESTRICTIONS_ENABLED.get()) return;
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getContainer() instanceof net.minecraft.world.inventory.EnchantmentMenu)) return;
        if (com.kingdomrp.core.system.EnchantSystem.getEnchanterLevel(event.getEntity()) < 1) {
            event.getEntity().sendSystemMessage(Component.literal(
                    "§c[Kingdom RP] Зачарование на столе доступно с 1 уровня навыка «Зачарователь»."));
        }
    }

    @SubscribeEvent
    public static void onItemUse(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        List<SpecRequirement> req = RestrictionSystem.getUseRequirement(event.getItemStack());
        if (req == null) return;

        if (!meetsAny(player, req)) {
            event.setCanceled(true);
            sendRestrictionMessage(player, req);
            // Ресинк инвентаря: клиент предсказал надевание брони по ПКМ, иначе фантом.
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                sp.containerMenu.sendAllDataToRemote();
            }
        }
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        // Требование для ношения/использования — показывается всегда (актуально и
        // в инвентаре). Подсказка для КРАФТА — только в меню верстака, вынесена в
        // клиентский CraftTooltipClient (нужен доступ к открытому экрану).
        List<SpecRequirement> useReqs = RestrictionSystem.getUseRequirement(event.getItemStack());
        if (useReqs != null && !useReqs.isEmpty()) {
            StringBuilder sb = new StringBuilder("§7Требует: ");
            for (int i = 0; i < useReqs.size(); i++) {
                SpecRequirement r = useReqs.get(i);
                if (i > 0) sb.append(" §7или ");
                sb.append("§e").append(XPSystem.getSpecName(r.spec().id)).append(" §7ур. §f").append(r.level());
            }
            event.getToolTip().add(Component.literal(sb.toString()));
        }

        // F3+H (advanced): показываем ВСЕ настроенные ограничения предмета.
        if (event.getFlags().isAdvanced()) {
            addAdvancedRestrictions(event.getItemStack(), event.getToolTip());
        }
    }

    /** Полный список ограничений предмета из всех карт (для advanced-тултипа). */
    private static void addAdvancedRestrictions(ItemStack stack, List<Component> tip) {
        var item = stack.getItem();
        List<String> lines = new java.util.ArrayList<>();

        if (com.kingdomrp.core.data.map.BannedCraftMap.isBanned(item)) {
            lines.add("§cКрафт запрещён");
        }
        List<SpecRequirement> craft = ItemCraftTierMap.get(item);
        if (craft != null) {
            StringBuilder sb = new StringBuilder("крафт: ");
            for (int i = 0; i < craft.size(); i++) {
                SpecRequirement r = craft.get(i);
                if (i > 0) sb.append(", ");
                sb.append(XPSystem.getSpecName(r.spec().id)).append(" ур.").append(r.level());
            }
            lines.add(sb.toString());
        }
        List<SpecRequirement> use = ItemUseTierMap.get(item);
        if (use != null && !use.isEmpty()) {
            StringBuilder sb = new StringBuilder("ношение: ");
            for (int i = 0; i < use.size(); i++) {
                SpecRequirement r = use.get(i);
                if (i > 0) sb.append(" или ");
                sb.append(XPSystem.getSpecName(r.spec().id)).append(" ур.").append(r.level());
            }
            lines.add(sb.toString());
        }

        var food = com.kingdomrp.core.data.map.tier.FoodTierMap.get(item);
        if (food != null) lines.add("еда (произв.): " + XPSystem.getSpecName(food.spec().id) + " ур." + food.level());

        SpecRequirement smelt = com.kingdomrp.core.data.map.tier.SmeltTierMap.get(item);
        if (smelt != null) lines.add("переплавка: " + XPSystem.getSpecName(smelt.spec().id) + " ур." + smelt.level());

        if (item instanceof net.minecraft.world.item.BlockItem bi) {
            var block = bi.getBlock();
            var plant = com.kingdomrp.core.data.map.tier.PlantTierMap.get(block);
            if (plant != null) lines.add("посадка: " + XPSystem.getSpecName(plant.spec().id) + " ур." + plant.level());
            var harvest = com.kingdomrp.core.data.map.tier.BlockTierMap.get(block);
            if (harvest != null) lines.add("добыча: " + XPSystem.getSpecName(harvest.spec().id) + " ур." + harvest.level());
        }

        if (lines.isEmpty()) return;
        tip.add(Component.literal("§8[KRP] ограничения:"));
        for (String l : lines) tip.add(Component.literal("§8• " + l));
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        EquipmentSlot slot = event.getSlot();
        if (slot != EquipmentSlot.HEAD && slot != EquipmentSlot.CHEST
                && slot != EquipmentSlot.LEGS && slot != EquipmentSlot.FEET) return;

        List<SpecRequirement> req = RestrictionSystem.getUseRequirement(event.getTo());
        if (req != null && !meetsAny(player, req)) {
            player.setItemSlot(slot, ItemStack.EMPTY);
            player.getInventory().add(event.getTo());
            sendRestrictionMessage(player, req);
        }

        // Штраф скорости за броню — пересчёт по фактически надетому (после снятия).
        ArmorWeightHandler.recompute(player);
    }

    /** Анти-грифинг: закрываем доступ в Энд — отменяем телепорт в это измерение. */
    @SubscribeEvent
    public static void onTravelToDimension(
            net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent event) {
        if (!KRPConfig.ANTIGRIEF_CLOSE_END.get()) return;
        if (event.getDimension() != Level.END) return;
        event.setCanceled(true);
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            player.sendSystemMessage(Component.literal(
                    "§c[Kingdom RP] Мир Энда временно закрыт."));
        }
    }

    public static boolean meetsRequirement(Player player, SpecRequirement req) {
        return player.getData(KRPAttachments.PLAYER_DATA)
                .getSpecializationLevel(req.spec().id) >= req.level();
    }

    /** Выполнено ли ХОТЯ БЫ ОДНО требование (OR-семантика ношения). */
    public static boolean meetsAny(Player player, List<SpecRequirement> reqs) {
        if (reqs == null || reqs.isEmpty()) return true;
        for (SpecRequirement r : reqs) if (meetsRequirement(player, r)) return true;
        return false;
    }

    public static void sendRestrictionMessage(Player player, SpecRequirement req) {
        player.sendSystemMessage(Component.literal(
                "§c[Kingdom RP] Требуется навык «" + XPSystem.getSpecName(req.spec().id)
                        + "» уровня " + req.level() + "!"
        ));
    }

    /** Сообщение для OR-требований ношения: «Воин ур.1 или Лучник ур.2». */
    public static void sendRestrictionMessage(Player player, List<SpecRequirement> reqs) {
        StringBuilder sb = new StringBuilder("§c[Kingdom RP] Требуется навык ");
        for (int i = 0; i < reqs.size(); i++) {
            SpecRequirement r = reqs.get(i);
            if (i > 0) sb.append(" §cили ");
            sb.append("«").append(XPSystem.getSpecName(r.spec().id)).append("» уровня ").append(r.level());
        }
        sb.append("!");
        player.sendSystemMessage(Component.literal(sb.toString()));
    }

    /** Список требований для крафта (все обязательны), либо null. */
    public static List<SpecRequirement> getCraftRequirements(ItemStack stack) {
        if (!KRPConfig.RESTRICTIONS_ENABLED.get()) return null;
        if (stack.isEmpty()) return null;
        return ItemCraftTierMap.get(stack.getItem());
    }

    public static List<SpecRequirement> getUseRequirement(ItemStack stack) {
        if (!KRPConfig.RESTRICTIONS_ENABLED.get()) return null;
        if (stack.isEmpty()) return null;
        return ItemUseTierMap.get(stack.getItem());
    }

    // ================================================================
    // Гейт КРАФТА (вызывается из SlotMixin — блокирует изъятие результата)
    // ================================================================

    /** Троттлинг сообщений о блокировке крафта: по игроку → время последнего. */
    private static final java.util.Map<Player, Long> CRAFT_WARN =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    private static final long CRAFT_WARN_COOLDOWN_MS = 1500L;

    /** Жёсткий бан крафта (анти-грифинг) — независимо от тир-гейта и RESTRICTIONS_ENABLED. */
    public static boolean isCraftBanned(ItemStack result) {
        return KRPConfig.ANTIGRIEF_CRAFT_BAN.get() && !result.isEmpty()
                && BannedCraftMap.isBanned(result.getItem());
    }

    /** Заблокирован ли крафт предмета (анти-грифинг бан ИЛИ тир-гейт специализации ИЛИ гейт Повара). */
    public static boolean isCraftBlocked(Player player, ItemStack result) {
        if (result.isEmpty()) return false;
        if (isCraftBanned(result)) return true;
        List<SpecRequirement> reqs = getCraftRequirements(result);
        if (reqs != null) {
            for (SpecRequirement req : reqs) {
                if (!meetsRequirement(player, req)) return true;
            }
        }
        return !CookSystem.canProduce(player, result.getItem());
    }

    /** Сообщение о блокировке крафта на попытку изъятия (троттлится по времени). */
    public static void warnCraftBlocked(Player player, ItemStack result) {
        long now = System.currentTimeMillis();
        Long last = CRAFT_WARN.get(player);
        if (last != null && now - last < CRAFT_WARN_COOLDOWN_MS) return;
        CRAFT_WARN.put(player, now);

        if (isCraftBanned(result)) {
            player.sendSystemMessage(Component.literal(
                    "§c[Kingdom RP] Крафт этого предмета запрещён."));
            return;
        }

        List<SpecRequirement> reqs = getCraftRequirements(result);
        if (reqs != null) {
            for (SpecRequirement req : reqs) {
                if (!meetsRequirement(player, req)) {
                    sendRestrictionMessage(player, req);
                    return;
                }
            }
        }
        if (!CookSystem.canProduce(player, result.getItem())
                && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            CookSystem.sendRestriction(sp, result.getItem());
        }
    }

    // ================================================================
    // Гейт ПЕРЕПЛАВКИ металла (вызывается из CookGatedInputSlot — гейт входа печи)
    // ================================================================

    /** Заблокирована ли переплавка в данный результат по уровню Кузнеца. */
    public static boolean isSmeltBlocked(Player player, net.minecraft.world.item.Item resultItem) {
        if (!KRPConfig.RESTRICTIONS_ENABLED.get()) return false;
        SpecRequirement req = com.kingdomrp.core.data.map.tier.SmeltTierMap.get(resultItem);
        return req != null && !meetsRequirement(player, req);
    }

    /** Сообщение о блокировке переплавки (по факту попытки положить сырьё). */
    public static void sendSmeltRestriction(Player player, net.minecraft.world.item.Item resultItem) {
        SpecRequirement req = com.kingdomrp.core.data.map.tier.SmeltTierMap.get(resultItem);
        if (req != null) sendRestrictionMessage(player, req);
    }

}
