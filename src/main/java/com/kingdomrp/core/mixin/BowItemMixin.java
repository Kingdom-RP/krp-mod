package com.kingdomrp.core.mixin;

import com.kingdomrp.core.capability.PlayerDataProvider;
import com.kingdomrp.core.data.Spec;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = BowItem.class, remap = false)
public class BowItemMixin {

    @org.spongepowered.asm.mixin.injection.Redirect(
            method = "releaseUsing",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/BowItem;getPowerForTime(I)F",
                    remap = false
            ),
            remap = false
    )
    private float redirectGetPowerForTime(int ticks, ItemStack stack,
                                          Level level, LivingEntity entity,
                                          int remainingTicks) {
        if (entity instanceof Player player) {
            int[] archerLevel = {0};
            player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                archerLevel[0] = data.getSpecializationLevel(Spec.ARCHER.id);
            });

            if (archerLevel[0] > 0) {
                // +3% скорости за уровень — увеличиваем ticks перед передачей
                float speedBonus = 1.0f + archerLevel[0] * 0.03f;
                speedBonus = 20;
                ticks = Math.min(20, (int)(ticks * speedBonus));
            }
        }
        return BowItem.getPowerForTime(ticks);
    }
}