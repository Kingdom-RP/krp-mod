package com.kingdomrp.core.kingdom.ftb.mixin;

import com.kingdomrp.core.kingdom.ftb.FtbBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Обход лимита клейма FTB Chunks для НАШИХ клеймов. Проверка {@code claim()} в
 * {@code ChunkTeamDataImpl} сравнивает {@code getClaimedChunks().size()} с
 * {@code getMaxClaimChunks()} и НЕ учитывает {@code extraClaimChunks} — из-за
 * чего расширение королевства упиралось в {@code max_claimed_chunks}. Пока активен
 * {@link FtbBridge#isClaimBypass()} (только внутри наших claim-вызовов), возвращаем
 * {@code Integer.MAX_VALUE}: KingdomData — авторитет, лимит FTB на нас не действует.
 * Цель по СТРОКЕ, применяется только если ftbchunks загружен.
 */
@Mixin(targets = "dev.ftb.mods.ftbchunks.data.ChunkTeamDataImpl", remap = false)
public abstract class ChunkTeamClaimLimitMixin {

    @Inject(method = "getMaxClaimChunks", at = @At("HEAD"), cancellable = true, remap = false)
    private void krp$unlimitedForKingdomClaim(CallbackInfoReturnable<Integer> cir) {
        if (FtbBridge.isClaimBypass()) cir.setReturnValue(Integer.MAX_VALUE);
    }
}
