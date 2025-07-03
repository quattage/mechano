package com.quattage.mechano.foundation.mixin.client.accessor;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;


@Mixin(LevelRenderer.class)
public interface RenderBuffersAccessor {
    /**
     * Accesses and returns {@link LevelRenderer#renderBuffers}.
     * @return {@link RenderBuffers} instance belonging to this {@link LevelRenderer}
     */
    @Accessor("renderBuffers")
    @Nullable RenderBuffers mechano$getRenderBuffers();
}
