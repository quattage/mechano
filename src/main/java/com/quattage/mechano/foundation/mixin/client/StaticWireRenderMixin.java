package com.quattage.mechano.foundation.mixin.client;

import java.util.Set;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.quattage.mechano.Mechano;

import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.world.level.BlockAndTintGetter;


@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk$RebuildTask")
public class StaticWireRenderMixin {

    @Nullable
    private RenderChunkRegion renderRegion;
    
    private Set<RenderType> mechano$chunkRenderTypes;
    private BlockAndTintGetter mechano$chunkTintGetterCopy;

    @ModifyVariable(method = "compile", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;create()Lnet/minecraft/util/RandomSource;"))
	public Set<RenderType> copyRenderLayers(Set<RenderType> set) {
		this.mechano$chunkRenderTypes = set;
		return set;
	}

    @Inject(method = "compile", at = @At(value = "INVOKE", target = "Ljava/util/Set;iterator()Ljava/util/Iterator;", remap = false))
    public void addConnectionQuads(float pX, float pY, float pZ, ChunkBufferBuilderPack pBuffers, CallbackInfoReturnable<?> cir) {
		reset();
	}

    private void reset() {
        this.mechano$chunkRenderTypes = null;
        this.mechano$chunkTintGetterCopy = null;
    }
}
