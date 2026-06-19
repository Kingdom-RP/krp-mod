package com.kingdomrp.core.network;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.registry.KRPAttachments;
import com.kingdomrp.core.specialization.SpecializationRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Клиент → сервер: выбор/улучшение специализации. */
public record ChooseSpecializationPacket(String specId) implements CustomPacketPayload {

    public static final Type<ChooseSpecializationPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KingdomRPCore.MODID, "choose_specialization"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChooseSpecializationPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, ChooseSpecializationPacket::specId,
                    ChooseSpecializationPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChooseSpecializationPacket packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;

        var specOpt = SpecializationRegistry.get(packet.specId());
        if (specOpt.isEmpty()) return;
        var spec = specOpt.get();

        var data = player.getData(KRPAttachments.PLAYER_DATA);
        if (!data.canAffordSpecialization(spec.getPath(), packet.specId())) return;

        data.levelUpSpecialization(packet.specId());
        PacketHelper.syncPlayer(player);

        player.sendSystemMessage(Component.literal(
                "§6[Kingdom RP] §eСпециализация «" + spec.getName()
                        + "» улучшена до уровня "
                        + data.getSpecializationLevel(packet.specId()) + "!"
        ));
    }
}
