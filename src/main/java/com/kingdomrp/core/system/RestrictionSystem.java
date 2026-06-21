package com.kingdomrp.core.system;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.registry.KRPAttachments;
import com.kingdomrp.core.config.KRPConfig;
import com.kingdomrp.core.data.BannedCraftMap;
import com.kingdomrp.core.data.ItemCraftTierMap;
import com.kingdomrp.core.data.ItemUseTierMap;
import com.kingdomrp.core.data.SpecRequirement;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;

@EventBusSubscriber(modid = KingdomRPCore.MODID)
public class RestrictionSystem {

    @SubscribeEvent
    public static void onItemUse(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        SpecRequirement req = RestrictionSystem.getUseRequirement(event.getItemStack());
        if (req == null) return;

        if (!meetsRequirement(player, req)) {
            event.setCanceled(true);
            sendRestrictionMessage(player, req);
            // Клиент предсказал надевание брони по ПКМ — без ресинка остаётся
            // фантом (визуально надета, в инвентаре "испаряется" при клике).
            // Принудительно ресинкаем инвентарь (грабли №12).
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
        SpecRequirement useReq = RestrictionSystem.getUseRequirement(event.getItemStack());
        if (useReq != null) {
            event.getToolTip().add(Component.literal(
                    "§7Требует: §e" + XPSystem.getSpecName(useReq.spec().id) + " §7ур. §f" + useReq.level()
            ));
        }
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        EquipmentSlot slot = event.getSlot();
        if (slot != EquipmentSlot.HEAD && slot != EquipmentSlot.CHEST
                && slot != EquipmentSlot.LEGS && slot != EquipmentSlot.FEET) return;

        SpecRequirement req = RestrictionSystem.getUseRequirement(event.getTo());
        if (req == null) return;

        if (!meetsRequirement(player, req)) {
            player.setItemSlot(slot, ItemStack.EMPTY);
            player.getInventory().add(event.getTo());
            sendRestrictionMessage(player, req);
        }
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

    public static void sendRestrictionMessage(Player player, SpecRequirement req) {
        player.sendSystemMessage(Component.literal(
                "§c[Kingdom RP] Требуется навык «" + XPSystem.getSpecName(req.spec().id)
                        + "» уровня " + req.level() + "!"
        ));
    }

    /** Список требований для крафта (все обязательны), либо null. */
    public static List<SpecRequirement> getCraftRequirements(ItemStack stack) {
        if (!KRPConfig.RESTRICTIONS_ENABLED.get()) return null;
        if (stack.isEmpty()) return null;
        return ItemCraftTierMap.get(stack.getItem());
    }

    public static SpecRequirement getUseRequirement(ItemStack stack) {
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
                    "§c[Kingdom RP] Крафт этого предмета отключён на сервере (анти-грифинг)."));
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
        SpecRequirement req = com.kingdomrp.core.data.SmeltTierMap.get(resultItem);
        return req != null && !meetsRequirement(player, req);
    }

    /** Сообщение о блокировке переплавки (по факту попытки положить сырьё). */
    public static void sendSmeltRestriction(Player player, net.minecraft.world.item.Item resultItem) {
        SpecRequirement req = com.kingdomrp.core.data.SmeltTierMap.get(resultItem);
        if (req != null) sendRestrictionMessage(player, req);
    }

}
