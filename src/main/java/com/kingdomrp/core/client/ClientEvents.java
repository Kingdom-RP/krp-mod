package com.kingdomrp.core.client;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.client.screen.KingdomHubScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = KingdomRPCore.MODID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        while (KeyBindings.OPEN_PATHS.consumeClick()) {
            mc.setScreen(new KingdomHubScreen());
        }
    }

    /**
     * Скрываем плашку copyright «Mojang AB» ({@code title.credits}) внизу справа.
     * Остальную компоновку главного меню (кнопка Realms, лого, splash) отдаём моду
     * FancyMenu — кнопки не удаляем и не сдвигаем. Делается на штатном
     * {@code ScreenEvent.Init.Post} (а не миксином).
     */
    @SubscribeEvent
    public static void onTitleScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen)) return;

        for (GuiEventListener w : event.getListenersList()) {
            if (w instanceof AbstractWidget aw
                    && aw.getMessage().getContents() instanceof TranslatableContents tc
                    && "title.credits".equals(tc.getKey())) {
                event.removeListener(w);
                break;
            }
        }
    }
}
