package com.quattage.mechano.foundation.api.entity;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public class GriddableEntityRenderer extends EntityRenderer<GriddableEntity> {

        public GriddableEntityRenderer(Context context) {
            super(context);
        }

        @Override
        public boolean shouldRender(GriddableEntity livingEntity, Frustum camera, double camX, double camY, double camZ) { return false; }

        @Override
        public void render(GriddableEntity p_entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {}

        @Override
        public ResourceLocation getTextureLocation(GriddableEntity entity) {
            return null;
        }
}
