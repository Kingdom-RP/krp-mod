package com.kingdomrp.core.network;

import com.kingdomrp.core.KingdomRPCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(KingdomRPCore.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.registerMessage(
                packetId++,
                SyncPlayerDataPacket.class,
                SyncPlayerDataPacket::encode,
                SyncPlayerDataPacket::decode,
                SyncPlayerDataPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                ChooseSpecializationPacket.class,
                ChooseSpecializationPacket::encode,
                ChooseSpecializationPacket::decode,
                ChooseSpecializationPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                XPGainPacket.class,
                XPGainPacket::encode,
                XPGainPacket::decode,
                XPGainPacket::handle
        );
    }
}