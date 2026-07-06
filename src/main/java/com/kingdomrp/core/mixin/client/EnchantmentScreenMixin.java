package com.kingdomrp.core.mixin.client;

import com.kingdomrp.core.registry.KRPAttachments;
import com.kingdomrp.core.data.map.tier.EnchantTierMap;
import com.kingdomrp.core.data.type.Spec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Клиентская подсветка лестницы доступа Зачарователя на столе зачарования:
 * варианты сверх доступного по уровню числа ({@link EnchantTierMap#slotCount})
 * затемняются и не реагируют на клик. Серверный отказ — в
 * {@code EnchantmentMenuMixin}; здесь чисто визуально-клиентская защита.
 */
@Mixin(value = EnchantmentScreen.class, remap = false)
public class EnchantmentScreenMixin {

    @Unique
    private static int krp$allowedSlots() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 3;
        int level = mc.player.getData(KRPAttachments.PLAYER_DATA).getSpecializationLevel(Spec.ENCHANTER.id);
        return EnchantTierMap.slotCount(level);
    }

    @Inject(method = "render", at = @At("TAIL"), remap = false)
    private void krp$dimLocked(GuiGraphics graphics, int mouseX, int mouseY, float partial,
                               CallbackInfo ci) {
        int allowed = krp$allowedSlots();
        if (allowed >= 3) return;

        EnchantmentScreen self = (EnchantmentScreen) (Object) this;
        EnchantmentMenu menu = self.getMenu();
        int left = self.getGuiLeft();
        int top = self.getGuiTop();

        for (int slot = allowed; slot < 3; slot++) {
            if (menu.costs[slot] <= 0) continue;
            int x = left + 60;
            int y = top + 14 + 19 * slot;
            graphics.fill(x, y, x + 108, y + 19, 0xC0101010);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, remap = false)
    private void krp$blockLockedClick(double mouseX, double mouseY, int button,
                                      CallbackInfoReturnable<Boolean> cir) {
        int allowed = krp$allowedSlots();
        if (allowed >= 3) return;

        EnchantmentScreen self = (EnchantmentScreen) (Object) this;
        EnchantmentMenu menu = self.getMenu();
        int left = self.getGuiLeft();
        int top = self.getGuiTop();

        for (int slot = allowed; slot < 3; slot++) {
            if (menu.costs[slot] <= 0) continue;
            double dx = mouseX - (left + 60);
            double dy = mouseY - (top + 14 + 19 * slot);
            if (dx >= 0 && dy >= 0 && dx < 108 && dy < 19) {
                cir.setReturnValue(true); // клик по запертому варианту — поглощаем
                return;
            }
        }
    }
}
