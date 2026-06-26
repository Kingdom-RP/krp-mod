package com.kingdomrp.core.capability;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.network.PacketHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Синк данных игрока клиенту при входе. Прикрепление/копирование при смерти —
 * система Data Attachment ({@link com.kingdomrp.core.registry.KRPAttachments}); синк
 * на респаун/смену измерения — {@code SpecializationEffects}.
 */
@EventBusSubscriber(modid = KingdomRPCore.MODID)
public class PlayerDataEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PacketHelper.syncPlayer(player);
    }
}
