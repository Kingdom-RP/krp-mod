package com.kingdomrp.core.mixin;

import com.kingdomrp.core.system.RestrictionSystem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Гейт крафта по уровню специализации: результат виден в слоте, но недоступен к
 * изъятию. Цель — базовый {@link Slot} с guard {@code instanceof ResultSlot};
 * {@code mayPickup == false} перекрывает все пути изъятия (клик/shift/Q/свап).
 * Сообщение — только на сервере, троттлится.
 */
@Mixin(value = Slot.class, remap = false)
public class SlotMixin {

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true, remap = false)
    private void krp$gateCraftPickup(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ResultSlot)) return;
        ItemStack result = ((Slot) (Object) this).getItem();
        if (result.isEmpty()) return;

        if (RestrictionSystem.isCraftBlocked(player, result)) {
            if (!player.level().isClientSide()) {
                RestrictionSystem.warnCraftBlocked(player, result);
            }
            cir.setReturnValue(false);
        }
    }
}
