package com.kingdomrp.core.compat.mixin;

import com.kingdomrp.core.data.Spec;
import com.kingdomrp.core.registry.KRPAttachments;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Ускоренный клёв Рыбака под мод Tide. Цель — по СТРОКЕ (без зависимости от Tide),
 * применяется только если Tide загружен — см. {@link com.kingdomrp.core.compat.TideMixinPlugin}.
 * <p>
 * В {@code catchingFish} Tide один раз вычитает из {@code timeUntilLured} логистику
 * от {@code lureSpeed} ({@code 2000/(1+e^{-0.2·lureSpeed}) − 1000}). Перехватываем
 * ЧТЕНИЕ поля {@code lureSpeed} и с 5 уровня Рыбака добавляем +1 (как одна ступень
 * Lure). Ниже 5 уровня — бонуса нет. Стакается с удочкой/наживкой/чарой Lure.
 */
@Mixin(targets = "com.li64.tide.registries.entities.misc.fishing.TideFishingHook", remap = false)
public abstract class TideFishingHookMixin {

    @ModifyExpressionValue(
            method = "catchingFish",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/li64/tide/registries/entities/misc/fishing/TideFishingHook;lureSpeed:I",
                    opcode = Opcodes.GETFIELD,
                    remap = false
            ),
            remap = false
    )
    private int krp$lureSpeed(int lureSpeed) {
        Entity owner = ((Projectile) (Object) this).getOwner();
        if (!(owner instanceof Player player)) return lureSpeed;
        int level = player.getData(KRPAttachments.PLAYER_DATA).getSpecializationLevel(Spec.FISHERMAN.id);
        return level >= 5 ? lureSpeed + 1 : lureSpeed;
    }
}
