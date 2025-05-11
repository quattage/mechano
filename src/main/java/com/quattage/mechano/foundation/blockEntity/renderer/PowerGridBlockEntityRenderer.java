package com.quattage.mechano.foundation.blockEntity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.foundation.api.PowerGridBlockEntity;
import com.quattage.mechano.foundation.api.client.AnchorPoint;
import com.quattage.mechano.foundation.api.client.AnchorSelector;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;

public class PowerGridBlockEntityRenderer<T extends PowerGridBlockEntity> extends SimpleBlockEntityRenderer<T> {

    public PowerGridBlockEntityRenderer(Context context) {
        super(context);
    }
    
    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight, int packedOverlay) {
        super.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        tickAnchors(blockEntity);
    }

    /**
     * This method continuously evaluates the visibility of AnchorPoints within this PGBE.
     * <p>
     * This is done in the renderer for a few reasons:
     * <ul>
     * <li>This process only needs to occur on the client</li>
     * <li>BERs have built-in frustum culling</li>
     * <li>This code is executed at the framerate of the game rather than a fixed rate</li>
     * </ul>
     * The visibility and interaction status of each anchor is evaluated in the {@link AnchorSelector.INSTANCE Anchor Selector}
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
}
