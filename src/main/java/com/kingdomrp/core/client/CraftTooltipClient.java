package com.kingdomrp.core.client;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.data.SpecRequirement;
import com.kingdomrp.core.system.RestrictionSystem;
import com.kingdomrp.core.system.XPSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.CraftingMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;

/**
 * Клиентская подсказка о требуемом уровне для КРАФТА — показывается ТОЛЬКО когда
 * открыто меню верстака ({@link CraftingMenu}), а не при наведении на предмет в
 * инвентаре, сундуке или где-либо ещё.
 * <p>
 * Вынесена в клиентский класс ({@code Dist.CLIENT}), т.к. нужен доступ к текущему
 * меню ({@code Minecraft.getInstance}); на выделенном сервере не грузится (грабли
 * №0). Требование для НОШЕНИЯ показывается всегда — оно в общем
 * {@code RestrictionSystem.onTooltip}.
 */
@EventBusSubscriber(modid = KingdomRPCore.MODID, value = Dist.CLIENT)
public class CraftTooltipClient {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!(mc.player.containerMenu instanceof CraftingMenu)) return;

        List<SpecRequirement> reqs = RestrictionSystem.getCraftRequirements(event.getItemStack());
        if (reqs == null) return;
        for (SpecRequirement req : reqs) {
            event.getToolTip().add(Component.literal(
                    "§7Крафт требует: §e" + XPSystem.getSpecName(req.spec().id)
                            + " §7ур. §f" + req.level()));
        }
    }
}
