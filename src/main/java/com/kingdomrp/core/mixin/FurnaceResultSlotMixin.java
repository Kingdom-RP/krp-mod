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
 * Повар: начисление XP за готовку при изъятии готового продукта из печи/коптильни.
 * <p>
 * Гейтинг готовки теперь на ВХОДЕ (см. {@link AbstractFurnaceMenuMixin}), поэтому
 * на выходе ограничений нет — любой продукт в печи был приготовлен из разрешённого
 * сырья, забрать его можно всегда.
 * <p>
 * XP вешаем на {@code checkTakeAchievements} — единую точку всех путей изъятия
 * ({@code onTake} и {@code onQuickCraft}); {@code removeCount} на момент вызова =
 * фактически забранное количество (ваниль обнуляет его сразу после, поэтому
 * повторный вызов в shift-click даёт 0 и не двоит XP).
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
