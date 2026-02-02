
package com.quattage.mechano.api.blockEntity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.api.blockEntity.GriddableBlockEntity;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.GriddableTerminus;
import com.quattage.mechano.api.grid.topology.landmark.AncillaryNode;
import com.quattage.mechano.api.switchboard.JackSelector;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class GriddableBlockEntityRenderer<T extends GriddableBlockEntity> implements BlockEntityRenderer<T> {

    public GriddableBlockEntityRenderer(Context context) {}

    @Override
    public void render(T be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight, int packedOverlay) {
        LocalPlayer player = Minecraft.getInstance().player;
        if(player == null) return;
        tickAnchors(player, be);
        renderMovingWires(be, bufferSource, poseStack, partialTick);
    }

    /**
     * This method continuously evaluates the visibility of 
     * {@link AncillaryNode} instances within the parent {@link Griddable}
     * <p>
     * This is done in the renderer for a few reasons:
     * <ul>
     * <li>This process only needs to occur on the client</li>
     * <li>BERs have built-in frustum culling</li>
     * <li>This code is executed at the framerate of the game rather than a fixed rate</li>
     * </ul>
     * The visibility and interaction status of each anchor is evaluated in the {@link JackSelector jack selector}
     * @param player the client player
     * @param be the block entity that's responsible for providing ancillaries
     */
    public void tickAnchors(LocalPlayer player, T be) {
        be.provideTerminus().forEach(joint -> {
            JackSelector.getInstance().trackForThisFrame(player, be, joint);
        });
    }

    public void renderMovingWires(T be, MultiBufferSource bufferSource, PoseStack matrixStack, float pTicks) {
        be.forEachExternalLink(link -> {
            // if(!WindManager.INSTANCE.isEnabled() && !cat.isMoving(be.getLevel())) return;
            link.render(be, bufferSource, matrixStack, pTicks);
        });
    }

    @Override
    public AABB getRenderBoundingBox(T be) {
        return be.getRenderBoundingBox();
    }
    
    @Override
    public boolean shouldRender(T be, Vec3 cameraPos) {
        if(shouldRenderOffScreen(be)) return true;
        return Vec3.atCenterOf(be.getBlockPos()).closerThan(cameraPos, (double)this.getViewDistance());
    }

    @Override
    public boolean shouldRenderOffScreen(T be) {
        GriddableTerminus gt = be.provideTerminus();
        return gt != null && gt.hasConnections();
    }
}
