package com.quattage.mechano.foundation.mixin.client;

import java.util.concurrent.atomic.AtomicReference;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.quattage.mechano.foundation.electricity.power.GridClientCache;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.SectionPos;

@Mixin(LevelRenderer.class)
public class StaticWireCullingMixin {

    @Final @Shadow private ObjectArrayList<LevelRenderer.RenderChunkInfo> renderChunksInFrustum;
    @Final @Shadow private AtomicReference<LevelRenderer.RenderChunkStorage> renderChunkStorage;

    // quick and dirty frustum culling patch so wires don't dissappear while in view
    // TODO more intelligent culling for better performance
    @Inject(method = "applyFrustum(Lnet/minecraft/client/renderer/culling/Frustum;)V", at = {@At(value = "TAIL")}, cancellable = true)
    private void applyFrustum(Frustum pFrustum, CallbackInfo info) {
        for(LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo : (this.renderChunkStorage.get()).renderChunks) {
            if(GridClientCache.INSTANCE.containsPos(SectionPos.of(levelrenderer$renderchunkinfo.chunk.getOrigin())))
                renderChunksInFrustum.add(levelrenderer$renderchunkinfo);
        }
    }
}
