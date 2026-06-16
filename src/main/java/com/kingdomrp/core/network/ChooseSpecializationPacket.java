package com.kingdomrp.core.network;

import com.kingdomrp.core.capability.PlayerDataProvider;
import com.kingdomrp.core.specialization.SpecializationRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ChooseSpecializationPacket {

    private final String specId;

    public ChooseSpecializationPacket(String specId) {
        this.specId = specId;
    }

    public static void encode(ChooseSpecializationPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.specId);
    }

    public static ChooseSpecializationPacket decode(FriendlyByteBuf buf) {
        return new ChooseSpecializationPacket(buf.readUtf());
    }

    public static void handle(ChooseSpecializationPacket packet,
                              Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            var specOpt = SpecializationRegistry.get(packet.specId);
            if (specOpt.isEmpty()) return;

            var spec = specOpt.get();

            player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                if (!data.canAffordSpecialization(spec.getPath(), packet.specId)) return;

                data.levelUpSpecialization(packet.specId);
                PacketHelper.syncPlayer(player);

                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§6[Kingdom RP] §eСпециализация «" + spec.getName()
                                + "» улучшена до уровня "
                                + data.getSpecializationLevel(packet.specId) + "!"
                ));
            });
        });
        ctx.get().setPacketHandled(true);
    }
}