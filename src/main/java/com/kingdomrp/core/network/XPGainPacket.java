package com.kingdomrp.core.network;

import com.kingdomrp.core.KingdomRPCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Сервер → клиент: уведомление о получении XP по пути. Триггерит полоску
 * прогресса в HUD ({@code client.XPHudOverlay}).
 */
public record XPGainPacket(int pathIndex, float currentXP, float requiredXP, int level, boolean leveledUp)
        implements CustomPacketPayload {

    public static final Type<XPGainPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KingdomRPCore.MODID, "xp_gain"));

    public static final StreamCodec<RegistryFriendlyByteBuf, XPGainPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, XPGainPacket::pathIndex,
                    ByteBufCodecs.FLOAT, XPGainPacket::currentXP,
                    ByteBufCodecs.FLOAT, XPGainPacket::requiredXP,
                    ByteBufCodecs.VAR_INT, XPGainPacket::level,
                    ByteBufCodecs.BOOL, XPGainPacket::leveledUp,
                    XPGainPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(XPGainPacket packet, IPayloadContext context) {
        // Только клиент (playToClient) — обращение к клиентскому оверлею безопасно.
        com.kingdomrp.core.client.XPHudOverlay.onXPGain(
                packet.pathIndex(), packet.currentXP(), packet.requiredXP(),
                packet.level(), packet.leveledUp());
    }
}
