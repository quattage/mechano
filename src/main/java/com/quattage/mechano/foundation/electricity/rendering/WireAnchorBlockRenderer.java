package com.quattage.mechano.foundation.electricity.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoRenderTypes;
import com.quattage.mechano.foundation.electricity.AnchorPointBank;
import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.power.features.GID;
import com.quattage.mechano.foundation.electricity.spool.WireSpool;
import com.quattage.mechano.foundation.helper.VectorHelper;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.utility.Pair;

import java.util.HashSet;

import javax.annotation.Nullable;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class WireAnchorBlockRenderer<T extends WireAnchorBlockEntity> implements BlockEntityRenderer<T> {

    private static Entity renderSubject = null;
    private static BlockEntityRenderDispatcher cachedDispatcher;
    private static Minecraft instance = null;
    private static float time = 0;

    private static AnchorPoint selectedAnchor = null;

    private static final int ANCHOR_NORM_SIZE = 25;
    public static final int ANCHOR_SELECT_SIZE = 40;
    private static final int ANCHOR_HOOK_RENDER_DISTANCE = 11;
    

    private static Vec3 oldToPos = new Vec3(0, 0, 0);

    private static final HashSet<AnchorPoint> nearbyAnchors = new HashSet<AnchorPoint>();

    public WireAnchorBlockRenderer(BlockEntityRendererProvider.Context context) {
        super();
        identifyRenderer(context.getBlockEntityRenderDispatcher());
    }

    @Override
    public void render(T be, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferSource, int light,
            int overlay) {

        identifyRenderer(cachedDispatcher);
        if(renderSubject instanceof Player player) {
            showNearbyAnchorPoints(player, be, partialTicks);
            showWireProgress(player, be, partialTicks, matrixStack, bufferSource);
        }

        return;
    }

    @Override
    public boolean shouldRender(T pBlockEntity, Vec3 pCameraPos) {
        return true;
    }

    @Override
    public boolean shouldRenderOffScreen(T pBlockEntity) {
        return true;
    }

    private void showWireProgress(Player player, T be, float pTicks, PoseStack matrixStack, MultiBufferSource bufferSource) {

        // world sanity checks
        if(player == null) return;
        Level world = player.level();
        if(!world.isClientSide()) return;
        if(!(be instanceof WireAnchorBlockEntity wbe)) return;
        if(!instance.options.getCameraType().isFirstPerson()) return;

        // spool sanity checks
        ItemStack spool = WireSpool.getHeldSpool(player);
        if(spool == null) return;
        CompoundTag spoolTag = spool.getOrCreateTag();
        if(!GID.isValidTag(spoolTag)) return;

        // anchor sanity checks
        GID connectID = GID.of(spoolTag);
        Pair<AnchorPoint, WireAnchorBlockEntity> targetAnchor = AnchorPoint.getAnchorAt(world, connectID);
        if(targetAnchor == null || targetAnchor.getFirst() == null || !wbe.equals(targetAnchor.getSecond())) 
            return;

        if(time < 1)
            time += 0.001f;
        else
            time = 0;

        Vec3 fromPos = targetAnchor.getFirst().getPos();
        Vec3 fromOffset = targetAnchor.getFirst().getLocalOffset();
        Vec3 toPos;
        boolean isAnchored = false;

        
        if(selectedAnchor != null && AnchorPoint.getAnchorAt(world, selectedAnchor.getID()) == null)
            selectedAnchor = null;

        if(selectedAnchor != null && !selectedAnchor.equals(targetAnchor.getFirst())) {
            isAnchored = true;
            toPos = oldToPos.lerp(selectedAnchor.getPos(), 0.04);
        }
        else if(instance.hitResult instanceof BlockHitResult hit)
            toPos = oldToPos.lerp(hit.getBlockPos().relative(hit.getDirection(), 1).getCenter(), 0.04);
        else
            toPos = oldToPos;

        matrixStack.pushPose();
        matrixStack.translate(fromOffset.x, fromOffset.y, fromOffset.z);
        VertexConsumer buffer = bufferSource.getBuffer(MechanoRenderTypes.getWireTranslucent(((WireSpool)spool.getItem()).asResource()));

        Vector3f offset = WireModelRenderer.getWireOffset(fromPos, toPos);
        matrixStack.translate(offset.x(), 0, offset.z());
        
        Vec3 startPos = fromPos.add(offset.x(), 0, offset.z());
        Vec3 endPos = toPos.add(-offset.x(), 0, -offset.z());
        Vector3f wireOrigin = new Vector3f((float)(endPos.x - startPos.x), (float)(endPos.y - startPos.y), (float)(endPos.z - startPos.z));

        float angleY = -(float)Math.atan2(wireOrigin.z(), wireOrigin.x());
        matrixStack.mulPose(new Quaternionf().rotateXYZ(0, angleY, 0));

        WireModelRenderer.INSTANCE.renderDynamic(buffer, matrixStack, wireOrigin, 1, 15, 4, 15, !isAnchored, (int)((Math.sin(time * 3.1) * 89f) + 60f));
        matrixStack.popPose();

        oldToPos = toPos;
    }

    private void showNearbyAnchorPoints(Player player, T be, float pTicks) {

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
        instance = Minecraft.getInstance();
    }
}
