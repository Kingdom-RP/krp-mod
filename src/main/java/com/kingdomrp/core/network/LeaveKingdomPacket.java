package com.kingdomrp.core.network;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.kingdom.Kingdom;
import com.kingdomrp.core.kingdom.KingdomData;
import com.kingdomrp.core.kingdom.KingdomManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Клиент → сервер: житель добровольно покидает своё королевство (король не может). */
public record LeaveKingdomPacket() implements CustomPacketPayload {

    public static final Type<LeaveKingdomPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KingdomRPCore.MODID, "leave_kingdom"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LeaveKingdomPacket> STREAM_CODEC =
            StreamCodec.unit(new LeaveKingdomPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(LeaveKingdomPacket packet,
                              net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        Kingdom k = KingdomData.get(player.server).byPlayer(player.getUUID());
        if (k == null || k.isKing(player.getUUID())) return;   // король выходит только через роспуск
        KingdomManager.removeMember(player.server, k, player.getUUID(),
                player.getGameProfile().getName());
    }
}
