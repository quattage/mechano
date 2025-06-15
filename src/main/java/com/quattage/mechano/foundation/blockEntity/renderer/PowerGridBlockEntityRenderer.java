
package com.quattage.mechano.foundation.blockEntity.renderer;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.foundation.api.PowerGridBlockEntity;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.anchor.AnchorSelector;
import com.quattage.mechano.foundation.api.landmark.uuid.GridUUID;
import com.quattage.mechano.foundation.catenary.Catenary;
import com.quattage.mechano.foundation.catenary.meshing.GeoHolder;
import com.quattage.mechano.foundation.catenary.model.SimulatedCatenary;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer; 
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PowerGridBlockEntityRenderer<T extends PowerGridBlockEntity> extends SimpleBlockEntityRenderer<T> {

    public static @Nullable AnchorPoint selected;
    public static @Nullable GeoHolder mesher = null;
    public static @Nullable Catenary<?> catenary = null;

    public PowerGridBlockEntityRenderer(Context context) {
        super(context);
    }
    
    @Override
    public void render(T pgbe, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight, int packedOverlay) {
        super.render(pgbe, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        LocalPlayer player = Minecraft.getInstance().player;
        if(player == null) return;
        double reach = player.getAttributes().getValue(Attributes.ENTITY_INTERACTION_RANGE);
        tickAnchors(player, pgbe, reach);
        renderPlayerWire(player, pgbe, poseStack, bufferSource, partialTick);
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
        pgbe.getAnchors().forEach(anchor -> {
            float distance = (float)anchor.distanceTo(player);
            if(distance > reach * 1.5f) return;
            AnchorSelector.INSTANCE.track(pgbe, anchor, distance);
        });
    }

    public void renderPlayerWire(LocalPlayer player, T pgbe, PoseStack matrixStack, MultiBufferSource buffers, float pTicks) {
        if(!pgbe.containsAnchor(selected)) return;

        if(player == null || selected == null) {
            if(mesher != null) {
                mesher = null;
                selected = null;
                catenary = null;
            }
            return;
        }
        Vec3 rp = selected.getPos();
        if(AnchorSelector.INSTANCE.lookingRay == null) return;
        if(mesher == null) {
            mesher = GeoHolder.as(AnchorSelector.INSTANCE.playerHands.implementingItem().getTransmitterType())
                .in(pgbe.getLevel())
                .withPosition(rp);
            catenary = new SimulatedCatenary().setOffset(rp, AnchorSelector.INSTANCE.lookingRay.end).initialize();
        }
        mesher.render(buffers, matrixStack, catenary, selected.getOffset(), pTicks);
    }

    @Override
    public boolean shouldRender(T blockEntity, Vec3 cameraPos) {
        if(mesher != null) return true;
        return super.shouldRender(blockEntity, cameraPos);
    }

    @Override
    public boolean shouldRenderOffScreen(T blockEntity) {
        if(mesher != null) return true;
        return super.shouldRenderOffScreen(blockEntity);
    }
    
    @Override
    public AABB getRenderBoundingBox(T blockEntity) {
        if(mesher != null) return AABB.INFINITE;
        return super.getRenderBoundingBox(blockEntity);
    }
}
