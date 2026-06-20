package com.kingdomrp.core.client;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.client.screen.PathScreen;
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

    // 1.21: TickEvent удалён → ClientTickEvent.Post (фаза END уже подразумевается).
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        while (KeyBindings.OPEN_PATHS.consumeClick()) {
            mc.setScreen(new PathScreen());
        }
    }

    /**
     * Чистка главного меню: убираем кнопку Realms ({@code menu.online}) и
     * ссылку-копирайт «Mojang AB» ({@code title.credits}), затем подтягиваем все
     * виджеты, что были НИЖЕ кнопки Realms, вверх на её высоту (24 px) — чтобы не
     * оставалось пустого места. Делается на штатном {@code ScreenEvent.Init.Post}
     * (а не миксином), чтобы не шадоуить {@code Screen.removeWidget}.
     */
    @SubscribeEvent
    public static void onTitleScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen)) return;

        GuiEventListener realms = null;
        GuiEventListener copyright = null;
        for (GuiEventListener w : event.getListenersList()) {
            if (w instanceof AbstractWidget aw
                    && aw.getMessage().getContents() instanceof TranslatableContents tc) {
                if ("menu.online".equals(tc.getKey())) realms = w;
                else if ("title.credits".equals(tc.getKey())) copyright = w;
            }
        }
        if (realms == null) return; // демо-меню или кнопки уже нет

        int realmsY = ((AbstractWidget) realms).getY();
        event.removeListener(realms);
        if (copyright != null) event.removeListener(copyright);

        // Сдвигаем всё, что было ниже Realms (моды/настройки/выход/язык/доступность),
        // вверх на высоту слота Realms — закрываем образовавшуюся дыру.
        for (GuiEventListener w : event.getListenersList()) {
            if (w instanceof AbstractWidget aw && aw.getY() >= realmsY) {
                aw.setY(aw.getY() - 24);
            }
        }
    }
}
