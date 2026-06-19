package com.kingdomrp.core.network;

import com.kingdomrp.core.capability.PlayerData;
import com.kingdomrp.core.data.Path;
import com.kingdomrp.core.registry.KRPAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class PacketHelper {

    /** Отправить полные данные игроку (вызывается на сервере). */
    public static void syncPlayer(ServerPlayer player) {
        PlayerData data = player.getData(KRPAttachments.PLAYER_DATA);
        PacketDistributor.sendToPlayer(player,
                new SyncPlayerDataPacket(data.serializeNBT(player.registryAccess())));
        // Любое изменение уровней — повод пересчитать атрибуты по специализациям
        // (дальность блоков Плотника). Единая точка для всех путей смены.
        com.kingdomrp.core.system.SpecializationEffects.refreshBlockReach(player);
    }

    /** Уведомить клиента о начислении XP по пути — триггер полоски прогресса в HUD. */
    public static void sendXPBar(ServerPlayer player, Path path, float currentXP,
                                 float requiredXP, int level, boolean leveledUp) {
        PacketDistributor.sendToPlayer(player,
                new XPGainPacket(path.index, currentXP, requiredXP, level, leveledUp));
    }
}
