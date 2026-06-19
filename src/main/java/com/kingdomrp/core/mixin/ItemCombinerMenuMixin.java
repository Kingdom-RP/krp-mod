package com.kingdomrp.core.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Закрывает дюп shift-click'ом результата наковальни при применении книги:
 * {@code quickMoveStack} перемещает результат в инвентарь ДО вызова
 * {@code onTake}, поэтому шанс провала в {@code AnvilMenuMixin#onTake} не успел
 * бы отобрать предмет. Блокируем shift-взятие результата только для случая
 * «применение зачарованной книги» — обычный ремонт/переименование не затронуты.
 */
@Mixin(value = ItemCombinerMenu.class, remap = false)
public class ItemCombinerMenuMixin {

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true, remap = false)
    private void krp$blockBookApplyShiftTake(Player player, int index,
                                             CallbackInfoReturnable<ItemStack> cir) {
        if (!((Object) this instanceof AnvilMenu)) return;
        ItemCombinerMenu self = (ItemCombinerMenu) (Object) this;
        if (index != self.getResultSlot()) return;

        Container inputs = ((ItemCombinerMenuAccessor) (Object) this).getInputSlots();
        ItemStack book = inputs.getItem(1);
        if (book.is(Items.ENCHANTED_BOOK) && !EnchantmentHelper.getEnchantmentsForCrafting(book).isEmpty()) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
