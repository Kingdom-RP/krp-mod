package com.kingdomrp.core.network;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.kingdom.Kingdom;
import com.kingdomrp.core.kingdom.KingdomData;
import com.kingdomrp.core.kingdom.KingdomManager;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/** Клиент → сервер: король исключает жителя по UUID. */
public record KickMemberPacket(UUID target) implements CustomPacketPayload {

    public static final Type<KickMemberPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KingdomRPCore.MODID, "kick_member"));

    public static final StreamCodec<RegistryFriendlyByteBuf, KickMemberPacket> STREAM_CODEC =
            StreamCodec.composite(UUIDUtil.STREAM_CODEC, KickMemberPacket::target, KickMemberPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(KickMemberPacket packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer king)) return;
        Kingdom k = KingdomData.get(king.server).byPlayer(king.getUUID());
        if (k == null || !k.isKing(king.getUUID())) return;
        if (packet.target().equals(king.getUUID()) || !k.isMember(packet.target())) return;

        String name = name(king.server, packet.target());
        KingdomManager.removeMember(king.server, k, packet.target(), name);
    }

    private static String name(net.minecraft.server.MinecraftServer server, UUID uuid) {
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        if (online != null) return online.getGameProfile().getName();
        return server.getProfileCache() != null
                ? server.getProfileCache().get(uuid).map(p -> p.getName()).orElse(uuid.toString())
                : uuid.toString();
    }
}
