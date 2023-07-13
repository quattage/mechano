package com.quattage.mechano.core.blockEntity.render;

import java.util.function.Consumer;

import com.jozufozu.flywheel.core.PartialModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.core.blockEntity.ElectricBlockEntity;
import com.quattage.mechano.core.electricity.node.NodeBank;
import com.quattage.mechano.core.electricity.node.base.ElectricNode;
import com.quattage.mechano.core.electricity.node.base.NodeConnection;
import com.quattage.mechano.registry.MechanoPartials;
import com.quattage.mechano.registry.MechanoRenderTypes;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.Color;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.client.renderer.entity.LeashKnotRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ElectricBlockRenderer<T extends ElectricBlockEntity> extends SafeBlockEntityRenderer<T> {

    public ElectricBlockRenderer(BlockEntityRendererProvider.Context context) {
        super();
    }

    private final int lod = 14;
    private final int max = 24;

    @Override
    protected void renderSafe(ElectricBlockEntity blockEntity, float partialTicks, PoseStack local, 
        MultiBufferSource bufferSource, int light, int overlay) {
        BlockState state = blockEntity.getBlockState();

        VertexConsumer builder = bufferSource.getBuffer(MechanoRenderTypes.WIRE_ALL);
        renderWires(blockEntity, local, builder, light, partialTicks, state);

        //Mechano.log("tick");
    }

    private void renderWires(ElectricBlockEntity ebe, PoseStack local, VertexConsumer builder, 
        int light, float pTicks, BlockState state) {

        ElectricNode[] nodes = ebe.nodes.values();

        for(int n = 0; n < nodes.length; n++) {
            ElectricNode thisNode = nodes[n];
            NodeConnection[] theseConnections = thisNode.getConnections();

            for(int c = 0; c < theseConnections.length; c++) {
                if(!thisNode.hasConnection(c)) continue;
                Vec3 fromPos = thisNode.getPosition();
                NodeBank other = NodeBank.retrieveFrom(ebe.getLevel(), ebe, theseConnections[c]
                        .getRelativePos());
                if(other == null) continue;

                Vec3 toPos = other.get(theseConnections[c].getDestinationId()).getPosition();
                translateStack(local, fromPos, toPos);
                drawWire(c, fromPos, toPos, local, builder, ebe);

                // debug tomfoolery
                if(Minecraft.getInstance().options.renderDebug == true) 
                    drawDebug(c, fromPos, toPos);
            }
        }
    }

    private void drawWire(int node, Vec3 fromPos, Vec3 toPos, PoseStack local, 
        VertexConsumer builder, ElectricBlockEntity ebe) {
        
        local.pushPose();

        Matrix4f matrix = local.last().pose();
        Vector3f combinedVec = new Vector3f(
            (float)(toPos.x - fromPos.x), 
            (float)(toPos.y - fromPos.y), 
            (float)(toPos.z - fromPos.z)
        );

        float angleY = -(float) Math.atan2(combinedVec.z(), combinedVec.x());
        float distance = (float)fromPos.distanceTo(toPos);
        float magnitude = getGreaterMagnitude(fromPos, toPos);

        Vector3f offset = getOffset(fromPos, toPos);
        matrix.translate(new Vector3f(offset.x(), 0f, offset.z()));
        matrix.multiply(new Quaternion(0f, angleY, 0f, true));
        

        // Modify the level of detail depending on the physical seperation between positions.
        // Longer connections get more detail, and shorter ones get less. To be perfectly 
        // honest these numbers are entirely arbitrary and I have no idea why they work.
        int lodMax = (Math.round(lod * 
            (distance * (0.1161f - (0.00376f * distance)))));

        for(int iter = 0; iter < lodMax; iter++) {
            double step = (double)iter / (lodMax - 1);

            Vec3 target = stepThroughArc(fromPos, toPos, distance, magnitude, step);
            int targetLight = lightmapPack(step, lightmapGet(ebe.getLevel(), fromPos, toPos));

            addVertices(builder, matrix, target, targetLight);
        }

        local.popPose();
    }

    private Vector3f getOffset(Vec3 from, Vec3 to) {
        Vector3f offset = new Vector3f(to.subtract(from));
        offset.set(offset.x(), 0, offset.z());
        offset.normalize();
        return offset;
    }

    /***
     * Gets the in-world location along a generated arc derived from a start and end position.
     * This is essentially just a lerp function, interpolating between two Vec3s while adding 
     * a small offset in the -Y direction to generate an arc shape.
     * 
     * @param from Vector to start at.
     * @param to Vector to end at.
     * @param distance The distance between these two points. 
     * @param magnitude The magnitude of the starting vector.
     * @param step Iterative step value from 0 to 1.
     * @return
     */
    private Vec3 stepThroughArc(Vec3 from, Vec3 to, float distance, float magnitude, double step) {

        if(step < 0.0001) return from;
        if(step > 0.999) return to;

        double yVariation = Mth.lerp(step, from.y, to.y);
        double arcComponent = getArcOffsetAt(step, distance, magnitude);

        return new Vec3(
            Mth.lerp(step, from.x, to.x),
            yVariation + arcComponent,
            Mth.lerp(step, from.z, to.z)
        );
    }

    /***
     * Gets the arc offset at the given step iteration.
     * @param step "Progress" through this line, from 0 to 1
     * @param seperation Distance between the two points. Used to modify the intensity of the arc.
     * @param magnitude The vector magnitude (distance from 0, 0, 0) of the starting position of the arc.
     * @return Float value representing the arc offset at this step
     */
    private float getArcOffsetAt(double step, float seperation, float magnitude) {
		return (float) Math.sin((float)Math.PI * -step) * ((seperation * 0.02f) * magnitude / (float)max);
	}


    /***
     * Gets the magnitude of two vectors and returns the higher one.
     * @param vec1 
     * @param vec2
     * @return The greater magnitude between vec1 and vec2
     */
    private float getGreaterMagnitude(Vec3 vec1, Vec3 vec2) {   
        float m1 = getMagnitude(vec1);
        float m2 = getMagnitude(vec2);
        return m1 > m2 ? m1 : m2;
    }

    private void translateStack(PoseStack local, Vec3 from, Vec3 to) {
        BlockPos bFrom = new BlockPos(from);
        BlockPos bTo = new BlockPos(to);
        local.translate(
            (bFrom.getX() - bTo.getX()) + 0.5f, 
            (bFrom.getY() - bTo.getY()) + 0.5f, 
            (bFrom.getZ() - bTo.getZ()) + 0.5f
        );
    }

    /***
     * Gets the magnitude of the given vector.
     * @param vec Vector to use.
     * @return The magnitude (distance from 0, 0, 0) of this Vector.
     */
	private float getMagnitude(Vec3 vec) {
		return (float) Math.sqrt(Math.pow(vec.x, 2) + Math.pow(vec.y, 2) + Math.pow(vec.z, 2));
	}

    private void addVertices(VertexConsumer buffer, Matrix4f matrix, Vec3 pos, int light) {
        buffer
            .vertex(matrix, (float)pos.x, (float)pos.y, (float)pos.z)
            .color(255, 255, 255, 255)
            .uv2(light)
            .endVertex();

        CreateClient.OUTLINER.showAABB("wireIter" + pos, boxFromPos(pos, 0.05f))
            .colored(new Color(120, 255, 0).mixWith(new Color(0, 120, 255), (float)light * 0.000001f))
            .disableLineNormals()
            .lineWidth(1/16f);
    }

    private int[] lightmapGet(Level world, Vec3 from, Vec3 to) {
        return lightmapGet(world, new BlockPos(from), new BlockPos(to));
    }

    private int[] lightmapGet(Level world, BlockPos from, BlockPos to) {
        int[] out = new int[4];

        out[0] = world.getBrightness(LightLayer.BLOCK, from);
        out[1] = world.getBrightness(LightLayer.BLOCK, to);
        out[2] = world.getBrightness(LightLayer.SKY, from);
        out[3] = world.getBrightness(LightLayer.SKY, to);

        return out;
    }

    private int lightmapPack(double step, int[] get) {
        int block = (int)Mth.lerp(step, (float)get[0], (float)get[1]);
        int sky = (int)Mth.lerp(step, (float)get[2], (float)get[3]);
        return LightTexture.pack(block, sky);
    }



    private void drawDebug(int iteration, Vec3 fromPos, Vec3 toPos) {
        if(fromPos != null && toPos != null) {
            CreateClient.OUTLINER.showAABB("wireVF" + iteration + fromPos + toPos, boxFromPos(fromPos))
                .lineWidth(1/32f);
            CreateClient.OUTLINER.showAABB("wireVT" + iteration + fromPos + toPos, boxFromPos(toPos))
                .lineWidth(1/32f);

            CreateClient.OUTLINER.showLine("N" + iteration + fromPos + toPos, fromPos, toPos)
                .colored(new Color(255, 120, 255))
                .lineWidth(1/16f);
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
