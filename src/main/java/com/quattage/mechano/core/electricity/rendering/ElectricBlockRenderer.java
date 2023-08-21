package com.quattage.mechano.core.electricity.rendering;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.content.item.spool.WireSpool;
import com.quattage.mechano.core.electricity.blockEntity.ElectricBlockEntity;
import com.quattage.mechano.core.electricity.node.base.ElectricNode;
import com.quattage.mechano.core.electricity.node.connection.NodeConnection;
import com.quattage.mechano.core.util.VectorHelper;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ElectricBlockRenderer<T extends ElectricBlockEntity> extends SafeBlockEntityRenderer<T> {

    private final WireModelRenderer wireRenderer = new WireModelRenderer();
    private static final boolean USE_CACHE = true;

    public ElectricBlockRenderer(BlockEntityRendererProvider.Context context) {
        super();
    }   

    @Override
    protected void renderSafe(ElectricBlockEntity ebe, float partialTicks, PoseStack local, 
        MultiBufferSource bufferSource, int light, int overlay) {
    
        ElectricNode[] nodes = ebe.nodes.values();

        for(int n = 0; n < nodes.length; n++) {
            ElectricNode thisNode = nodes[n];
            NodeConnection[] theseConnections = thisNode.getConnections();

            for(int c = 0; c < theseConnections.length; c++) {
                if(!thisNode.hasConnection(c)) continue;

                NodeConnection thisConnection = theseConnections[c];

                if(!thisConnection.isValid()) continue;

                Vec3 from = thisConnection.getSourcePos();
                Vec3 to = thisConnection.getDestPos();

                boolean needsConstantUpdates = false;
                if(thisConnection.needsLerped()) {
                    thisConnection.updatePosition(partialTicks);
                    needsConstantUpdates = true;
                }

                renderWire(ebe, thisNode.getLocalPosition(), from, to, thisConnection.getSpoolType(), 
                    thisConnection.getAge(), partialTicks, local, bufferSource, needsConstantUpdates);

                // debug tomfoolery
                if(Minecraft.getInstance().options.renderDebug == true) 
                    drawDebug(c, from, to);
                // TODO check singleton statically
            }
        }
    }

    @Override
    public boolean shouldRender(T ebe, Vec3 cameraPos) {
        return true;
    }

    @Override
    public boolean shouldRenderOffScreen(T ebe) {
        return true;
    }

    private void renderWire(ElectricBlockEntity ebe, Vec3 fromOffset, Vec3 fromPos, Vec3 toPos, WireSpool spoolType, int age,
        float pTicks, PoseStack matrix, MultiBufferSource buffers, boolean needsConstantUpdates) {

        matrix.pushPose();

        matrix.translate(fromOffset.x, fromOffset.y, fromOffset.z);
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(spoolType.asResource()));

        Vector3f offset = getWireOffset(fromPos, toPos);
        matrix.translate(offset.x(), 0, offset.z());

        int[] lights = lightmapGet(ebe.getLevel(), fromPos, toPos);

        Vec3 startPos = fromPos.add(offset.x(), 0, offset.z());
        Vec3 endPos = toPos.add(-offset.x(), 0, -offset.z());

        Vector3f wireOrigin = new Vector3f((float)(endPos.x - startPos.x), (float)(endPos.y - startPos.y), (float)(endPos.z - startPos.z));

        float angleY = -(float)Math.atan2(wireOrigin.z(), wireOrigin.x());
        matrix.mulPose(new Quaternionf().rotateXYZ(0, angleY, 0));


        if(age > -1)
            wireRenderer.renderWiggly(buffer, matrix, wireOrigin, age, pTicks, lights[0], lights[1], lights[2], lights[3]);
        else if(needsConstantUpdates)
            wireRenderer.renderFrequent(buffer, matrix, wireOrigin, lights[0], lights[1], lights[2], lights[3]);
        else {
            if(USE_CACHE) {
                WireModelRenderer.BakedModelHashKey key = new  WireModelRenderer.BakedModelHashKey(fromPos, toPos);
                wireRenderer.renderFromCache(buffer, matrix, key, wireOrigin, lights[0], lights[1], lights[2], lights[3]);
            } else {
                wireRenderer.renderFrequent(buffer, matrix, wireOrigin, lights[0], lights[1], lights[2], lights[3]);
            }
        }
        wireRenderer.setFrom(fromPos);

        matrix.popPose();
    }

    public Vector3f getWireOffset(Vec3 start, Vec3 end) {
        Vector3f offset = end.subtract(start).toVector3f();
        offset.set(offset.x(), 0, offset.z());
        offset.normalize();
        offset.mul(1 / 64f);
        return offset;
    }

    private int[] lightmapGet(Level world, Vec3 from, Vec3 to) {
        return lightmapGet(world, VectorHelper.toBlockPos(from), VectorHelper.toBlockPos(to));
    }

    private int[] lightmapGet(Level world, BlockPos from, BlockPos to) {
        int[] out = new int[4];

        out[0] = world.getBrightness(LightLayer.BLOCK, from);
        out[1] = world.getBrightness(LightLayer.BLOCK, to);
        out[2] = world.getBrightness(LightLayer.SKY, from);
        out[3] = world.getBrightness(LightLayer.SKY, to);

        return out;
    }

    private void drawDebug(int iteration, Vec3 fromPos, Vec3 toPos) {
        if(fromPos != null && toPos != null) {
            CreateClient.OUTLINER.showAABB("wireVF" + iteration + fromPos + toPos, boxFromPos(fromPos))
                .lineWidth(1/32f);
            CreateClient.OUTLINER.showAABB("wireVT" + iteration + fromPos + toPos, boxFromPos(toPos))
                .lineWidth(1/32f);
        }
    }

    private AABB boxFromPos(Vec3 pos) {
        return boxFromPos(pos, 0.1f);
    }

    private AABB boxFromPos(Vec3 pos, float s) {
        Vec3 size = new Vec3(s, s, s);
        return new AABB(pos.subtract(size), pos.add(size));
    }
}
