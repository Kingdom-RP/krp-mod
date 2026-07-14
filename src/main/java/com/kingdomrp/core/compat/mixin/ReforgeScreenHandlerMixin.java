package com.kingdomrp.core.compat.mixin;

import com.kingdomrp.core.registry.KRPAttachments;
import com.kingdomrp.core.data.type.Spec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Гейт рефоржа Tiered More по уровню Мастерового. Цель по СТРОКЕ (без хард-зависимости),
 * применяется только если tiered_more загружен — см. {@link com.kingdomrp.core.compat.TieredMoreMixinPlugin}.
 * <p>
 * Отменяем {@code ReforgeScreenHandler.reforge()} (действие кнопки «Улучшить») при
 * уровне Мастерового ниже {@value #MIN_LEVEL} — предмет/материал/аметист не тратятся,
 * тир не меняется. Смещение шанса по уровню — отдельно через Luck ({@link
 * com.kingdomrp.core.compat.TieredMoreCompat}).
 */
@Mixin(targets = "draylar.tiered.reforge.ReforgeScreenHandler", remap = false)
public abstract class ReforgeScreenHandlerMixin {

    private static final int MIN_LEVEL = 5;

    @Shadow @Final private Player player;

    @Inject(method = "reforge", at = @At("HEAD"), cancellable = true, remap = false)
    private void krp$gateReforge(CallbackInfo ci) {
        if (player == null || player.level().isClientSide()) return;
        int level = player.getData(KRPAttachments.PLAYER_DATA).getSpecializationLevel(Spec.CRAFTSMAN.id);
        if (level < MIN_LEVEL) {
            player.sendSystemMessage(Component.literal(
                    "§c[Kingdom RP] Улучшение предметов доступно с " + MIN_LEVEL
                            + " уровня навыка «Мастеровой»."));
            ci.cancel();
        }
    }
}
