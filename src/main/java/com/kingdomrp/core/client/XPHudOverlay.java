package com.kingdomrp.core.client;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.data.Path;
import com.kingdomrp.core.system.XPSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Полоска прогресса уровня пути сверху экрана. Появляется при получении XP
 * (пакет {@link com.kingdomrp.core.network.XPGainPacket}), показывает прогресс
 * к следующему уровню. Светло-синяя при обычном начислении, жёлтая при левел-апе
 * (плюс дефолтный звук левел-апа). На экране всегда не больше одной полоски —
 * состояние единое и статическое, каждый новый пакет его перезаписывает.
 * <p>
 * 1.21: GUI-оверлей — слой {@link LayeredDraw.Layer}, регистрируется через
 * {@link RegisterGuiLayersEvent}.
 */
@EventBusSubscriber(modid = KingdomRPCore.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class XPHudOverlay implements LayeredDraw.Layer {

    private static final long DISPLAY_MS = 3000L; // сколько держится полоска
    private static final long FADE_MS    = 500L;  // длительность затухания в конце

    private static final int BAR_WIDTH  = 182; // как у ванильной полоски опыта
    private static final int BAR_HEIGHT = 7;
    private static final int TOP_Y      = 12;
    private static final int SEGMENTS   = 20;  // число делений, как насечки ванили

    // Цвета заливки: основной / блик (верхняя строка) / тень (нижняя строка)
    private static final int BLUE_MAIN  = 0xFF55CCFF, BLUE_HI = 0xFFAEE9FF, BLUE_LO = 0xFF2E8FBF;
    private static final int YEL_MAIN   = 0xFFFFD21E, YEL_HI  = 0xFFFFEFA0, YEL_LO  = 0xFFB28A00;
    private static final int TRACK      = 0xFF202020; // тёмный жёлоб
    private static final int BORDER     = 0xFF000000;
    private static final int TICK       = 0x80000000; // полупрозрачные деления

    // Единственное активное состояние полоски
    private static int pathIndex = -1;
    private static float fraction = 0f;
    private static int level = 0;
    private static boolean leveledUp = false;
    private static long shownAt = 0L;

    /** Вызывается из обработчика {@link com.kingdomrp.core.network.XPGainPacket} на клиенте. */
    public static void onXPGain(int pathIndex, float currentXP, float requiredXP,
                                int level, boolean leveledUp) {
        XPHudOverlay.pathIndex = pathIndex;
        XPHudOverlay.level = level;
        XPHudOverlay.leveledUp = leveledUp;
        XPHudOverlay.shownAt = System.currentTimeMillis();

        float f = requiredXP > 0f ? currentXP / requiredXP : 0f;
        XPHudOverlay.fraction = Math.max(0f, Math.min(1f, f));

        if (leveledUp) {
            // При новом уровне полоска заполнена и жёлтая + дефолтный звук левел-апа
            XPHudOverlay.fraction = 1f;
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.0f));
        }
    }

    @SubscribeEvent
    public static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(KingdomRPCore.MODID, "xp_bar"),
                new XPHudOverlay());
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker deltaTracker) {
        if (pathIndex < 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        long elapsed = System.currentTimeMillis() - shownAt;
        if (elapsed > DISPLAY_MS) return;

        // Затухание в конце показа
        float alpha = 1f;
        if (elapsed > DISPLAY_MS - FADE_MS) {
            alpha = (DISPLAY_MS - elapsed) / (float) FADE_MS;
        }
        int a = Math.max(0, Math.min(255, (int) (alpha * 255)));

        int screenWidth = g.guiWidth();
        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = TOP_Y;

        int main = leveledUp ? YEL_MAIN : BLUE_MAIN;
        int hi   = leveledUp ? YEL_HI   : BLUE_HI;
        int lo   = leveledUp ? YEL_LO   : BLUE_LO;

        // Рамка + тёмный жёлоб
        g.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, withAlpha(BORDER, a));
        g.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, withAlpha(TRACK, a));

        // Заливка прогресса с бликом сверху и тенью снизу (объём «как у текстуры»)
        int filled = Math.round(BAR_WIDTH * fraction);
        if (filled > 0) {
            g.fill(x, y, x + filled, y + BAR_HEIGHT, withAlpha(main, a));
            g.fill(x, y, x + filled, y + 1, withAlpha(hi, a));                       // блик
            g.fill(x, y + BAR_HEIGHT - 1, x + filled, y + BAR_HEIGHT, withAlpha(lo, a)); // тень
        }

        // Деления (насечки) поверх всей полоски — как у ванильного XP-бара
        for (int s = 1; s < SEGMENTS; s++) {
            int tx = x + Math.round(BAR_WIDTH * s / (float) SEGMENTS);
            g.fill(tx, y, tx + 1, y + BAR_HEIGHT, withAlpha(TICK, a));
        }

        // Подпись: название пути и уровень
        String label = pathName(pathIndex) + " — Ур. " + level;
        g.drawCenteredString(mc.font, Component.literal(label), screenWidth / 2, y - 10,
                withAlpha(0xFFFFFF, a));
    }

    // Масштабирует альфу цвета на множитель затухания (a в 0..255)
    private static int withAlpha(int argb, int a) {
        int baseA = (argb >>> 24) & 0xFF;
        int outA  = baseA * a / 255;
        return (outA << 24) | (argb & 0x00FFFFFF);
    }

    private static String pathName(int index) {
        for (Path p : Path.values()) {
            if (p.index == index) return XPSystem.getPathName(p);
        }
        return "";
    }
}
