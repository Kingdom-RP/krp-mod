package com.kingdomrp.core.capability;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.network.PacketHelper;
import com.kingdomrp.core.registry.KRPAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
        giveStarterKit(player);
        PacketHelper.syncPlayer(player);
    }

    /** Стартовый набор: 5 хлеба при первом входе на сервер (однократно). */
    private static void giveStarterKit(ServerPlayer player) {
        var data = player.getData(KRPAttachments.PLAYER_DATA);
        if (data.hasReceivedStarterKit()) return;
        player.getInventory().add(new ItemStack(Items.BREAD, 5));
        data.markStarterKitReceived();
    }
}
