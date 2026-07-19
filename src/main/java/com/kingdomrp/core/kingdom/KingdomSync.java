package com.kingdomrp.core.kingdom;

import com.kingdomrp.core.network.SyncKingdomInfoPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/** Отправка клиентам сводки о королевстве ({@link SyncKingdomInfoPacket}). */
public final class KingdomSync {

    private KingdomSync() {}

    /** Отправить игроку актуальную сводку (его королевство либо «нет королевства»). */
    public static void send(ServerPlayer player) {
        Kingdom k = KingdomData.get(player.server).byPlayer(player.getUUID());
        PacketDistributor.sendToPlayer(player, build(player.server, k, player.getUUID()));
    }

    /** Разослать сводку всем онлайн-участникам королевства (isKing — индивидуально). */
    public static void broadcast(MinecraftServer server, Kingdom k) {
        for (UUID member : k.getMembers()) {
            ServerPlayer p = server.getPlayerList().getPlayer(member);
            if (p != null) PacketDistributor.sendToPlayer(p, build(server, k, member));
        }
    }

    private static SyncKingdomInfoPacket build(MinecraftServer server, @Nullable Kingdom k, UUID viewer) {
        if (k == null) return SyncKingdomInfoPacket.NONE;
        List<UUID> ids = new java.util.ArrayList<>(k.getMembers());
        List<String> members = ids.stream().map(u -> name(server, u)).toList();
        List<String> invites = KingdomInvites.forKingdom(k.getId()).stream()
                .map(u -> name(server, u)).toList();
        return new SyncKingdomInfoPacket(true, k.getName(), name(server, k.getKing()),
                members, ids, k.getClaims().size(), k.getColor(), k.isKing(viewer), invites,
                k.getCharacteristic(com.kingdomrp.core.kingdom.upkeep.Characteristic.FOOD),
                k.getCharacteristic(com.kingdomrp.core.kingdom.upkeep.Characteristic.MATERIALS),
                k.getCharacteristic(com.kingdomrp.core.kingdom.upkeep.Characteristic.PROSPERITY),
                com.kingdomrp.core.kingdom.upkeep.KingdomUpkeep.drain(k, com.kingdomrp.core.kingdom.upkeep.Characteristic.FOOD),
                com.kingdomrp.core.kingdom.upkeep.KingdomUpkeep.drain(k, com.kingdomrp.core.kingdom.upkeep.Characteristic.MATERIALS),
                com.kingdomrp.core.kingdom.upkeep.KingdomUpkeep.drain(k, com.kingdomrp.core.kingdom.upkeep.Characteristic.PROSPERITY));
    }

    private static String name(MinecraftServer server, UUID uuid) {
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        if (online != null) return online.getGameProfile().getName();
        return server.getProfileCache() != null
                ? server.getProfileCache().get(uuid).map(p -> p.getName()).orElse(uuid.toString().substring(0, 8))
                : uuid.toString().substring(0, 8);
    }
}
