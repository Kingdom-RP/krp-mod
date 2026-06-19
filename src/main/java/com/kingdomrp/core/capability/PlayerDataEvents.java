package com.kingdomrp.core.capability;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.network.PacketHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * События жизненного цикла данных игрока. Само хранилище — Data Attachment
 * (см. {@link com.kingdomrp.core.registry.KRPAttachments}): прикрепление и
 * копирование при смерти ({@code copyOnDeath}) делает система аттачментов, ручные
 * обработчики больше не нужны. Здесь — только синк клиенту при входе. Синк на
 * респаун/смену измерения делает {@code SpecializationEffects} (через
 * {@code PacketHelper.syncPlayer}).
 */
@EventBusSubscriber(modid = KingdomRPCore.MODID)
public class PlayerDataEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PacketHelper.syncPlayer(player);
    }
}
