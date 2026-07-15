package com.kingdomrp.core.mixin;

import com.kingdomrp.core.config.KRPConfig;
import com.kingdomrp.core.kingdom.upkeep.KingdomExhaustHolder;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Баланс: множитель расхода голода. Все действия в ваниле тратят голод через
 * {@code FoodData.addExhaustion(float)} — домножаем на {@code balance.exhaustionMultiplier}
 * (базовый, 1.5) И на множитель баффа еды королевства (складывается, не переопределяет).
 */
@Mixin(value = FoodData.class, remap = false)
public class FoodDataMixin implements KingdomExhaustHolder {

    @Unique private float krp$kingdomExhaust = 1f;

    @Override public void krp$setKingdomExhaust(float factor) { this.krp$kingdomExhaust = factor; }
    @Override public float krp$getKingdomExhaust() { return this.krp$kingdomExhaust; }

    @ModifyVariable(method = "addExhaustion", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private float krp$scaleExhaustion(float amount) {
        if (!KRPConfig.SPEC.isLoaded()) return amount;
        return (float) (amount * KRPConfig.EXHAUSTION_MULTIPLIER.get() * krp$kingdomExhaust);
    }
}
