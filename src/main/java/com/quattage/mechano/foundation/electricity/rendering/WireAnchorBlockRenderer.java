package com.quattage.mechano.foundation.electricity.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.MechanoRenderTypes;
import com.quattage.mechano.foundation.block.anchor.AnchorPoint;
import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.electricity.WattBatteryHandlable.ExternalInteractMode;
import com.quattage.mechano.foundation.electricity.WireSpool;
import com.quattage.mechano.foundation.electricity.grid.GridClientCache;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GID;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridClientEdge;
import com.quattage.mechano.foundation.electricity.impl.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.impl.WireAnchorBlockEntity.ChevronTransform;
import com.quattage.mechano.foundation.helper.VectorHelper;

import net.createmod.catnip.data.Pair;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.quattage.mechano.MechanoClient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WireAnchorBlockRenderer<T extends WireAnchorBlockEntity> implements BlockEntityRenderer<T> {

    private static Vec3 oldToPos = new Vec3(0, 0, 0);
    private static GridClientCache cache = null;
    private static float timeTick = 0;

    public WireAnchorBlockRenderer(BlockEntityRendererProvider.Context context) {
        super();
    }

    @Override
    @SuppressWarnings("resource")
    public void render(T be, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferSource, int light,
            int overlay) {

        if(!be.hasLevel() || be.getBlockState().getBlock() == Blocks.AIR) return;

        drawWireProgress(
            Minecraft.getInstance().player, 
            be, partialTicks, 
            matrixStack, bufferSource, 
            ((WireAnchorBlockEntity)be).getTimeSinceLastRender()
        );

        MechanoClient.ANCHOR_SELECTOR.tickAnchorsBelongingTo(be);
        if(cache == null) cache = GridClientCache.ofInstance();
        else drawWigglyWires(be, partialTicks, cache, matrixStack, bufferSource);
        drawChevron(be, partialTicks, matrixStack, bufferSource);
    }

    @Override
    public boolean shouldRenderOffScreen(T be) {
        return true;
    }

    private void drawWigglyWires(T be, float pTicks, GridClientCache cache, PoseStack matrixStack, MultiBufferSource bufferSource) {
        
        for(GridClientEdge edge : cache.getAllNewEdges()) {

            if(!edge.getSideA().getBlockPos().equals(be.getBlockPos())) continue;

            float age = edge.getAge();
            
            float percent = 1 - (age / (float)edge.getInitialAge());
            float sagOverride = scalarToSag(percent);

            Pair<AnchorPoint, WireAnchorBlockEntity> fromAnchor = AnchorPoint.getAnchorAt(cache.getWorld(), edge.getSideA());
            if(!isValidPair(fromAnchor)) continue;
        
            Pair<AnchorPoint, WireAnchorBlockEntity> toAnchor = AnchorPoint.getAnchorAt(cache.getWorld(), edge.getSideB());
            if(!isValidPair(toAnchor)) continue;

            Vec3 fromPos = fromAnchor.getFirst().getPos();
            Vec3 fromOffset = fromAnchor.getFirst().getLocalOffset();
            Vec3 toPos = toAnchor.getFirst().getPos();

            matrixStack.pushPose();
            matrixStack.translate(fromOffset.x, fromOffset.y, fromOffset.z);
            VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(WireSpool.ofType(edge.getTypeID()).asResource()));

            Vector3f offset = WirePipeline.getWireOffset(fromPos, toPos);
            int[] lightmap = WirePipeline.deriveLightmap(cache.getWorld(), fromPos, toPos);

            matrixStack.translate(offset.x(), 0, offset.z());
            
            Vec3 startPos = fromPos.add(offset.x(), 0, offset.z());
            Vec3 endPos = toPos.add(-offset.x(), 0, -offset.z());
            Vector3f wireOrigin = new Vector3f((float)(endPos.x - startPos.x), (float)(endPos.y - startPos.y), (float)(endPos.z - startPos.z));

            float angleY = -(float)Math.atan2(wireOrigin.z(), wireOrigin.x());
            matrixStack.mulPose(new Quaternionf().rotateXYZ(0, angleY, 0));

            WirePipeline.INSTANCE.renderDynamic(buffer, matrixStack, wireOrigin, sagOverride, lightmap[0], lightmap[1], lightmap[2], lightmap[3]);
            matrixStack.popPose();
        }
    }

    private float scalarToSag(float x) {
        float c4 = (2f * (float)Math.PI) / 3f;
        return x == 0 ? 0 : x == 1 ? 1 : (float)Math.pow(2f, -10f * x) * (float)Math.sin((x * 10f - 0.75f) * c4) + 1;
    }

    @SuppressWarnings("resource")
    private void drawWireProgress(Player player, T be, float pTicks, PoseStack matrixStack, MultiBufferSource bufferSource, double delta) {

        if(timeTick < 1) timeTick += delta * 0.0001f;
        else timeTick = 0;

        // world sanity checks
        if(player == null) return;
        Level world = player.level();
        if(!world.isClientSide()) return;
        if(!Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;

        // spool sanity checks
        ItemStack spool = WireSpool.getHeldByPlayer(player);
        if(spool == null) return;
        CompoundTag spoolTag = spool.getOrCreateTag();
        if(!GID.isValidTag(spoolTag)) return;

        GID connectID = GID.of(spoolTag);
        Pair<AnchorPoint, WireAnchorBlockEntity> targetAnchor = AnchorPoint.getAnchorAt(world, connectID);

        // anchor sanity checks
        if(!isValidPair(targetAnchor)) return;
        if(!be.equals(targetAnchor.getSecond())) return;

        Vec3 fromPos = targetAnchor.getFirst().getPos();
        Vec3 fromOffset = targetAnchor.getFirst().getLocalOffset();
        Vec3 toPos;
        boolean isAnchored = false;

        AnchorPoint selectedAnchor = MechanoClient.ANCHOR_SELECTOR.getSelectedAnchor(world);
        if(selectedAnchor != null && !selectedAnchor.equals(targetAnchor.getFirst())) {
            isAnchored = true;
            toPos = oldToPos.lerp(selectedAnchor.getPos(), pTicks * delta * 0.1);
        }
        else if(Minecraft.getInstance().hitResult instanceof BlockHitResult hit)
            toPos = oldToPos.lerp(hit.getBlockPos().relative(hit.getDirection(), 1).getCenter(), pTicks * delta * 0.04);
        else
            toPos = oldToPos;

        matrixStack.pushPose();
        matrixStack.translate(fromOffset.x, fromOffset.y, fromOffset.z);
        VertexConsumer buffer = bufferSource.getBuffer(MechanoRenderTypes.getWireTranslucent(((WireSpool)spool.getItem()).asResource()));

        Vector3f offset = WirePipeline.getWireOffset(fromPos, toPos);
        matrixStack.translate(offset.x(), 0, offset.z());
        
        Vec3 startPos = fromPos.add(offset.x(), 0, offset.z());
        Vec3 endPos = toPos.add(-offset.x(), 0, -offset.z());
        Vector3f wireOrigin = new Vector3f((float)(endPos.x - startPos.x), (float)(endPos.y - startPos.y), (float)(endPos.z - startPos.z));

        float angleY = -(float)Math.atan2(wireOrigin.z(), wireOrigin.x());
        matrixStack.mulPose(new Quaternionf().rotateXYZ(0, angleY, 0));

        WirePipeline.INSTANCE.renderDynamic(buffer, matrixStack, wireOrigin, 1, 15, 4, 15, !isAnchored, (int)((Math.sin(timeTick * 3.1) * 89f) + 60f));
        matrixStack.popPose();

        oldToPos = toPos;
    }

    public static boolean isValidPair(Pair<?, ?> pair) {
        if(pair == null) return false;
        if(pair.getFirst() == null) return false;
        if(pair.getSecond() == null) return false;
        return true;
    }

    private void drawChevron(T be, float pTicks, PoseStack matrixStack, MultiBufferSource bufferSource) {

        if(!be.getWattBatteryHandler().getInteractionStatus().isInteracting()) return;

        CombinedOrientation orient = DirectionTransformer.extract(be.getBlockState());
        SuperByteBuffer headBufferA = CachedBuffers.partial(MechanoClient.PART_CHEV_OVERLAY, be.getBlockState());
        SuperByteBuffer headBufferB = CachedBuffers.partial(MechanoClient.PART_CHEV_OVERLAY_INV, be.getBlockState());
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutout());

        ChevronTransform[] chevrons = be.getChevronLocations();

        float scalar = Mth.lerp(pTicks, be.getPrevRotation(), be.getNextRotation());
    
        float progress = be.getWattBatteryHandler().getMode() == 
            ExternalInteractMode.PUSH_OUT ? (float)easeInElastic(scalar) * 2 : (float)easeOutElastic(scalar) * 2;

        for(int x = 0; x < chevrons.length; x++) {
            ChevronTransform transform = chevrons[x];
            Vector3f scaledOffset = VectorHelper.rotate(transform.getOffset(), orient);
            Direction rotationAxis = getRotationAxis(transform, orient);

            if(arbitrary(transform.getDefault(), orient)) {
                headBufferB
                    .translate(scaledOffset)
                    .rotate((float) (Math.PI / 2 * progress), rotationAxis)
                    .rotateToFace(orient.getLocalUp())
                    .renderInto(matrixStack, buffer);
            } else {
                headBufferA
                    .translate(scaledOffset)
                    .rotate((float) (Math.PI / 2 * progress), rotationAxis)
                    .rotateToFace(orient.getLocalUp())
                    .renderInto(matrixStack, buffer);
            }
        }
    }

    private double easeOutElastic(float x) {
        double c4 = (2 * Math.PI) / 3;
        return x == 0 ? 0 : x == 1 ? 1 : Math.pow(2, -10 * x) * Math.sin((x * 10 - 0.75) * c4) + 1;
    }


    private double easeInElastic(float x) {
        double c4 = (2 * Math.PI) / 3;
        return x == 0 ? 0 : x == 1 ? 1 : -Math.pow(2, 10 * x - 10) * Math.sin((x * 10 - 10.75) * c4);
    }


    // this is stupid but i hate matrix math and im tired
    private static boolean arbitrary(Direction a, CombinedOrientation b) {
        Direction dir = b.getLocalUp();
        Direction fac = b.getLocalForward();
        boolean out = dir.getAxis() == Axis.Y ? a.getAxis() == Axis.Z : a.getAxis() == Axis.X;
        boolean out2 = fac.getAxis() != Axis.Z ? !out : out;
        return dir.getAxis() == Axis.Z && fac.getAxis() == Axis.X ? !out2 : out2;
    }

    
    public Direction getRotationAxis(ChevronTransform transform, CombinedOrientation orient) {

        if(transform.getDefault() != Direction.EAST) 
            return transform.getRotated(orient);

        if(orient == CombinedOrientation.UP_SOUTH)
            return Direction.WEST;
        if(orient == CombinedOrientation.UP_WEST)
            return Direction.NORTH;
        if(orient == CombinedOrientation.DOWN_WEST)
            return Direction.SOUTH;
        if(orient == CombinedOrientation.NORTH_DOWN)
            return Direction.EAST;
        if(orient == CombinedOrientation.SOUTH_DOWN)
            return Direction.WEST;
        if(orient == CombinedOrientation.WEST_DOWN)
            return Direction.NORTH;
        
        return transform.getRotated(orient);
    }


    public static void resetOldPos(Player player, AnchorPoint anchor) {
        oldToPos = player.getOnPos().getCenter().lerp(anchor.getPos(), 0.5);
    }
}