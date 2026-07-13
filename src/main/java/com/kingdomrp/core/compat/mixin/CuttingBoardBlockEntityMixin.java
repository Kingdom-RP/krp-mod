package com.kingdomrp.core.compat.mixin;

import com.kingdomrp.core.data.type.Path;
import com.kingdomrp.core.system.XPSystem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * XP Повару (path Промысел) за нарезку на разделочной доске Farmer's Delight.
 * Цель по СТРОКЕ (без хард-зависимости), применяется только если FD загружен —
 * см. {@link com.kingdomrp.core.compat.FDMixinPlugin}. {@code
 * processStoredItemUsingTool(ItemStack, Player)} возвращает true при успешной
 * обработке — тогда даём 2 XP.
 */
@Mixin(targets = "vectorwing.farmersdelight.common.block.entity.CuttingBoardBlockEntity", remap = false)
public class CuttingBoardBlockEntityMixin {

    @Inject(method = "processStoredItemUsingTool", at = @At("RETURN"), remap = false)
    private void krp$cutXp(ItemStack tool, Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        if (player == null || player.level().isClientSide()) return;
        XPSystem.giveXP(player, Path.HARVEST, 2f);
    }
}
