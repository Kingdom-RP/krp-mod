package com.kingdomrp.core.mixin;

import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Доступ к package-private полю {@code brewTime} зельеварки — нужен Алхимику,
 * чтобы отличать «варка не началась» (brewTime==0, точка гейта/захвата owner-а)
 * от «варка идёт» (brewTime&gt;0, не пере-гейтим и не перезаписываем owner-а).
 */
@Mixin(value = BrewingStandBlockEntity.class, remap = false)
public interface BrewingStandAccessor {
    @Accessor("brewTime")
    int krp$getBrewTime();
}
