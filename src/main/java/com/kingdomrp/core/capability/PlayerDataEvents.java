package com.kingdomrp.core.capability;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.network.PacketHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KingdomRPCore.MODID)
public class PlayerDataEvents {

    // Прикрепляем capability к каждому игроку при создании
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof Player)) return;

        PlayerDataProvider provider = new PlayerDataProvider();
        event.addCapability(
                new ResourceLocation(KingdomRPCore.MODID, "player_data"),
                provider
        );
        event.addListener(provider::invalidate);
    }

    // Копируем данные при возрождении игрока (смерть не сбрасывает прогресс)
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getOriginal().reviveCaps();

            event.getOriginal().getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(oldData ->
                    event.getEntity().getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(newData -> {
                        newData.deserializeNBT(oldData.serializeNBT());
                    })
            );

            event.getOriginal().invalidateCaps();
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PacketHelper.syncPlayer(player);
    }
}