
package com.quattage.mechano.foundation.api.blockEntity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.anchor.AnchorSelector;
import com.quattage.mechano.foundation.api.blockEntity.GriddableBlockEntity;
import com.quattage.mechano.foundation.api.catenary.WindManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class GriddableBlockEntityRenderer<T extends GriddableBlockEntity> implements BlockEntityRenderer<T> {

    public GriddableBlockEntityRenderer(Context context) {}

    @Override
    public void render(T be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight, int packedOverlay) {
        LocalPlayer player = Minecraft.getInstance().player;
        if(player == null) return;
        double reach = player.getAttributes().getValue(Attributes.ENTITY_INTERACTION_RANGE);
        tickAnchors(player, be, reach);
        renderMovingWires(be, bufferSource, poseStack, partialTick);
    }

    /**
     * This method continuously evaluates the visibility of 
     * {@link AnchorPoint AnchorPoints} within this PGBE.
     * <p>
     * This is done in the renderer for a few reasons:
     * <ul>
     * <li>This process only needs to occur on the client</li>
     * <li>BERs have built-in frustum culling</li>
     * <li>This code is executed at the framerate of the game rather than a fixed rate</li>
     * </ul>
     * The visibility and interaction status of each anchor is evaluated in the {@link AnchorSelector#INSTANCE Anchor Selector}
     * @param be
     */
    public void tickAnchors(LocalPlayer player, T be, double reach) {
        be.getAnchors().forEach(anchor -> {
            float distance = (float)anchor.distanceTo(player);
            if(distance > reach * 1.5f) return;
            AnchorSelector.INSTANCE.trackForThisFrame(be, anchor, distance);
        });
    }

    public void renderMovingWires(T be, MultiBufferSource bufferSource, PoseStack matrixStack, float pTicks) {
        be.forEachCatenary(cat -> {
            if(!WindManager.INSTANCE.isEnabled() && !cat.isMoving(be.getLevel())) return;
            cat.render(be, bufferSource, matrixStack, pTicks);
        });
    }

    @Override
    public AABB getRenderBoundingBox(T be) {
        return be.getRenderBoundingBox();
    }
    
    @Override
    public boolean shouldRender(T be, Vec3 cameraPos) {
        if(be.getSurrogate() != null && be.getSurrogate().isSynced()) return true;
        return Vec3.atCenterOf(be.getBlockPos()).closerThan(cameraPos, (double)this.getViewDistance());
    }

    @Override
    public boolean shouldRenderOffScreen(T be) {
        return be.getSurrogate() != null && be.getSurrogate().isSynced();
    }
}
