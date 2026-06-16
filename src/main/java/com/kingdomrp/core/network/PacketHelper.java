package com.kingdomrp.core.network;

import com.kingdomrp.core.capability.PlayerDataProvider;
import com.kingdomrp.core.data.Path;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public class PacketHelper {

    // Уведомить клиента о начислении XP по пути — триггер полоски прогресса в HUD
    public static void sendXPBar(ServerPlayer player, Path path, float currentXP,
                                 float requiredXP, int level, boolean leveledUp) {
        NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new XPGainPacket(path.index, currentXP, requiredXP, level, leveledUp)
        );
    }

    // Отправить данные конкретному игроку (вызывается на сервере)
    public static void syncPlayer(ServerPlayer player) {
        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data ->
                NetworkHandler.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new SyncPlayerDataPacket(data.serializeNBT())
                )
        );
        // Любое изменение уровней — повод пересчитать атрибуты по специализациям
        // (сейчас — дальность блоков Плотника). Единая точка для всех путей смены.
        com.kingdomrp.core.system.SpecializationEffects.refreshBlockReach(player);
    }
}