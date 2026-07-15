package com.kingdomrp.core.network;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.kingdom.Kingdom;
import com.kingdomrp.core.kingdom.KingdomData;
import com.kingdomrp.core.kingdom.KingdomInvites;
import com.kingdomrp.core.kingdom.KingdomSync;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Клиент → сервер: король приглашает игрока по нику (только онлайн). */
public record InvitePlayerPacket(String name) implements CustomPacketPayload {

    public static final Type<InvitePlayerPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KingdomRPCore.MODID, "invite_player"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InvitePlayerPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, InvitePlayerPacket::name, InvitePlayerPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(InvitePlayerPacket packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer king)) return;
        KingdomData data = KingdomData.get(king.server);
        Kingdom k = data.byPlayer(king.getUUID());
        if (k == null || !k.isKing(king.getUUID())) return;

        ServerPlayer target = king.server.getPlayerList().getPlayerByName(packet.name().trim());
        if (target == null) { msg(king, "kingdomrp.invite.not_online", ChatFormatting.RED); return; }
        if (target.getUUID().equals(king.getUUID()) || k.isMember(target.getUUID())) {
            msg(king, "kingdomrp.invite.already_member", ChatFormatting.RED); return;
        }
        if (data.byPlayer(target.getUUID()) != null) {
            msg(king, "kingdomrp.invite.target_in_kingdom", ChatFormatting.RED); return;
        }
        if (KingdomInvites.has(target.getUUID())) {
            msg(king, "kingdomrp.invite.already_invited", ChatFormatting.RED); return;
        }

        KingdomInvites.put(target.getUUID(), k.getId());
        KingdomInvites.sendInviteMessage(target, king.getGameProfile().getName(), k.getName(), k.getColor());
        msg(king, "kingdomrp.invite.sent", ChatFormatting.GREEN);
        KingdomSync.broadcast(king.server, k);   // обновить список приглашений в UI
    }

    private static void msg(ServerPlayer p, String key, ChatFormatting color) {
        p.displayClientMessage(Component.translatable(key).withStyle(color), false);
    }
}
