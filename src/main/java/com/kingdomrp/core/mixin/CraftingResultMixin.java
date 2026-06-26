package com.kingdomrp.core.mixin;

import com.kingdomrp.core.system.SpecializationEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Закалка результата крафта (Кузнец/Мастеровой) при СБОРКЕ результата в
 * {@link CraftingMenu#slotChangedCraftingGrid} (ДО {@code ResultContainer.setItem}).
 * Так оба пути изъятия (клик и shift-click) берут уже закалённый предмет, и превью
 * слота показывает корректную прочность. Через этот же метод идёт и сетка 2×2.
 */
@Mixin(value = CraftingMenu.class, remap = false)
public class CraftingResultMixin {

    @Redirect(
            method = "slotChangedCraftingGrid",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V"),
            remap = false)
    private static void krp$temperCraftResult(ResultContainer resultContainer, int index, ItemStack stack,
                                              AbstractContainerMenu menu, Level level, Player player,
                                              CraftingContainer craftSlots, ResultContainer resultSlots,
                                              net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe> recipe) {
        SpecializationEffects.applyTemperingToCraftResult(player, stack);
        resultContainer.setItem(index, stack);
    }
}
