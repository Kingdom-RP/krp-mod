package com.kingdomrp.core.mixin;

import com.kingdomrp.core.system.MagicSystem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BrewingStandBlockEntity.class, remap = false)
public class BrewingStandMixin {

    private static final org.slf4j.Logger LOGGER =
            com.mojang.logging.LogUtils.getLogger();

    static {
        LOGGER.info("[KRP Mixin] BrewingStandMixin loaded!");
    }

    @Inject(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/BrewingStandBlockEntity;doBrew(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/NonNullList;)V",
                    shift = At.Shift.AFTER
            ),
            remap = false
    )
    private static void onBrewTick(Level level, BlockPos pos, BlockState state,
                                   BrewingStandBlockEntity stand, CallbackInfo ci) {
        MagicSystem.onBrewComplete(level, pos, stand);
    }

    /**
     * Гейт Алхимика «не запускать»: перехватываем результат ванильного
     * {@code isBrewable(items)} в {@code serverTick}. Если зелье недоступно по
     * уровню владельца — возвращаем false, и варка не стартует (и гасится, если
     * как-то идёт), без траты blaze powder. Также здесь фиксируется owner и
     * снимок ингредиента (см. {@link MagicSystem#canBrewGate}).
     */
    @Redirect(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/BrewingStandBlockEntity;isBrewable(Lnet/minecraft/core/NonNullList;)Z"
            ),
            remap = false
    )
    private static boolean krp$gateBrewable(NonNullList<ItemStack> items,
                                            Level level, BlockPos pos, BlockState state,
                                            BrewingStandBlockEntity stand) {
        if (!MagicSystem.isVanillaBrewable(items)) return false;
        int brewTime = ((BrewingStandAccessor) stand).krp$getBrewTime();
        return MagicSystem.canBrewGate(level, pos, items, brewTime);
    }
}