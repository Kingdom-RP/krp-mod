package com.kingdomrp.core.client;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.data.type.SpecRequirement;
import com.kingdomrp.core.system.RestrictionSystem;
import com.kingdomrp.core.system.XPSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;

/**
 * Клиентская подсказка о требуемом уровне для КРАФТА — показывается ТОЛЬКО когда
 * открыта крафт-сетка: верстак ({@link CraftingMenu}) или 2×2 в инвентаре
 * ({@link InventoryMenu}), а не при наведении на предмет в сундуке или где-либо ещё.
 * <p>
 * Клиентский класс ({@code Dist.CLIENT}) — нужен доступ к текущему меню
 * ({@code Minecraft.getInstance}). Требование для НОШЕНИЯ показывает общий
 * {@code RestrictionSystem.onTooltip}.
 */
@EventBusSubscriber(modid = KingdomRPCore.MODID, value = Dist.CLIENT)
public class CraftTooltipClient {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var menu = mc.player.containerMenu;
        if (!(menu instanceof CraftingMenu) && !(menu instanceof InventoryMenu)) return;

        List<SpecRequirement> reqs = RestrictionSystem.getCraftRequirements(event.getItemStack());
        if (reqs == null) return;
        for (SpecRequirement req : reqs) {
            event.getToolTip().add(Component.literal(
                    "§7Крафт требует: §e" + XPSystem.getSpecName(req.spec().id)
                            + " §7ур. §f" + req.level()));
        }
    }
}
