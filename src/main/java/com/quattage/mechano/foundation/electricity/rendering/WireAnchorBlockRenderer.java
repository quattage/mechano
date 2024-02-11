package com.quattage.mechano.foundation.electricity.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.electricity.AnchorPointBank;
import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.spool.WireSpool;
import com.quattage.mechano.foundation.helper.VectorHelper;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.CreateClient;

import java.util.HashSet;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class WireAnchorBlockRenderer<T extends WireAnchorBlockEntity> implements BlockEntityRenderer<T> {

    // private final WireModelRenderer wireRenderer = new WireModelRenderer();
    Entity renderSubject = null;
    BlockEntityRenderDispatcher cachedDispatcher;
    private static AnchorPoint selectedAnchor = null;

    private static final int ANCHOR_NORM_SIZE = 25;
    public static final int ANCHOR_SELECT_SIZE = 40;
    private static final int ANCHOR_HOOK_RENDER_DISTANCE = 11;

    private static final HashSet<AnchorPoint> nearbyAnchors = new HashSet<AnchorPoint>();

    public WireAnchorBlockRenderer(BlockEntityRendererProvider.Context context) {
        super();
        identifyRenderer(context.getBlockEntityRenderDispatcher());
    }

    // TODO probably remove this idk if its useful
    public static HashSet<AnchorPoint> getNearbyAnchors() {
        return nearbyAnchors;
    }

    @Override
    public void render(T be, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light,
            int overlay) {

        identifyRenderer(cachedDispatcher);
        if(renderSubject instanceof Player player) {
            showHooks(player, be, partialTicks);
        }

        return;
    }

    private void showHooks(Player player, T be, float pTicks) {

        float distance = (float)player.position().distanceTo(be.getBlockPos().getCenter());
        float dmo = Mth.clamp((distance * -0.3f) + 3f, 0.4f, 6f);
        Vec3 raycastPos = VectorHelper.getLookingRay(player, 10).getLocation();
        AnchorPointBank<?> anchorBank = be.getAnchorBank();

        for(AnchorPoint anchor : anchorBank.getAll()) {
            if(anchor == null) continue;
            if((anchor.getSize() > 0 || (isHoldingSpool(player)) && distance < ANCHOR_HOOK_RENDER_DISTANCE)) {

                double dist = anchor.getDistanceToRaycast(player.getEyePosition(), raycastPos);

                if(dist < 0.6) {
                    nearbyAnchors.add(anchor);
                } else {
                    nearbyAnchors.remove(anchor);
                    if(anchor.equals(selectedAnchor)) selectedAnchor = null;
                }

                if(isHoldingSpool(player)) {

                    if(anchor.equals(selectedAnchor)) {
                        if(anchor.getSize() < ANCHOR_SELECT_SIZE) {
                            anchor.inflate(dmo);
                        } 
                    } else {

                        if(anchor.getSize() > ANCHOR_NORM_SIZE) {
                            anchor.deflate(dmo);
                        }

                        if(anchor.getSize() < ANCHOR_NORM_SIZE) {
                            anchor.inflate(dmo);
                        }
                    }
                } else {
                    anchor.deflate(dmo);
                }
            } else {
                if(anchor.getSize() > 0) {
                    anchor.deflate(dmo);
                }
            }

            if(anchor.getSize() > 0) {
                AABB anchorBox = anchor.getHitbox();
                if(anchorBox == null ) continue;

                CreateClient.OUTLINER.showAABB(anchor.hashCode(), anchorBox)
                    .disableLineNormals()
                    .withFaceTexture(AllSpecialTextures.CUTOUT_CHECKERED)
                    .colored(anchor.getColor())
                    .lineWidth(anchor.getSize() * 0.001f);
            }
        }

        double closestDist = -1;
        for(AnchorPoint near : nearbyAnchors) {
            if(near == null) continue;
            double dist = near.getDistanceToRaycast(player.getEyePosition(), raycastPos);
            if(closestDist == -1 || dist < closestDist) {
                closestDist = dist;
                selectedAnchor = near;
            }
        }
    }

    @Nullable
    public static AnchorPoint getSelectedAnchor() {
        return selectedAnchor;
    }

    public boolean isHoldingSpool(Player player) {
        return player.getMainHandItem().getItem() instanceof WireSpool ||
            player.getOffhandItem().getItem() instanceof WireSpool;
    }

    public void identifyRenderer(BlockEntityRenderDispatcher dispatcher) {
        if(dispatcher == null) return;
        cachedDispatcher = dispatcher;
        if(dispatcher.camera == null) return;
        renderSubject = dispatcher.camera.getEntity();
    }
}
