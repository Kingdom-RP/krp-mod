package com.kingdomrp.core.mixin;

import com.kingdomrp.core.system.CookSystem;
import com.kingdomrp.core.system.XPSystem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * XP за готовку/плавку при изъятии продукта из печи/коптильни (гейтинг — на входе,
 * {@link AbstractFurnaceMenuMixin}). Инжект в {@code checkTakeAchievements} — единая
 * точка всех путей изъятия; {@code removeCount} = фактически забранное количество.
 */
@Mixin(value = FurnaceResultSlot.class, remap = false)
public abstract class FurnaceResultSlotMixin {

    @Shadow @Final private Player player;
    @Shadow private int removeCount;

    @Inject(method = "checkTakeAchievements", at = @At("HEAD"), remap = false)
    private void krp$cookXP(ItemStack stack, CallbackInfo ci) {
        if (this.removeCount <= 0) return;
        ItemStack counted = stack.copy();
        counted.setCount(this.removeCount);
        CookSystem.onCooked(this.player, counted);          // Повар — еда
        XPSystem.onMetalSmelted(this.player, counted);      // Кузнец — металл
        XPSystem.onNaturalSmelted(this.player, counted);    // Мастеровой — натуральные материалы
    }
}
