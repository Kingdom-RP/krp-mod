package com.kingdomrp.core.mixin.client;

import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Чтение приватного флага границ чанков (F3+G) для рендера границ королевств. */
@Mixin(value = DebugRenderer.class, remap = false)
public interface DebugRendererAccessor {
    @Accessor("renderChunkborder")
    boolean krp$isChunkBorder();
}
