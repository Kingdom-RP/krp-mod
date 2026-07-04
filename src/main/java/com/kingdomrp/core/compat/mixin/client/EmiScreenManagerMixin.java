package com.kingdomrp.core.compat.mixin.client;

import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.screen.EmiScreenManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * «Distraction-free» EMI (часть 2 — ввод): пока строка поиска пуста, наведение на
 * область индекс-панели не возвращает предмет → нет тултипа и клика по скрытым
 * предметам. Перехват {@code getHoveredStack} (обе перегрузки — единый путь тултипа
 * и клика в EMI). Рендер прячет {@link EmiSidebarPanelMixin}.
 */
@Mixin(value = EmiScreenManager.class, remap = false)
public abstract class EmiScreenManagerMixin {

    private static boolean krp$blocked(int mouseX, int mouseY) {
        if (EmiScreenManager.search == null || !EmiScreenManager.search.getValue().isEmpty()) {
            return false;
        }
        EmiScreenManager.SidebarPanel panel = EmiScreenManager.getHoveredPanel(mouseX, mouseY);
        return panel != null && panel.isSearch();
    }

    @Inject(method = "getHoveredStack(IIZ)Ldev/emi/emi/api/stack/EmiStackInteraction;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void krp$blockHover(int mouseX, int mouseY, boolean notClick,
                                       CallbackInfoReturnable<EmiStackInteraction> cir) {
        if (krp$blocked(mouseX, mouseY)) cir.setReturnValue(EmiStackInteraction.EMPTY);
    }

    @Inject(method = "getHoveredStack(IIZZ)Ldev/emi/emi/api/stack/EmiStackInteraction;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void krp$blockHover2(int mouseX, int mouseY, boolean notClick, boolean showHidden,
                                        CallbackInfoReturnable<EmiStackInteraction> cir) {
        if (krp$blocked(mouseX, mouseY)) cir.setReturnValue(EmiStackInteraction.EMPTY);
    }
}
