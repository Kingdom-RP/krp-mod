package com.kingdomrp.core.network;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.registry.KRPAttachments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Сервер → клиент: полные данные игрока (синк после изменения). */
public record SyncPlayerDataPacket(CompoundTag data) implements CustomPacketPayload {

    public static final Type<SyncPlayerDataPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KingdomRPCore.MODID, "sync_player_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPlayerDataPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.COMPOUND_TAG, SyncPlayerDataPacket::data,
                    SyncPlayerDataPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncPlayerDataPacket packet, IPayloadContext context) {
        Player player = context.player(); // клиентский игрок (playToClient)
        player.getData(KRPAttachments.PLAYER_DATA).deserializeNBT(player.registryAccess(), packet.data());
    }
}
