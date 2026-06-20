package com.kingdomrp.core.mixin.client;

import com.kingdomrp.core.KingdomRPCore;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.BiConsumer;

/**
 * Косметика главного меню Kingdom RP (часть в рендере):
 * <ul>
 *   <li>скрыты нижние-левые строки брендинга (версии Minecraft/NeoForge) —
 *       глушим {@code BrandingControl.forEachLine};</li>
 *   <li>лого Minecraft + «Java Edition» заменено текстурой-логотипом
 *       «Kingdom RP» ({@code assets/kingdomrpcore/textures/gui/title_logo.png}) —
 *       редирект {@code LogoRenderer.renderLogo} + blit.</li>
 * </ul>
 * Удаление и перекомпоновка кнопок (Realms, копирайт) — НЕ здесь, а через
 * {@code ScreenEvent.Init.Post} в {@code ClientEvents} (штатный API, без @Shadow
 * на унаследованный {@code Screen.removeWidget}).
 */
@Mixin(value = TitleScreen.class, remap = false)
public abstract class TitleScreenMixin {

    /** Скрываем нижние-левые строки версий (Minecraft/NeoForge/моды). */
    @Redirect(
            method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/internal/BrandingControl;forEachLine(ZZLjava/util/function/BiConsumer;)V"),
            remap = false)
    private void krp$hideBranding(boolean includeMC, boolean includeMods, BiConsumer<Integer, String> consumer) {
        // намеренно пусто — версии снизу слева не рисуем
    }

    /**
     * ВРЕМЕННО скрываем splash-текст (жёлтая подпись справа от лого). Позже сделаем
     * свои строки и вернём отрисовку.
     */
    @Redirect(
            method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/SplashRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/client/gui/Font;I)V"),
            remap = false)
    private void krp$hideSplash(SplashRenderer instance, GuiGraphics graphics, int centerX, Font font, int color) {
        // намеренно пусто — splash-текст временно отключён
    }

    /** Вместо лого Minecraft + «Java Edition» рисуем текстуру-логотип «Kingdom RP». */
    @Redirect(
            method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/LogoRenderer;renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IF)V"),
            remap = false)
    private void krp$customTitle(LogoRenderer instance, GuiGraphics graphics, int screenWidth, float alpha) {
        int drawW = 440;
        int drawH = drawW * LOGO_TEX_H / LOGO_TEX_W; // сохраняем пропорции
        int x = (screenWidth - drawW) / 2;
        int y = 30;

        RenderSystem.enableBlend();
        graphics.blit(LOGO, x, y, drawW, drawH, 0.0f, 0.0f,
                LOGO_TEX_W, LOGO_TEX_H, LOGO_TEX_W, LOGO_TEX_H);
        RenderSystem.disableBlend();
    }

    @org.spongepowered.asm.mixin.Unique
    private static final ResourceLocation LOGO =
            ResourceLocation.fromNamespaceAndPath(KingdomRPCore.MODID, "textures/gui/title_logo.png");
    @org.spongepowered.asm.mixin.Unique private static final int LOGO_TEX_W = 770;
    @org.spongepowered.asm.mixin.Unique private static final int LOGO_TEX_H = 123;
}
