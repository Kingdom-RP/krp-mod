package com.kingdomrp.core.network;

import com.kingdomrp.core.capability.PlayerData;
import com.kingdomrp.core.capability.PlayerDataProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncPlayerDataPacket {

    private final CompoundTag data;

    public SyncPlayerDataPacket(CompoundTag data) {
        this.data = data;
    }

    public static void encode(SyncPlayerDataPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.data);
    }

    public static SyncPlayerDataPacket decode(FriendlyByteBuf buf) {
        return new SyncPlayerDataPacket(buf.readNbt());
    }

    public static void handle(SyncPlayerDataPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    var player = Minecraft.getInstance().player;
                    if (player == null) return;
                    player.getCapability(PlayerDataProvider.PLAYER_DATA)
                            .ifPresent(data -> data.deserializeNBT(packet.data));
                })
        );
        ctx.get().setPacketHandled(true);
    }
}