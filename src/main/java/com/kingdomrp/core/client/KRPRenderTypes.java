package com.kingdomrp.core.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Кастомный render type для границ королевств: ванильная forcefield-текстура
 * (как worldborder), аддитивная полупрозрачность, без отсечения граней, depth-test
 * LEQUAL (перекрывается рельефом), без записи глубины.
 *
 * <p>Наследуемся от {@link RenderType} только чтобы получить доступ к protected
 * static-шардам ({@code POSITION_COLOR_TEX_SHADER} и т.п.). Инстанс не создаётся.
 */
public final class KRPRenderTypes extends RenderType {

    private static final ResourceLocation FORCEFIELD =
            ResourceLocation.withDefaultNamespace("textures/misc/forcefield.png");

    public static final RenderType KINGDOM_BORDER = create(
            "kingdomrpcore:kingdom_border",
            DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 256, false, true,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
                    .setTextureState(new TextureStateShard(FORCEFIELD, false, false))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)   // аддитивная (SRC_ALPHA, ONE)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)                 // не пишем глубину
                    .setDepthTestState(LEQUAL_DEPTH_TEST)           // перекрывается рельефом
                    .createCompositeState(false));

    private KRPRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                          boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
        throw new UnsupportedOperationException();   // не инстанцируется
    }
}
