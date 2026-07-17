package com.kingdomrp.core.network;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.kingdom.Kingdom;
import com.kingdomrp.core.kingdom.KingdomData;
import com.kingdomrp.core.kingdom.KingdomSync;
import com.kingdomrp.core.kingdom.ftb.FtbBridge;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Клиент → сервер: смена цвета королевства (только король). */
public record SetKingdomColorPacket(int color) implements CustomPacketPayload {

    public static final Type<SetKingdomColorPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KingdomRPCore.MODID, "set_kingdom_color"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetKingdomColorPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.INT, SetKingdomColorPacket::color, SetKingdomColorPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SetKingdomColorPacket packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        KingdomData data = KingdomData.get(player.server);
        Kingdom k = data.byPlayer(player.getUUID());
        if (k == null || !k.isKing(player.getUUID())) return;

        k.setColor(packet.color());
        data.markDirty();
        FtbBridge.applyColor(player.server, k);
        KingdomSync.broadcast(player.server, k);

        // Пересинк блока → перекрасить луч-маяк на клиентах.
        var level = player.server.getLevel(k.getDimension());
        if (level != null && level.isLoaded(k.getBlockPos())
                && level.getBlockEntity(k.getBlockPos())
                    instanceof com.kingdomrp.core.kingdom.block.KingdomBlockEntity be) {
            be.syncToClient();
        }
    }
}
