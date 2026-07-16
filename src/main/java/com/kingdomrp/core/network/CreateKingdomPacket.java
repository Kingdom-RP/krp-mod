package com.kingdomrp.core.network;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.kingdom.CharterData;
import com.kingdomrp.core.kingdom.KingdomData;
import com.kingdomrp.core.kingdom.KingdomManager;
import com.kingdomrp.core.kingdom.block.KingdomBlockEntity;
import com.kingdomrp.core.kingdom.item.CharterItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Клиент → сервер: кнопка «Создать королевство» в меню блока.
 * Валидация: блок на месте, игрок = король хартии, ≥2 соподписанта, область 5×5
 * свободна. При успехе — {@link KingdomManager#create}, блок помечается активным,
 * хартия расходуется.
 */
public record CreateKingdomPacket(BlockPos pos, int color) implements CustomPacketPayload {

    public static final Type<CreateKingdomPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KingdomRPCore.MODID, "create_kingdom"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CreateKingdomPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, CreateKingdomPacket::pos,
                    net.minecraft.network.codec.ByteBufCodecs.INT, CreateKingdomPacket::color,
                    CreateKingdomPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(CreateKingdomPacket packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();

        // Дистанция (анти-чит на пакет): блок в разумной близости.
        if (!player.blockPosition().closerThan(packet.pos(), 8.0)) return;
        if (!(level.getBlockEntity(packet.pos()) instanceof KingdomBlockEntity be)) return;
        if (be.isActive()) { msg(player, "kingdomrp.create.already", ChatFormatting.RED); return; }

        ItemStack charter = be.getCharter();
        CharterData d = CharterItem.data(charter);
        if (d == null) { msg(player, "kingdomrp.create.no_charter", ChatFormatting.RED); return; }
        if (!d.king().equals(player.getUUID())) { msg(player, "kingdomrp.create.not_king", ChatFormatting.RED); return; }
        if (!d.readyToCreate()) {
            msg(player, "kingdomrp.create.need_cosigners", ChatFormatting.RED);
            return;
        }

        KingdomData data = KingdomData.get(player.server);
        if (!KingdomManager.isAreaFree(data, level.dimension(), new net.minecraft.world.level.ChunkPos(packet.pos()))) {
            msg(player, "kingdomrp.create.area_taken", ChatFormatting.RED);
            return;
        }

        // Соподписанты должны быть онлайн — присоединяем их в команду FTB при создании.
        for (UUID c : d.cosigners()) {
            if (player.server.getPlayerList().getPlayer(c) == null) {
                msg(player, "kingdomrp.create.cosigner_offline", ChatFormatting.RED);
                return;
            }
        }

        // Король сам не должен уже состоять в королевстве (владельца хартии не вычистить).
        if (data.byPlayer(d.king()) != null) {
            msg(player, "kingdomrp.create.king_in_kingdom", ChatFormatting.RED);
            return;
        }

        // Протухшие соподписанты (успели вступить в другое королевство) — убираем из
        // хартии, чтобы не блокировать её навсегда; король добирает новую подпись.
        List<UUID> valid = new ArrayList<>();
        boolean removed = false;
        for (UUID c : d.cosigners()) {
            if (data.byPlayer(c) != null) removed = true;
            else valid.add(c);
        }
        if (removed) {
            CharterItem.set(charter, new CharterData(d.name(), d.king(), valid));
            be.getCharterSlot().setChanged();
            msg(player, "kingdomrp.create.cosigner_removed", ChatFormatting.RED);
            return;
        }

        Set<UUID> members = new LinkedHashSet<>();
        members.add(d.king());
        members.addAll(d.cosigners());

        var kingdom = KingdomManager.create(player.server, level, packet.pos(), d.name(), d.king(),
                members, packet.color());
        be.setKingdomId(kingdom.getId());   // хартия остаётся в слоте, запирается (mayPickup=false)

        com.kingdomrp.core.kingdom.KingdomSync.broadcast(player.server, kingdom);
        player.closeContainer();   // переоткрытие покажет активное состояние (без кнопки)
        msg(player, "kingdomrp.create.success", ChatFormatting.GREEN);
    }

    private static void msg(ServerPlayer player, String key, ChatFormatting color) {
        player.displayClientMessage(Component.translatable(key).withStyle(color), false);
    }
}
