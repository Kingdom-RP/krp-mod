package com.kingdomrp.core.mixin.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Пока открыт чат, ни один кейбинд не считается зажатым/кликнутым. Некоторые моды
 * (напр. Simple Voice Chat) опрашивают {@link KeyMapping#isDown()}/{@code
 * consumeClick()} напрямую и срабатывают на буквы, набираемые в чате. Форсим
 * false — при открытом чате геймплейные бинды в любом случае не должны действовать.
 */
@Mixin(KeyMapping.class)
public class KeyMappingMixin {

    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    private void krp$blockDown(CallbackInfoReturnable<Boolean> cir) {
        if (typingInChat()) cir.setReturnValue(false);
    }

    @Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
    private void krp$blockClick(CallbackInfoReturnable<Boolean> cir) {
        if (typingInChat()) cir.setReturnValue(false);
    }

    private static boolean typingInChat() {
        return Minecraft.getInstance().screen instanceof ChatScreen;
    }
}
