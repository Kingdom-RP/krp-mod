package com.kingdomrp.core.mixin;

import com.kingdomrp.core.config.KRPConfig;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Баланс: множитель расхода голода. Все действия в ваниле тратят голод через
 * {@code FoodData.addExhaustion(float)} (бег, прыжки, атака, копание, урон и т.п.) —
 * домножаем входной аргумент на {@code balance.exhaustionMultiplier} (по умолч. 1.5),
 * чтобы голод уходил быстрее и еда/Повар были важнее.
 */
@Mixin(value = FoodData.class, remap = false)
public class FoodDataMixin {

    @ModifyVariable(method = "addExhaustion", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private float krp$scaleExhaustion(float amount) {
        // Конфиг — SERVER; на клиенте до синка может быть не загружен → без изменений.
        if (!KRPConfig.SPEC.isLoaded()) return amount;
        return (float) (amount * KRPConfig.EXHAUSTION_MULTIPLIER.get());
    }
}
