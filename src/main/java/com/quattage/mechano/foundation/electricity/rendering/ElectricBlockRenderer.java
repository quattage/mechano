package com.quattage.mechano.foundation.electricity.rendering;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.item.spool.WireSpool;
import com.quattage.mechano.foundation.electricity.WireNodeBlockEntity;
import com.quattage.mechano.foundation.electricity.core.connection.NodeConnection;
import com.quattage.mechano.foundation.electricity.core.node.ElectricNode;
import com.quattage.mechano.foundation.electricity.system.SystemNode;
import com.quattage.mechano.foundation.electricity.system.TransferSystem;
import com.quattage.mechano.foundation.helper.VectorHelper;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.utility.Pair;

import static com.quattage.mechano.foundation.electricity.system.GlobalTransferNetwork.NETWORK;

import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ElectricBlockRenderer<T extends WireNodeBlockEntity> extends SafeBlockEntityRenderer<T> {

    private final WireModelRenderer wireRenderer = new WireModelRenderer();
    private static final boolean USE_CACHE = true;

    public ElectricBlockRenderer(BlockEntityRendererProvider.Context context) {
        super();
    }   

    @Override
    protected void renderSafe(WireNodeBlockEntity ebe, float partialTicks, PoseStack matrix, 
        MultiBufferSource buffers, int light, int overlay) {
    
        // debug tomfoolery
        Minecraft mc = Minecraft.getInstance();
        if(mc.options.renderDebug == true) 
            drawDebug(ebe, mc, matrix, buffers, light);

        ElectricNode[] nodes = ebe.nodeBank.values();

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
                    thisConnection.getAge(), partialTicks, matrix, buffers, light, needsConstantUpdates);
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

    private void renderWire(WireNodeBlockEntity ebe, Vec3 fromOffset, Vec3 fromPos, Vec3 toPos, WireSpool spoolType, int age,
        float pTicks, PoseStack matrix, MultiBufferSource buffers, int light,boolean needsConstantUpdates) {

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

    private void drawDebug(WireNodeBlockEntity ebe, Minecraft mc, PoseStack matrix, MultiBufferSource buffer, int light) {
        Font fontRenderer = mc.font;
		Quaternionf cameraRotation = mc.getEntityRenderDispatcher().cameraOrientation();

        Pair<TransferSystem, SystemNode> approx = NETWORK.getSystemAndNode(ebe.nodeBank.approximate());
        String lineA = "[NO NET]";
        ArrayList<String> connLines = new ArrayList<>();
        connLines.add("[ NONE ]");

        if(approx != null) {
            int id = NETWORK.getSubsystemID(approx.getFirst());
            lineA = "NET: [" + (id + 1) + " / " + NETWORK.getSubsystemCount() + "]";
            connLines.set(0, "");

            int x = 1;
            for(SystemNode node : approx.getFirst().all()) {
                if(node == null || node.getPos() == null)
                    connLines.add(x  + ": [NULL]");
                else
                    if(node.getPos() == ebe.getBlockPos())
                        connLines.add("● " + x + ": [" + node.getPos().getX() + ", " + node.getPos().getY() + ", " + node.getPos().getZ() + "]"); 
                    else
                        connLines.add("  " + x + ": [" + node.getPos().getX() + ", " + node.getPos().getY() + ", " + node.getPos().getZ() + "]"); 
                x++;
            }
        }

		matrix.pushPose(); 
		
		Matrix4f matrix4f = matrix.last().pose();
		float textOffset = -fontRenderer.width(lineA) / 2;
	
        matrix.translate(0.5F, 2F, 0.5F);
		matrix.mulPose(cameraRotation);
		matrix.scale(-0.025F, -0.025F, 0.025F);
		fontRenderer.drawInBatch(lineA, textOffset, 0f, -1, false, matrix4f, buffer, DisplayMode.SEE_THROUGH, 0, light, false);

        for(String connLine : connLines) {
            matrix.translate(0f, -8f, 0f);
            fontRenderer.drawInBatch(connLine, textOffset, 0f, -1, false, matrix4f, buffer, DisplayMode.SEE_THROUGH, 0, light, false);
        }
        
		
		matrix.popPose();
    }

    private AABB boxFromPos(Vec3 pos) {
        return boxFromPos(pos, 0.1f);
    }

    private AABB boxFromPos(Vec3 pos, float s) {
        Vec3 size = new Vec3(s, s, s);
        return new AABB(pos.subtract(size), pos.add(size));
    }
}
