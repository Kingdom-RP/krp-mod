package com.kingdomrp.core.network;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.kingdom.CharterData;
import com.kingdomrp.core.kingdom.KingdomReqs;
import com.kingdomrp.core.kingdom.item.CharterItem;
import com.kingdomrp.core.registry.KRPAttachments;
import com.kingdomrp.core.registry.KRPItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Клиент → сервер: подписание хартии будущим королём (выбор названия).
 * Проверяет требования {@link KingdomReqs}; при успехе пишет {@link CharterData}
 * в хартию в руке, король = подписавший.
 */
public record SignCharterPacket(String name) implements CustomPacketPayload {

    public static final Type<SignCharterPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KingdomRPCore.MODID, "sign_charter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SignCharterPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SignCharterPacket::name, SignCharterPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SignCharterPacket packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;

        String name = packet.name().trim();
        if (name.isEmpty() || name.length() > 32) {
            msg(player, "kingdomrp.charter.bad_name", ChatFormatting.RED);
            return;
        }

        ItemStack held = findUnsignedCharter(player);
        if (held.isEmpty()) return;

        if (com.kingdomrp.core.kingdom.KingdomData.get(player.server).byPlayer(player.getUUID()) != null) {
            msg(player, "kingdomrp.charter.in_kingdom", ChatFormatting.RED);
            return;
        }

        KingdomReqs.Result r = KingdomReqs.check(player.getData(KRPAttachments.PLAYER_DATA));
        if (!r.ok()) {
            msg(player, r.failKey(), ChatFormatting.RED);
            return;
        }

        CharterItem.set(held, CharterData.sign(name, player.getUUID()));
        msg(player, "kingdomrp.charter.signed", ChatFormatting.GREEN);
    }

    private static ItemStack findUnsignedCharter(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack s = player.getItemInHand(hand);
            if (s.getItem() == KRPItems.CHARTER.get() && !CharterItem.isSigned(s)) return s;
        }
        return ItemStack.EMPTY;
    }

    private static void msg(ServerPlayer player, String key, ChatFormatting color) {
        player.displayClientMessage(Component.translatable(key).withStyle(color), true);
    }
}
