package com.kingdomrp.core.network;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.client.ClientKingdomData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Сервер → клиент: сводка о королевстве игрока для меню/управления. */
public record SyncKingdomInfoPacket(boolean inKingdom, String name, String kingName,
                                    List<String> members, List<UUID> memberIds, int claims,
                                    int color, boolean isKing, List<String> pendingInvites,
                                    float food, float materials, float prosperity,
                                    float foodDrain, float materialsDrain, float prosperityDrain) implements CustomPacketPayload {

    public static final Type<SyncKingdomInfoPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KingdomRPCore.MODID, "sync_kingdom_info"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncKingdomInfoPacket> STREAM_CODEC =
            StreamCodec.of((buf, v) -> {
                buf.writeBoolean(v.inKingdom());
                buf.writeUtf(v.name());
                buf.writeUtf(v.kingName());
                buf.writeVarInt(v.members().size());
                for (String m : v.members()) buf.writeUtf(m);
                buf.writeVarInt(v.memberIds().size());
                for (UUID u : v.memberIds()) buf.writeUUID(u);
                buf.writeVarInt(v.claims());
                buf.writeInt(v.color());
                buf.writeBoolean(v.isKing());
                buf.writeVarInt(v.pendingInvites().size());
                for (String p : v.pendingInvites()) buf.writeUtf(p);
                buf.writeFloat(v.food());
                buf.writeFloat(v.materials());
                buf.writeFloat(v.prosperity());
                buf.writeFloat(v.foodDrain());
                buf.writeFloat(v.materialsDrain());
                buf.writeFloat(v.prosperityDrain());
            }, buf -> {
                boolean in = buf.readBoolean();
                String name = buf.readUtf();
                String king = buf.readUtf();
                int mn = buf.readVarInt();
                List<String> mem = new ArrayList<>(mn);
                for (int i = 0; i < mn; i++) mem.add(buf.readUtf());
                int idn = buf.readVarInt();
                List<UUID> ids = new ArrayList<>(idn);
                for (int i = 0; i < idn; i++) ids.add(buf.readUUID());
                int claims = buf.readVarInt();
                int color = buf.readInt();
                boolean isKing = buf.readBoolean();
                int pn = buf.readVarInt();
                List<String> pend = new ArrayList<>(pn);
                for (int i = 0; i < pn; i++) pend.add(buf.readUtf());
                float food = buf.readFloat(), mat = buf.readFloat(), pros = buf.readFloat();
                float fd = buf.readFloat(), md = buf.readFloat(), pd = buf.readFloat();
                return new SyncKingdomInfoPacket(in, name, king, mem, ids, claims, color, isKing, pend,
                        food, mat, pros, fd, md, pd);
            });

    public static final SyncKingdomInfoPacket NONE =
            new SyncKingdomInfoPacket(false, "", "", List.of(), List.of(), 0, 0xFFFFFF, false, List.of(),
                    0f, 0f, 0f, 0f, 0f, 0f);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SyncKingdomInfoPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientKingdomData.set(packet);
            com.kingdomrp.core.client.KingdomClientHooks.onKingdomSync();  // авто-рефреш экрана
        });
    }
}
