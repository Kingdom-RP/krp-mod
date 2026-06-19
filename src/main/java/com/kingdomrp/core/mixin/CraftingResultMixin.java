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
 * Закалка результата крафта (Кузнец/Мастеровой) применяется при СБОРКЕ результата
 * в слоте, а не на {@code ItemCraftedEvent}.
 * <p>
 * Причина (грабли №15): при shift-click {@code quickMoveStack} перемещает результат
 * в инвентарь отдельным стаком ДО вызова {@code onTake}/{@code ItemCraftedEvent},
 * поэтому правка {@code event.getCrafting()} не доходит до игрока (закалка терялась,
 * предмет выходил на 100%). Если же закалить стак результата в момент его сборки
 * ({@link CraftingMenu#slotChangedCraftingGrid}), то оба пути изъятия — обычный клик
 * и shift-click (через {@code split}/{@code copy}) — берут уже закалённый предмет.
 * Через этот же путь проходит и сетка 2×2 ({@code InventoryMenu} зовёт тот же метод).
 * <p>
 * Закаляем стак ДО {@code ResultContainer.setItem(0, result)} — так клиент получает
 * корректную прочность в превью слота результата.
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
