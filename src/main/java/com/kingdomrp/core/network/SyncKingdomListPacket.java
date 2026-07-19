package com.kingdomrp.core.network;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.client.ClientKingdomList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/** Сервер → клиент: список всех королевств (название, король, чанки) для вкладки «Королевства». */
public record SyncKingdomListPacket(List<Entry> entries) implements CustomPacketPayload {

    /** Строка списка: название королевства, ник короля, число заклеймленных чанков. */
    public record Entry(String name, String king, int chunks) {}

    public static final Type<SyncKingdomListPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KingdomRPCore.MODID, "sync_kingdom_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncKingdomListPacket> STREAM_CODEC =
            StreamCodec.of((buf, v) -> {
                buf.writeVarInt(v.entries().size());
                for (Entry e : v.entries()) {
                    buf.writeUtf(e.name());
                    buf.writeUtf(e.king());
                    buf.writeVarInt(e.chunks());
                }
            }, buf -> {
                int n = buf.readVarInt();
                List<Entry> list = new ArrayList<>(n);
                for (int i = 0; i < n; i++) list.add(new Entry(buf.readUtf(), buf.readUtf(), buf.readVarInt()));
                return new SyncKingdomListPacket(list);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SyncKingdomListPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientKingdomList.set(packet.entries());
            com.kingdomrp.core.client.KingdomClientHooks.onKingdomSync();  // авто-рефреш экрана
        });
    }
}
