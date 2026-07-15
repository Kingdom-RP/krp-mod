package com.kingdomrp.core.kingdom.item;

import com.kingdomrp.core.kingdom.Kingdom;
import com.kingdomrp.core.kingdom.KingdomData;
import com.kingdomrp.core.kingdom.KingdomManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;

/**
 * Кольцо лорда — расширение территории. ПКМ по земле в свободном чанке, соседнем
 * с чанком своего королевства → чанк присоединяется. Крафт доступен только королю
 * (гейт — позже); использовать может любой член королевства.
 */
public class LordRingItem extends Item {

    public LordRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getLevel().isClientSide) return InteractionResult.SUCCESS;
        if (!(ctx.getPlayer() instanceof ServerPlayer player)) return InteractionResult.PASS;

        var server = player.server;
        KingdomData data = KingdomData.get(server);
        Kingdom k = data.byPlayer(player.getUUID());
        if (k == null) {
            msg(player, "kingdomrp.ring.no_kingdom", ChatFormatting.RED);
            return InteractionResult.FAIL;
        }

        ChunkPos target = new ChunkPos(ctx.getClickedPos());
        if (data.byChunk(target) != null) {
            msg(player, "kingdomrp.ring.occupied", ChatFormatting.RED);
            return InteractionResult.FAIL;
        }
        if (!KingdomManager.isAdjacent(k, target)) {
            msg(player, "kingdomrp.ring.not_adjacent", ChatFormatting.RED);
            return InteractionResult.FAIL;
        }

        if (KingdomManager.expand(server, k, target)) {
            com.kingdomrp.core.kingdom.KingdomSync.broadcast(server, k);
            msg(player, "kingdomrp.ring.expanded", ChatFormatting.GREEN);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }

    private static void msg(ServerPlayer player, String key, ChatFormatting color) {
        player.displayClientMessage(Component.translatable(key).withStyle(color), true);
    }
}
