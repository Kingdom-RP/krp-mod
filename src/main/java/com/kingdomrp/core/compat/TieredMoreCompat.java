package com.kingdomrp.core.compat;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.registry.KRPAttachments;
import com.kingdomrp.core.data.type.Spec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;

/**
 * Интеграция с модом Tiered More (рефорж тиров). Смещение шанса на лучший тир по
 * уровню Мастерового реализовано через ВАНИЛЬНЫЙ Luck: ролл тира в Tiered More
 * читает {@code player.getLuck()} × {@code luckReforgeModifier}. Пока открыта
 * наковальня — вешаем временный Luck = уровень Мастерового × {@link #LUCK_PER_LEVEL},
 * на закрытие снимаем. Чем выше Мастеровой — тем лучше тиры.
 * <p>
 * ⚠️ В конфиге Tiered More должен быть {@code luckReforgeModifier > 0}, иначе Luck
 * на рефорж не влияет.
 */
@EventBusSubscriber(modid = KingdomRPCore.MODID)
public final class TieredMoreCompat {

    public static final String MODID = "tiered_more";
    private static final float LUCK_PER_LEVEL = 1.0f;

    private static final ResourceLocation LUCK_ID =
            ResourceLocation.fromNamespaceAndPath(KingdomRPCore.MODID, "reforge_luck");

    private TieredMoreCompat() {}

    @SubscribeEvent
    public static void onOpen(PlayerContainerEvent.Open event) {
        if (!ModList.get().isLoaded(MODID)) return;
        if (!(event.getContainer() instanceof AnvilMenu)) return;
        applyLuck(event.getEntity());
    }

    @SubscribeEvent
    public static void onClose(PlayerContainerEvent.Close event) {
        if (!ModList.get().isLoaded(MODID)) return;
        if (!(event.getContainer() instanceof AnvilMenu)) return;
        removeLuck(event.getEntity());
    }

    private static void applyLuck(Player player) {
        AttributeInstance luck = player.getAttribute(Attributes.LUCK);
        if (luck == null) return;
        luck.removeModifier(LUCK_ID); // idempotent
        int level = player.getData(KRPAttachments.PLAYER_DATA).getSpecializationLevel(Spec.CRAFTSMAN.id);
        if (level <= 0) return;
        luck.addTransientModifier(new AttributeModifier(
                LUCK_ID, level * LUCK_PER_LEVEL, AttributeModifier.Operation.ADD_VALUE));
    }

    private static void removeLuck(Player player) {
        AttributeInstance luck = player.getAttribute(Attributes.LUCK);
        if (luck != null) luck.removeModifier(LUCK_ID);
    }
}
