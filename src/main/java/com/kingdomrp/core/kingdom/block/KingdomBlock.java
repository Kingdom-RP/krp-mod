package com.kingdomrp.core.kingdom.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import com.mojang.serialization.MapCodec;

import javax.annotation.Nullable;

/**
 * Блок королевства. ПКМ открывает меню (слот хартии + кнопка «Создать»).
 * Устойчив к взрывам/толканию поршнем (чтобы force-load-тикет не «утёк»).
 * Защита от ломания активного блока и правила неба — обработчики событий (шаг 4).
 */
public class KingdomBlock extends BaseEntityBlock {

    public static final MapCodec<KingdomBlock> CODEC = simpleCodec(KingdomBlock::new);

    public KingdomBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KingdomBlockEntity(pos, state);
    }


    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp
                && level.getBlockEntity(pos) instanceof KingdomBlockEntity be) {
            // Активный блок (ресурсы/содержание) — только жителям; неактивный (создание) — всем.
            if (be.isActive()) {
                var k = com.kingdomrp.core.kingdom.KingdomData.get(sp.server).byId(be.getKingdomId());
                if (k == null || !k.isMember(sp.getUUID())) {
                    sp.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                            "kingdomrp.block.not_member").withStyle(net.minecraft.ChatFormatting.RED), true);
                    return InteractionResult.SUCCESS;
                }
            }
            boolean isKing = be.isKing(sp);
            sp.openMenu(be, buf -> { buf.writeBlockPos(pos); buf.writeBoolean(isKing); });
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof KingdomBlockEntity be) {
            Containers.dropContents(level, pos, be.getCharterSlot());  // не терять хартию
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
