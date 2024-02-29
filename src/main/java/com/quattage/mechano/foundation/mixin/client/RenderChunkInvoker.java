package com.quattage.mechano.foundation.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.RenderChunk;

@Mixin(RenderChunk.class)
public interface RenderChunkInvoker {
    
    @Invoker
    void invokeBeginLayer(BufferBuilder pBuilder);
}
