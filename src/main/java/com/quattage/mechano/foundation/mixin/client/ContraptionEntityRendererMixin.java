package com.quattage.mechano.foundation.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.foundation.api.catenary.CatenaryAccessor;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.render.ContraptionEntityRenderer;

import net.minecraft.client.renderer.MultiBufferSource;

@Mixin(ContraptionEntityRenderer.class)
public abstract class ContraptionEntityRendererMixin {
    
    @Inject(method = "render", at = @At(value = "TAIL"), cancellable = false, remap = false)
    private void mechano$renderContraptionCatenaries(AbstractContraptionEntity entity, float yaw, float pTicks, PoseStack poseStack, MultiBufferSource buffers, int overlay, CallbackInfo info) {
        ((CatenaryAccessor)entity).forEachCatenary(cat -> {
            cat.render(entity, buffers, poseStack, pTicks);
        });
    }
}
