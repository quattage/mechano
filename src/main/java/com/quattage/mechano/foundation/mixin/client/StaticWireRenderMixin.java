package com.quattage.mechano.foundation.mixin.client;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.quattage.mechano.foundation.electricity.power.GridClientCache;

import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;


// When chunk geometry is built, this mixin injects additional geometry to render Mechano's wires.
// The geometry to inject is based on the GridClientCache's ledger of active edges, received from
// the GridSyncDirector.
// TODO Sodium derivatives need custom implementation for this to function. 
@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk$RebuildTask")
public class StaticWireRenderMixin {

    @SuppressWarnings("target") @Shadow(aliases = {"this$1", "f_112859_"}) 
    private ChunkRenderDispatcher.RenderChunk this$1;
    private Set<RenderType> mechano$chunkRenderTypes;

    @ModifyVariable(method = "compile", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;create()Lnet/minecraft/util/RandomSource;"))
	public Set<RenderType> getRenderLayers(Set<RenderType> set) {
		this.mechano$chunkRenderTypes = set;
		return set;
	}

    @Inject(method = "compile", at = @At(value = "INVOKE", target = "Ljava/util/Set;iterator()Ljava/util/Iterator;", remap = false))
    public void injectEdgeRendering(float pX, float pY, float pZ, ChunkBufferBuilderPack buffer, CallbackInfoReturnable<?> cir) {
        GridClientCache.INSTANCE.renderConnectionsInChunk(this$1, mechano$chunkRenderTypes, buffer, this$1.getOrigin());
		this.mechano$chunkRenderTypes = null;
	}
}
