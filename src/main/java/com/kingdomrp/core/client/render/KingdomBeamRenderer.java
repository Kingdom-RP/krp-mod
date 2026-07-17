package com.kingdomrp.core.client.render;

import com.kingdomrp.core.kingdom.block.KingdomBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Луч-маяк над активным блоком королевства. Цвет — цвет королевства (синкается в
 * {@link KingdomBlockEntity#getBeamColor()}). Рисуется ванильным
 * {@link BeaconRenderer#renderBeaconBeam}.
 */
public class KingdomBeamRenderer implements BlockEntityRenderer<KingdomBlockEntity> {

    public KingdomBeamRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(KingdomBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        if (!be.isActive() || be.getLevel() == null) return;
        int color = 0xFF000000 | (be.getBeamColor() & 0xFFFFFF);   // ARGB, непрозрачный
        BeaconRenderer.renderBeaconBeam(pose, buffers, BeaconRenderer.BEAM_LOCATION,
                partialTick, 1.0f, be.getLevel().getGameTime(), 0, 1024, color, 0.2f, 0.25f);
    }

    @Override
    public boolean shouldRenderOffScreen(KingdomBlockEntity be) { return true; }

    @Override
    public int getViewDistance() { return 256; }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(KingdomBlockEntity be) {
        return net.minecraft.world.phys.AABB.INFINITE;   // высокий луч — не кулить
    }
}
