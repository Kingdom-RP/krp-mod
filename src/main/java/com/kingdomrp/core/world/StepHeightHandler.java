package com.kingdomrp.core.world;

import com.kingdomrp.core.KingdomRPCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Авто-шаг: постоянный бонус к высоте шага игрока (замена мода Accessible Step).
 * 0.6 (ваниль) + 0.65 = 1.25 — игрок автоматически заходит на блоки в 1 клетку.
 * При сникании бонус снимается: высокий step-height + shift сталкивает игрока
 * вниз по направлению движения на нижний блок.
 */
@EventBusSubscriber(modid = KingdomRPCore.MODID)
public final class StepHeightHandler {

    private static final ResourceLocation STEP_ID =
            ResourceLocation.fromNamespaceAndPath(KingdomRPCore.MODID, "auto_step");
    private static final double BONUS = 0.65;

    private StepHeightHandler() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        apply(event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        apply(event.getEntity());
    }

    /** Снимаем бонус на время сникания, возвращаем после. */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        AttributeInstance inst = player.getAttribute(Attributes.STEP_HEIGHT);
        if (inst == null) return;
        boolean has = inst.getModifier(STEP_ID) != null;
        if (player.isShiftKeyDown()) {
            if (has) inst.removeModifier(STEP_ID);
        } else if (!has) {
            inst.addPermanentModifier(new AttributeModifier(
                    STEP_ID, BONUS, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void apply(Player player) {
        AttributeInstance inst = player.getAttribute(Attributes.STEP_HEIGHT);
        if (inst == null) return;
        inst.removeModifier(STEP_ID); // idempotent — не стакать
        inst.addPermanentModifier(new AttributeModifier(
                STEP_ID, BONUS, AttributeModifier.Operation.ADD_VALUE));
    }
}
