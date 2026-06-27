package com.kingdomrp.core.mixin.client;

import com.kingdomrp.core.registry.KRPEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.ArrayList;
import java.util.List;

/**
 * Добавляет строку-описание в тултип нашего эффекта при наведении на его иконку
 * в инвентаре. Ванильный тултип эффекта собирается в {@code renderEffects} через
 * неизменяемый {@code List.of(name, duration)} и отдаётся в
 * {@code GuiGraphics.renderTooltip} — перехватываем аргумент-список и, если это
 * наш эффект (по имени), возвращаем копию с добавленным серым описанием.
 */
@Mixin(value = EffectRenderingInventoryScreen.class, remap = false)
public class EffectTooltipMixin {

    @ModifyArg(
            method = "renderEffects",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;II)V"),
            index = 1
    )
    private List<Component> krp$appendEffectDescription(List<Component> lines) {
        if (lines.isEmpty()) return lines;

        String wanted = KRPEffects.DEATH_XP_PENALTY.value().getDisplayName().getString();
        if (!lines.getFirst().getString().startsWith(wanted)) return lines;

        List<Component> copy = new ArrayList<>(lines);
        copy.add(Component.translatable("effect.kingdomrpcore.death_xp_penalty.desc")
                .withStyle(ChatFormatting.GRAY));
        return copy;
    }
}
