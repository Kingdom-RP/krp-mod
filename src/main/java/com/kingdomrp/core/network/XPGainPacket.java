package com.kingdomrp.core.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Сервер → клиент: уведомление о получении XP по пути. Триггерит появление
 * полоски прогресса в HUD (см. {@code client.XPHudOverlay}). Шлётся из
 * {@link com.kingdomrp.core.system.XPSystem#giveXP} при каждом начислении.
 */
public class XPGainPacket {

    private final int pathIndex;
    private final float currentXP;
    private final float requiredXP;
    private final int level;
    private final boolean leveledUp;

    public XPGainPacket(int pathIndex, float currentXP, float requiredXP, int level, boolean leveledUp) {
        this.pathIndex = pathIndex;
        this.currentXP = currentXP;
        this.requiredXP = requiredXP;
        this.level = level;
        this.leveledUp = leveledUp;
    }

    public static void encode(XPGainPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.pathIndex);
        buf.writeFloat(packet.currentXP);
        buf.writeFloat(packet.requiredXP);
        buf.writeVarInt(packet.level);
        buf.writeBoolean(packet.leveledUp);
    }

    public static XPGainPacket decode(FriendlyByteBuf buf) {
        return new XPGainPacket(buf.readVarInt(), buf.readFloat(), buf.readFloat(),
                buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(XPGainPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.kingdomrp.core.client.XPHudOverlay.onXPGain(
                                packet.pathIndex, packet.currentXP, packet.requiredXP,
                                packet.level, packet.leveledUp))
        );
        ctx.get().setPacketHandled(true);
    }
}
