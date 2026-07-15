package com.kingdomrp.core.client;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.client.screen.KingdomBlockScreen;
import com.kingdomrp.core.registry.KRPMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Клиентская регистрация экранов меню (шина мода). */
@EventBusSubscriber(modid = KingdomRPCore.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class KRPClientSetup {

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(KRPMenus.KINGDOM_BLOCK.get(), KingdomBlockScreen::new);
    }
}
