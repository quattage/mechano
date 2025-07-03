
package com.quattage.mechano.foundation.blockEntity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.anchor.AnchorSelector;
import com.quattage.mechano.foundation.blockEntity.GriddableBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class GriddableBlockEntityRenderer<T extends GriddableBlockEntity> implements BlockEntityRenderer<T> {

    
    public GriddableBlockEntityRenderer(Context context) {}

    @Override
    public void render(T pgbe, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight, int packedOverlay) {
        LocalPlayer player = Minecraft.getInstance().player;
        if(player == null) return;
        double reach = player.getAttributes().getValue(Attributes.ENTITY_INTERACTION_RANGE);
        tickAnchors(player, pgbe, reach);
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
    public void tickAnchors(LocalPlayer player, T pgbe, double reach) {
        if(player == null) return;
        if(!pgbe.isVisible()) return;
        pgbe.getAnchors().forEach(anchor -> {
            float distance = (float)anchor.distanceTo(player);
            if(distance > reach * 1.5f) return;
            AnchorSelector.INSTANCE.track(pgbe, anchor, distance);
        });
    }
}
