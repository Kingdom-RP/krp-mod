package com.kingdomrp.core.client;

import com.kingdomrp.core.client.screen.KingdomHubScreen;
import com.kingdomrp.core.client.screen.SignCharterScreen;
import net.minecraft.client.Minecraft;

/**
 * Клиентские точки входа UI королевств. Класс грузится ТОЛЬКО на клиенте
 * (вызовы гейтятся {@code level.isClientSide} / Dist.CLIENT).
 */
public final class KingdomClientHooks {

    private KingdomClientHooks() {}

    public static void openHub() {
        Minecraft.getInstance().setScreen(new KingdomHubScreen());
    }

    public static void openSignCharter() {
        Minecraft.getInstance().setScreen(new SignCharterScreen());
    }

    /** Пересобрать открытый экран королевства при приходе свежего sync. */
    public static void onKingdomSync() {
        if (Minecraft.getInstance().screen instanceof KingdomSyncListener l) l.onKingdomSync();
    }
}
