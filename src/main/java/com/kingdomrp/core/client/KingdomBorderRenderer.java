package com.kingdomrp.core.client;

import com.kingdomrp.core.KingdomRPCore;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ftb.mods.ftbchunks.client.map.MapChunk;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.client.map.MapRegion;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import dev.ftb.mods.ftblibrary.math.XZ;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import javax.annotation.Nullable;

/**
 * Полупрозрачные стены-границы королевств (цвет = цвет королевства), показываются
 * при включённых границах чанков (F3+G). Данные о claim'ах берём из клиентского кэша
 * FTB Chunks ({@link MapDimension} → {@link MapChunk#getTeam()}).
 */
@EventBusSubscriber(modid = KingdomRPCore.MODID, value = Dist.CLIENT)
public class KingdomBorderRenderer {

    private static final int RADIUS = 12;   // чанков вокруг игрока
    private static final int ALPHA = 190;
    private static final float TILE = 8f;   // блоков на тайл текстуры

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!ModList.get().isLoaded("ftbchunks")) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.debugRenderer == null
                || !((com.kingdomrp.core.mixin.client.DebugRendererAccessor) mc.debugRenderer).krp$isChunkBorder())
            return;   // F3+G выкл.
        if (mc.player == null || mc.level == null) return;

        MapDimension md = MapDimension.getCurrent().orElse(null);
        if (md == null) return;

        int minY = mc.level.getMinBuildHeight();
        int maxY = mc.level.getMaxBuildHeight();
        ChunkPos center = mc.player.chunkPosition();
        Vec3 cam = event.getCamera().getPosition();
        PoseStack.Pose pose = event.getPoseStack().last();

        // Скролл текстуры (эффект worldborder).
        float scroll = (float) (net.minecraft.Util.getMillis() % 3000L) / 3000f;
        MultiBufferSource.BufferSource src = mc.renderBuffers().bufferSource();
        VertexConsumer vc = src.getBuffer(KRPRenderTypes.KINGDOM_BORDER);

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                int cx = center.x + dx, cz = center.z + dz;
                Team team = teamAt(md, cx, cz);
                if (team == null) continue;
                int rgb = colorOf(team);
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;

                java.util.UUID id = team.getTeamId();
                int x0 = cx << 4, x1 = x0 + 16, z0 = cz << 4, z1 = z0 + 16;
                // Стена-барьер только на ребре, где сосед — не то же королевство (периметр).
                if (!sameTeam(md, cx - 1, cz, id)) wall(vc, pose, cam, x0, z0, x0, z1, minY, maxY, r, g, b, scroll);
                if (!sameTeam(md, cx + 1, cz, id)) wall(vc, pose, cam, x1, z0, x1, z1, minY, maxY, r, g, b, scroll);
                if (!sameTeam(md, cx, cz - 1, id)) wall(vc, pose, cam, x0, z0, x1, z0, minY, maxY, r, g, b, scroll);
                if (!sameTeam(md, cx, cz + 1, id)) wall(vc, pose, cam, x0, z1, x1, z1, minY, maxY, r, g, b, scroll);
            }
        }
        src.endBatch(KRPRenderTypes.KINGDOM_BORDER);
    }

    /** Текстурированная вертикальная стена по ребру (forcefield, скроллинг по высоте). */
    private static void wall(VertexConsumer vc, PoseStack.Pose pose, Vec3 cam,
                             int ax, int az, int bx, int bz, int minY, int maxY,
                             int r, int g, int b, float scroll) {
        float u0 = (ax + az) / TILE, u1 = (bx + bz) / TILE;   // по горизонтали вдоль ребра
        float vLo = minY / TILE + scroll, vHi = maxY / TILE + scroll;
        float fax = (float) (ax - cam.x), faz = (float) (az - cam.z);
        float fbx = (float) (bx - cam.x), fbz = (float) (bz - cam.z);
        float lo = (float) (minY - cam.y), hi = (float) (maxY - cam.y);
        vc.addVertex(pose, fax, lo, faz).setColor(r, g, b, ALPHA).setUv(u0, vLo);
        vc.addVertex(pose, fbx, lo, fbz).setColor(r, g, b, ALPHA).setUv(u1, vLo);
        vc.addVertex(pose, fbx, hi, fbz).setColor(r, g, b, ALPHA).setUv(u1, vHi);
        vc.addVertex(pose, fax, hi, faz).setColor(r, g, b, ALPHA).setUv(u0, vHi);
    }

    @Nullable
    private static Team teamAt(MapDimension md, int cx, int cz) {
        MapRegion region = md.getRegion(XZ.regionFromChunk(cx, cz));
        if (region == null) return null;
        MapChunk chunk = region.getChunkForAbsoluteChunkPos(XZ.of(cx, cz));
        if (chunk == null) return null;
        return chunk.getTeam().orElse(null);
    }

    private static boolean sameTeam(MapDimension md, int cx, int cz, java.util.UUID id) {
        Team t = teamAt(md, cx, cz);
        return t != null && t.getTeamId().equals(id);
    }

    private static int colorOf(Team team) {
        try {
            return team.getProperty(TeamProperties.COLOR).rgb();
        } catch (Exception e) {
            return 0xFFFFFF;
        }
    }
}
