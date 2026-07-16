package com.kingdomrp.core.compat.mixin;

import com.kingdomrp.core.compat.TieredMoreCompat;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Подмена ванильного {@code Player.getLuck()} в роллере тиров Tiered More на
 * {@link TieredMoreCompat#reforgeLuck} = вся удача игрока + бонус Мастерового. Цель
 * по СТРОКЕ, применяется только если tiered_more загружен ({@link
 * com.kingdomrp.core.compat.TieredMoreMixinPlugin}). Per-player, без правки конфига.
 */
@Mixin(targets = "draylar.tiered.api.ModifierUtils", remap = false)
public abstract class ReforgeLuckMixin {

    @Redirect(
            method = {
                    "applyDynamicWeightModifiers",
                    "getRandomAttributeIDFor(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/Item;ZLjava/lang/String;ZZ)Lnet/minecraft/resources/ResourceLocation;"
            },
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getLuck()F"),
            remap = false
    )
    private static float krp$reforgeLuck(Player player) {
        return TieredMoreCompat.reforgeLuck(player);
    }
}
