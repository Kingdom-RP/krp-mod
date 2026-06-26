package com.kingdomrp.core.compat.mixin;

import com.kingdomrp.core.system.CookSystem;
import com.kingdomrp.core.system.RestrictionSystem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Гейт + XP для выходного слота Cooking Pot (Farmer's Delight). Цель по СТРОКЕ
 * (без зависимости от FD), применяется только если FD загружен — см.
 * {@link com.kingdomrp.core.compat.FDMixinPlugin}.
 * <p>
 * {@code CookingPotMealSlot} — слот-превью (mayPlace/mayPickup = false), брать
 * блюдо нельзя; реальное изъятие идёт через {@code CookingPotResultSlot}
 * ({@code SlotItemHandler} с {@code onTake}/{@code remove}, не ванильный
 * {@code ResultSlot}). Поэтому:
 * <ul>
 *   <li><b>Гейт</b> — в {@code remove(int)}: если уровень Повара недостаточен
 *       ({@link RestrictionSystem#isCraftBlocked}), возвращаем пустой стак →
 *       блюдо не извлекается (vanilla {@code safeTake} не зовёт {@code onTake}).</li>
 *   <li><b>XP за готовку</b> — в {@code onTake} через {@link CookSystem#onCooked}:
 *       единый путь с печью/коптильней/костром (XP из {@link com.kingdomrp.core.data.FoodCookMap},
 *       × количество, с access-гейтом). FD-еда заведена в FoodCookMap в
 *       {@link com.kingdomrp.core.compat.FarmersDelightCompat}.</li>
 * </ul>
 */
@Mixin(targets = "vectorwing.farmersdelight.common.block.entity.container.CookingPotResultSlot", remap = false)
public class CookingPotResultSlotMixin {

    @Shadow @Final private Player player;

    /** Гейт изъятия блюда по уровню Повара (и анти-грифинг). */
    @Inject(method = "remove", at = @At("HEAD"), cancellable = true, remap = false)
    private void krp$gateRemove(int amount, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack meal = ((Slot) (Object) this).getItem();
        if (meal.isEmpty()) return;
        if (RestrictionSystem.isCraftBlocked(player, meal)) {
            if (!player.level().isClientSide()) {
                RestrictionSystem.warnCraftBlocked(player, meal);
            }
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }

    /** XP за готовку в котелке — единый путь с ванильной готовкой (FoodCookMap). */
    @Inject(method = "onTake", at = @At("TAIL"), remap = false)
    private void krp$cookXp(Player taker, ItemStack stack, CallbackInfo ci) {
        CookSystem.onCooked(taker, stack);
    }
}
