package com.kingdomrp.core.compat.mixin.client;

import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.EmiScreenManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * «Distraction-free» EMI (часть 1 — рендер): индекс-панель (список всех предметов)
 * не рисуется, пока строка поиска EMI пуста; вместо неё — подсказка «начните искать».
 * Блокировку наведения/тултипа/клика по скрытым предметам делает
 * {@link EmiScreenManagerMixin} (перехват {@code getHoveredStack}). Аналог
 * txnimc/DistractionFreeRecipes, только EMI, без TxniLib.
 * <p>
 * Гейт: панель поиска ({@code isSearch()}) + пустой {@code EmiScreenManager.search}.
 * Активен только если EMI загружен (см. {@link com.kingdomrp.core.compat.EmiMixinPlugin}).
 */
@Mixin(value = EmiScreenManager.SidebarPanel.class, remap = false)
public abstract class EmiSidebarPanelMixin {

    private static final Component HINT = Component.translatable("gui.kingdomrpcore.emi_search_hint");

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private void krp$hideRender(EmiDrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        EmiScreenManager.SidebarPanel self = (EmiScreenManager.SidebarPanel) (Object) this;
        if (!self.isSearch()
                || EmiScreenManager.search == null
                || !EmiScreenManager.search.getValue().isEmpty()) {
            return;
        }
        krp$drawHint(ctx, self.getBounds());
        ci.cancel();
    }

    private static void krp$drawHint(EmiDrawContext ctx, Bounds b) {
        if (b == null || b.empty()) return;
        Font font = Minecraft.getInstance().font;
        int maxW = Math.max(20, b.right() - b.left() - 8);
        List<FormattedCharSequence> lines = font.split(HINT, maxW);
        int cx = (b.left() + b.right()) / 2;
        int y = (b.top() + b.bottom()) / 2 - (lines.size() * font.lineHeight) / 2;
        for (FormattedCharSequence line : lines) {
            ctx.raw().drawString(font, line, cx - font.width(line) / 2, y, 0xFFAAAAAA, false);
            y += font.lineHeight;
        }
    }
}
