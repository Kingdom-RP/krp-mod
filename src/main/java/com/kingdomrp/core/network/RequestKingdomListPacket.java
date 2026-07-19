package com.kingdomrp.core.network;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.kingdom.KingdomSync;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Клиент → сервер: запрос актуального списка всех королевств (при открытии меню K). */
public record RequestKingdomListPacket() implements CustomPacketPayload {

    public static final Type<RequestKingdomListPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KingdomRPCore.MODID, "request_kingdom_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestKingdomListPacket> STREAM_CODEC =
            StreamCodec.unit(new RequestKingdomListPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(RequestKingdomListPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) KingdomSync.sendList(player);
    }
}
