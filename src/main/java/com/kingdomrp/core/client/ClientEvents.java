package com.kingdomrp.core.client;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.client.screen.PathScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = KingdomRPCore.MODID, value = Dist.CLIENT)
public class ClientEvents {

    // 1.21: TickEvent удалён → ClientTickEvent.Post (фаза END уже подразумевается).
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        while (KeyBindings.OPEN_PATHS.consumeClick()) {
            mc.setScreen(new PathScreen());
        }
    }
}
