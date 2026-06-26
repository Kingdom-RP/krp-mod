package com.kingdomrp.core.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Заголовок окна игры — «Kingdom RP». Инжект на {@code RETURN}
 * {@code Minecraft.createTitle()} покрывает все обновления заголовка.
 */
@Mixin(Minecraft.class)
public class MinecraftTitleMixin {

    @Inject(method = "createTitle", at = @At("RETURN"), cancellable = true, remap = false)
    private void krp$windowTitle(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("Kingdom RP");
    }
}
