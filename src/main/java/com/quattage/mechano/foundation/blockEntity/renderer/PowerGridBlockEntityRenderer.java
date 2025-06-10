
package com.quattage.mechano.foundation.blockEntity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.PowerGridBlockEntity;
import com.quattage.mechano.foundation.api.landmark.client.AnchorPoint;
import com.quattage.mechano.foundation.api.landmark.client.AnchorSelector;
import com.quattage.mechano.foundation.api.transmitter.MechanoTransmissionTypes;
import com.quattage.mechano.foundation.catenary.CatenaryGeometry;
import com.quattage.mechano.foundation.catenary.mesh.WireModel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PowerGridBlockEntityRenderer<T extends PowerGridBlockEntity> extends SimpleBlockEntityRenderer<T> {

    public static Vec3 endPos = null;
    private static CatenaryGeometry cat;

    public PowerGridBlockEntityRenderer(Context context) {
        super(context);
    }
    
    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight, int packedOverlay) {
        super.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        tickAnchors(blockEntity);
        test(blockEntity, poseStack, bufferSource, partialTick);
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
    public void tickAnchors(T be) {
        LocalPlayer player = Minecraft.getInstance().player;
        if(player == null) return;
        be.anchors.forEach(anchor -> {
            float distance = (float)player.getEyePosition().distanceTo(anchor.getRealPosition());
            if(distance > AnchorPoint.VIS_RANGE) return;
            AnchorSelector.INSTANCE.track(be, anchor, distance);
        });
    }

    public void test(T pgbe, PoseStack matrixStack, MultiBufferSource buffers, float pTicks) {
        if(endPos == null) return;
        Long start = System.nanoTime();
        WireModel<?> wire = WireModel.simulate(pgbe.anchors.getByIndex(0).getRealPosition(), endPos);
        cat = CatenaryGeometry.as(MechanoTransmissionTypes.HOOKUP)
            .in(pgbe.getLevel())
            .withPosition(pgbe.anchors.getByIndex(0).getRealPosition())
            .render(buffers, matrixStack, wire, pgbe.anchors.getByIndex(0).getOffset(), pTicks);
        // Mechano.LOGGER.warn("TIME: " + ((System.nanoTime() - start) / 1000000f) + "ms");
    }

    @Override
    public boolean shouldRender(T blockEntity, Vec3 cameraPos) {
        return true;
    }

    @Override
    public boolean shouldRenderOffScreen(T blockEntity) {
        return true;
    }
    
    @Override
    public AABB getRenderBoundingBox(T blockEntity) {
        return AABB.INFINITE;
    }
}
