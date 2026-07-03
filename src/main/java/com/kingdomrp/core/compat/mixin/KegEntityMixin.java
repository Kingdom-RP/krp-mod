package com.kingdomrp.core.compat.mixin;

import com.kingdomrp.core.compat.AlcoCraftCompat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * Гейт и XP Повара за варку пива в Кеге AlcoCraft+. Цель — по СТРОКЕ (без зависимости
 * от мода), применяется только если мод загружен ({@link
 * com.kingdomrp.core.compat.AlcoCraftMixinPlugin}).
 * <p>
 * В {@code tick} Кег продвигает {@code progress} пока {@code canBrewRecipe(keg)} present,
 * а по заполнению зовёт {@code brew(...)} (там же списывает ингредиенты). Перехватываем:
 * <ul>
 *   <li>{@code canBrewRecipe} — гейт: если сорт результата выше уровня Повара, возвращаем
 *       {@code Optional.empty()} → прогресс не растёт, ресурсы целы (списываются лишь в
 *       {@code brew}). Заодно снимаем сорт для XP.</li>
 *   <li>вызов {@code brew} — завершение варки: разовое начисление XP владельцу.</li>
 * </ul>
 * Owner — из {@link AlcoCraftCompat} по позиции. Результат рецепта берём через ванильный
 * {@link RecipeHolder} (типы мода не нужны).
 */
@Mixin(targets = "me.itzme1on.alcocraftplus.core.blocks.keg.KegEntity", remap = false)
public abstract class KegEntityMixin {

    @ModifyExpressionValue(
            method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lme/itzme1on/alcocraftplus/core/blocks/keg/KegEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/itzme1on/alcocraftplus/core/blocks/keg/KegEntity;canBrewRecipe(Lme/itzme1on/alcocraftplus/core/blocks/keg/KegEntity;)Ljava/util/Optional;",
                    remap = false
            ),
            remap = false
    )
    private Optional<?> krp$gateBrew(Optional<?> original) {
        if (original.isEmpty()) return original;
        BlockEntity be = (BlockEntity) (Object) this;
        ItemStack result = ((RecipeHolder<?>) original.get())
                .value().getResultItem(be.getLevel().registryAccess());
        return AlcoCraftCompat.gateBrew(be.getLevel(), be.getBlockPos(), result.getItem())
                ? original : Optional.empty();
    }

    @Inject(
            method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lme/itzme1on/alcocraftplus/core/blocks/keg/KegEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/itzme1on/alcocraftplus/core/blocks/keg/KegEntity;brew(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lme/itzme1on/alcocraftplus/core/blocks/keg/KegEntity;Lnet/minecraft/world/item/crafting/RecipeHolder;)V",
                    remap = false
            ),
            remap = false
    )
    private void krp$brewXP(CallbackInfo ci) {
        BlockEntity be = (BlockEntity) (Object) this;
        AlcoCraftCompat.onBrewComplete(be.getLevel(), be.getBlockPos());
    }
}
