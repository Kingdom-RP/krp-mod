package com.kingdomrp.core.client;

import com.kingdomrp.core.KingdomRPCore;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Клиентские события МОД-шины (setup-фаза). Отдельно от {@link ClientEvents}
 * (игровая шина), т.к. разные шины.
 */
@EventBusSubscriber(modid = KingdomRPCore.MODID, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // GLFW-вызовы — на главном потоке (enqueueWork); окно уже создано.
        event.enqueueWork(WindowIcon::apply);
    }
}
