package com.quattage.mechano.foundation.catenary;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import net.createmod.catnip.theme.Color;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes.CatenaryAttributeHolder;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes.ModelType;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes.Tension;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes.Thickness;
import com.quattage.mechano.foundation.catenary.mesh.WireModel;
import com.quattage.mechano.foundation.helper.VectorHelper;

/**
 * This class is a central place for managing mutable vertex data
 * before it's pushed to a VertexConsumer. This class manages
 * an array of floats to minimize strain on the GC by eliminating
 * boxing/unboxing vectors and continuous re-allocation of hundreds 
 * of floats. 
 * <p>
 * Since it's essentially just a wrapper for all kinds of data related
 * to pushing vertices, geometric operations that use this class
 * can reap the benefits of significantly smaller method headers,
 * such as {@link Extruder} and {@link WireModel#render}
 */
public class CatenaryGeometry extends CatenaryAttributeHolder {

    public static CatenaryGeometry as(TransmitterType<?> type) {
        return new CatenaryGeometry(type);
    }

    private Vec3 basis;
    private MutableBlockPos lightLookup;
    public final int[] light = new int[] { LightTexture.FULL_BRIGHT, LightTexture.FULL_BRIGHT };
    private final float[] data = new float[44];
    private @Nullable BlockAndTintGetter world;
    private RenderType material = RenderType.SOLID;
    private @Nullable TextureAtlasSprite atlas;
    private boolean useTextureAtlas = false;

    private CatenaryGeometry(ModelType model) {
        super(model);
        this.world = null;
        data[41] = 1;
        data[43] = 1;
        this.lightLookup = new MutableBlockPos();
    }

    private CatenaryGeometry(TransmitterType<?> type) {
        super(type.defaults.model);
        this.world = null;
        this.lightLookup = new MutableBlockPos();
        data[41] = 1;
        data[43] = 1;
        withAppearance(type);
    }

    public CatenaryGeometry withAppearance(TransmitterType<?> type) {
        this.material = this.model.getShader(type);
        if(this.material == null)
            throw new IllegalArgumentException("Can't create a CatenaryGeometry builder from transmitter '" + TransmitterRegistry.INSTANCE.getKey(type) + "' - Typs type has no configured material!");
        this.thick = type.defaults.thick;
        if(this.thick.equals(Thickness.ZERO))
            throw new IllegalArgumentException("Can't create a CatenaryGeometry builder from transmitter '" + TransmitterRegistry.INSTANCE.getKey(type) + "' - This type has a thickness of zero!");
        this.data[0] = thick.half();
        data[41] = thick.getPixels();
        this.tension = type.defaults.getTension();
        this.atlas = type.getSprite();
        return this;
    }

    @Override
    public CatenaryGeometry withThickness(Thickness thick) {
        data[0] = thick.get() / 2f;
        data[41] = thick.getPixels();
        return this;
    }

    public CatenaryGeometry in(BlockAndTintGetter world) {
        this.world = world;
        return this;
    } 

    public CatenaryGeometry withPosition(Vec3 pos) {
        this.basis = pos;
        return this;
    }

    @Override
    public CatenaryGeometry withTension(Tension tension) {
        return (CatenaryGeometry)super.withTension(tension);
    }

    public CatenaryGeometry useAtlas() {
        if(atlas == null) {
            Mechano.LOGGER.warn("Attempt to enable CatenaryGeometry texture atlas was ignored since this instance hasn't been configured with an atlas.");
            return this;
        }
        this.useTextureAtlas = true;
        return this;
    }

    public CatenaryGeometry ignoreAtlas() {
        this.useTextureAtlas = false;
        return this;
    }

    /**
     * Renders the geometry in this holder to the {@link RenderType} from the provided
     * {@link TransmitterRegistry Transmitter}. Vertices are automatically
     * submitted to the buffer associated with the supplied transmitter.
     * <p> For rendering to the chunk with BlockAtlas support, see {@link #renderFromAtlas}
     * 
     * @param buffers Buffers to render to. 
     * @param matrixStack matrixStack to use for transformations
     * @param model WireModel to render. This model must be initialized to render properly.
     * @param localOffset (Optional) The local offset of the model relative to <code>matrixStack.last()</code>.
     * @param pTicks Partial ticks, usually accessible from a higher-level rendering context.
     */
    public CatenaryGeometry render(MultiBufferSource buffers, PoseStack matrixStack, WireModel<?> model, Vec3 localOffset, float pTicks) {
        if(localOffset == null) localOffset = new Vec3(0, 0, 0);
        VertexConsumer buffer = buffers.getBuffer(material);
        matrixStack.pushPose();
        matrixStack.translate(localOffset.x, localOffset.y, localOffset.z);
        model.render(buffer, matrixStack.last(), this, pTicks);
        matrixStack.popPose();
        return this;
    }

    /**
     * Renders the geometry in this holder to the {@link RenderType} from the provided
     * {@link TransmitterRegistry Transmitter}. Vertices are automatically
     * submitted to the buffer associated with the supplied transmitter.
     * <p> For rendering to the chunk with BlockAtlas support, see {@link #renderFromAtlas}
     * 
     * @param buffers Buffers to render to. 
     * @param matrixStack matrixStack to use for transformations
     * @param model WireModel to render. This model must be initialized to render properly.
     * @param localOffset (Optional) The local offset of the model relative to <code>matrixStack.last()</code>.
     * @param pTicks Partial ticks, usually accessible from a higher-level rendering context.
     */
    public CatenaryGeometry render(MultiBufferSource buffers, PoseStack matrixStack, WireModel<?> model, float pTicks) {
        return render(buffers, matrixStack, model, null, pTicks);
    }

    public void resetMatrix() {
        data[1]  = 0; data[2]  = 0; data[3]  = 0; // right
        data[4]  = 0; data[5]  = 0; data[6]  = 0; // up
        data[7]  = 0; data[8]  = 0; data[9]  = 0;  // forward
        data[10] = 0; data[11] = 0; data[12] = 0; // normA
        data[13] = 0; data[14] = 0; data[15] = 0; // normB
    }

    public float radius() {
        return data[0];
    }

    public float rightX() {
        return data[1];
    }

    public float rightY() {
        return data[2];
    }

    public float rightZ() {
        return data[3];
    }

    public float upX() {
        return data[4];
    }

    public float upY() {
        return data[5];
    }

    public float upZ() {
        return data[6];
    }

    public float normAX() {
        return data[10];
    }

    public float normAY() {
        return data[11];
    }

    public float normAZ() {
        return data[12];
    }

    public float normBX() {
        return data[13];
    }

    public float normBY() {
        return data[14];
    }

    public float normBZ() {
        return data[15];
    }

    public void resetVerts() {
        data[16] = 0; data[17] = 0; data[18] = 0;
        data[19] = 0; data[20] = 0; data[21] = 0;
        data[22] = 0; data[23] = 0; data[24] = 0;
        data[25] = 0; data[26] = 0; data[27] = 0;
        data[28] = 0; data[29] = 0; data[30] = 0;
        data[31] = 0; data[32] = 0; data[33] = 0;
        data[34] = 0; data[35] = 0; data[36] = 0;
        data[37] = 0; data[38] = 0; data[39] = 0;
    }

    /**
     * Recomputes this extruder's internal matrix by using the average
     * between the two given vectors 
     * @see {@link #computeMatrix(Vector3f)}
     */
    public CatenaryGeometry computeMatrix(Vector3f forwardA, Vector3f forwardB) {
        return computeMatrix(forwardA.add(forwardB, new Vector3f()).normalize());
    }

    /**
     * Recomputes this extruder's internal matrix with the
     * given forward vector. Local up is assumed to be 
     * {@link CatenaryAttributes#UP globally-oriented.}
     * <p>
     * All resulting vectors are normalized automatically,
     * except for the given input vector. You must normalze
     * <code>forward</code> yourself.
     * @param forward
     */
    public CatenaryGeometry computeMatrix(Vector3f forward) {
        data[4] = CatenaryAttributes.UP.x; 
        data[5] = CatenaryAttributes.UP.y; 
        data[6] = CatenaryAttributes.UP.z;
        data[7] = forward.x; 
        data[8] = forward.y; 
        data[9] = forward.z;

        data[1] = Math.fma(data[5], forward.z, -data[6] * forward.y);
        data[2] = Math.fma(data[6], forward.x, -data[4] * forward.z);
        data[3] = Math.fma(data[4], forward.y, -data[5] * forward.x);

        data[4] = Math.fma(data[8], data[3], -data[9] * data[2]);
        data[5] = Math.fma(data[9], data[1], -data[7] * data[3]);
        data[6] = Math.fma(data[7], data[2], -data[8] * data[1]);

        float s = fastinvsqrt(Math.fma(data[1], data[1], Math.fma(data[2], data[2], data[3] * data[3])));
        data[1] *= s;
        data[2] *= s;
        data[3] *= s;

        s = fastinvsqrt(Math.fma(data[4], data[4], Math.fma(data[5], data[5], data[6] * data[6])));
        data[4] *= s;
        data[5] *= s;
        data[6] *= s;

        return this;
    }

    public CatenaryGeometry setNormalA(float x, float y, float z) {
        data[10] = x; data[11] = y; data[12] = z;
        float s = fastinvsqrt(Math.fma(data[10], data[10], Math.fma(data[11], data[11], data[12] * data[12])));
        data[10] *= s;
        data[11] *= s;
        data[12] *= s;
        return this;
    }

    public CatenaryGeometry setNormalB(float x, float y, float z) {
        data[13] = x; data[14] = y; data[15] = z;
        float s = fastinvsqrt(Math.fma(data[13], data[13], Math.fma(data[14], data[14], data[15] * data[15])));
        data[13] *= s;
        data[14] *= s;
        data[15] *= s;
        return this;
    }

    public CatenaryGeometry setNormalA(Vector3f norm) {
        data[10] = norm.x; data[11] = norm.y; data[12] = norm.z;
        float s = fastinvsqrt(Math.fma(data[10], data[10], Math.fma(data[11], data[11], data[12] * data[12])));
        data[10] *= s;
        data[11] *= s;
        data[12] *= s;
        return this;
    }

    public CatenaryGeometry setNormalB(Vector3f norm) {
        data[13] = norm.x; data[14] = norm.y; data[15] = norm.z;
        float s = fastinvsqrt(Math.fma(data[13], data[13], Math.fma(data[14], data[14], data[15] * data[15])));
        data[13] *= s;
        data[14] *= s;
        data[15] *= s;
        return this;
    }

    private float fastinvsqrt(float x) {
        float xh = 0.5f * x;
        int xi = 0x5f3759df - (Float.floatToIntBits(x) >> 1);
        x = Float.intBitsToFloat(xi);
        return x * (1.5f - xh * x * x);
    }

    public CatenaryGeometry setVert(int offset, float x, float y, float z) {
        if(offset >= 8)
            throw new ArrayIndexOutOfBoundsException("Can't get vertex at offset " + offset + " - There are only up to 8 vertices in this CatenaryGeometry!");
        offset = offset * 3 + 16;
        data[offset] = x; data[offset + 1] = y; data[offset + 2] = z;
        return this;
    }

    public float getVertX(int offset) {
        if(offset >= 8)
            throw new ArrayIndexOutOfBoundsException("Can't get vertex at offset " + offset + " - There are only up to 8 vertices in this CatenaryGeometry!");
        offset = offset * 3 + 16;
        return data[offset];
    }

    public float getVertY(int offset) {
        if(offset >= 8)
            throw new ArrayIndexOutOfBoundsException("Can't get vertex at offset " + offset + " - There are only up to 8 vertices in this CatenaryGeometry!");
        offset = offset * 3 + 16;
        return data[offset + 1];
    }

    public float getVertZ(int offset) {
        if(offset >= 8)
            throw new ArrayIndexOutOfBoundsException("Can't get vertex at offset " + offset + " - There are only up to 8 vertices in this CatenaryGeometry!");
        offset = offset * 3 + 16;
        return data[offset + 2];
    }
    
    /**
     * Place 4 vertices at the given position using this extruder's
     * up and right matrix vectors as a basis for its orientation.
     * the 4 resulting vertices create a diamond pattern with 
     * <code>pos</code> at its center.
     * @param pos
     * @param offset The offset to use when storing vertices here.
     */
    public CatenaryGeometry place4Verts(Vector3f pos, int offset) {
        return this.setVert(offset, 
            pos.x + rightX() * radius(),
            pos.y + rightY() * radius(),
            pos.z + rightZ() * radius()
        ).setVert(offset + 1, 
            pos.x + upX() * radius(),
            pos.y + upY() * radius(),
            pos.z + upZ() * radius()
        ).setVert(offset + 2, 
            pos.x - rightX() * radius(),
            pos.y - rightY() * radius(),
            pos.z - rightZ() * radius()
        ).setVert(offset + 3, 
            pos.x - upX() * radius(),
            pos.y - upY() * radius(),
            pos.z - upZ() * radius()
        );
    }

    /**
     * Place 4 vertices at the given position using this extruder's
     * up and right matrix vectors as a basis for its orientation.
     * the 4 resulting vertices create a diamond pattern with 
     * <code>x, y, z</code> at its center.
     * @param x
     * @param y
     * @param z
     * @param offset The offset to use when storing vertices here.
     */
    public CatenaryGeometry place4Verts(float x, float y, float z, int offset) {
        return this.setVert(offset, 
            x + rightX() * radius(),
            y + rightY() * radius(),
            z + rightZ() * radius()
        ).setVert(offset + 1, 
            x + upX() * radius(),
            y + upY() * radius(),
            z + upZ() * radius()
        ).setVert(offset + 2, 
            x - rightX() * radius(),
            y - rightY() * radius(),
            z - rightZ() * radius()
        ).setVert(offset + 3, 
            x - upX() * radius(),
            y - upY() * radius(),
            z - upZ() * radius()
        );
    }


    public CatenaryGeometry emitQuad(VertexConsumer buffer, Pose pose, int offsetA, int offsetB, int offsetC, int offsetD) {
        buffer.addVertex(pose, getVertX(offsetA), getVertY(offsetA), getVertZ(offsetA))
            .setColor(255, 255, 255, 255)
            .setUv(u0(), v0())
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light[0])
            .setNormal(pose, normAX(), normAY(), normAZ());
        buffer.addVertex(pose, getVertX(offsetB), getVertY(offsetB), getVertZ(offsetB))
            .setColor(255, 255, 255, 255)
            .setUv(u0(), v1())
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light[1])
            .setNormal(pose, normAX(), normAY(), normAZ());
        buffer.addVertex(pose, getVertX(offsetC), getVertY(offsetC), getVertZ(offsetC))
            .setColor(255, 255, 255, 255)
            .setUv(u1(), v1())
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light[1])
            .setNormal(pose, normAX(), normAY(), normAZ());
        buffer.addVertex(pose, getVertX(offsetD), getVertY(offsetD), getVertZ(offsetD))
            .setColor(255, 255, 255, 255)
            .setUv(u1(), v0())
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light[0])
            .setNormal(pose, normAX(), normAY(), normAZ());
        return this;
    }


    public CatenaryGeometry emitQuad(VertexConsumer buffer, Pose pose, float normX, float normY, float normZ, int offsetA, int offsetB, int offsetC, int offsetD) {
        buffer.addVertex(pose, getVertX(offsetA), getVertY(offsetA), getVertZ(offsetA))
            .setColor(255, 255, 255, 255)
            .setUv(u0(), v0())
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light[0])
            .setNormal(pose, normX, normY, normZ);
        buffer.addVertex(pose, getVertX(offsetB), getVertY(offsetB), getVertZ(offsetB))
            .setColor(255, 255, 255, 255)
            .setUv(u0(), v1())
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light[1])
            .setNormal(pose, normX, normY, normZ);
        buffer.addVertex(pose, getVertX(offsetC), getVertY(offsetC), getVertZ(offsetC))
            .setColor(255, 255, 255, 255)
            .setUv(u1(), v1())
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light[1])
            .setNormal(pose, normX, normY, normZ);
        buffer.addVertex(pose, getVertX(offsetD), getVertY(offsetD), getVertZ(offsetD))
            .setColor(255, 255, 255, 255)
            .setUv(u1(), v0())
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light[0])
            .setNormal(pose, normX, normY, normZ);
        return this;
    }


    public float u0() {
        return useTextureAtlas && atlas != null
            ? atlas.getU(data[40]) : data[40] / 16f;
    }

    public float u1() {
        return useTextureAtlas && atlas != null
            ? atlas.getU(data[41]) : data[41] / 16f;
    }

    public float v0() {
        return useTextureAtlas && atlas != null
            ? atlas.getU(data[42]) : data[42] / 16f;
    }

    public float v1() {
        return useTextureAtlas && atlas != null
            ? atlas.getU(data[43]) : data[43] / 16f;
    }

    public CatenaryGeometry walkUVs(Stick stick, int iteration) {
        data[42] = (stick.length * 2f) * (float)iteration;
        data[43] = data[42] + (stick.length) * 4;
        if(atlas == null || !useTextureAtlas) return this;
        return this;
    }

    public CatenaryGeometry shiftUVs() {
        data[40] += thick.getPixels();
        data[40] %= Math.min(thick.getPixels() * 2, CatenaryAttributes.TEX_DIMS[0]);
        data[41] += thick.getPixels();
        data[41] %= Math.min(thick.getPixels() * 2, CatenaryAttributes.TEX_DIMS[0]);
        return this;
    }

    public int getLight(Vector3f pos) {
        if(world == null)
            return LightTexture.FULL_BRIGHT;
        lightLookup.setX((int)Math.floor(pos.x + basis.x)); 
        lightLookup.setY((int)Math.floor(pos.y + basis.y)); 
        lightLookup.setZ((int)Math.floor(pos.z + basis.z));
        return LightTexture.pack(world.getBrightness(LightLayer.BLOCK, lightLookup), world.getBrightness(LightLayer.SKY, lightLookup));
    }

    public Vec3 getBasis() {
        return basis;
    }

    public BlockAndTintGetter getWorld() {
        return world;
    }


    /**
     * Represents a singular point in 3D space
     * with a controllable position and velocity.
     * <h2>Important Note:</h2>
     * All coordinates for both Points and Sticks fall within the parent
     * wire's Local frame of reference. This point's position vector
     * does NOT represent a point in the world. For more information, 
     * read the javadoc attached to {@link WireModel}
     */
    public static class Point {

        public Vector3f pos;
        public Vector3f lastPos;
        public boolean pinned;

        public Point(Vector3f pos) {
            setPos(pos);
            this.pinned = false;
        }

        public void clearPos() {
            this.pos = new Vector3f();
            this.lastPos = new Vector3f();
        }

        public void setPos(Vector3f pos) {
            this.pos = new Vector3f(pos);
            this.lastPos = new Vector3f(pos);
        }

        public void pin() {
            this.pinned = true;
            lastPos = new Vector3f(pos);
        }

        public void unpin() {
            this.pinned = false;
        }

        public String toString() {
            return"(" + String.format("%4.3f" , pos.x) + ", " +  String.format("%4.3f" , pos.y) + ", " +  String.format("%4.3f" , pos.z) + ")";
        }

        public void drawDebug(Vec3 basis, int hashIndex) {
            VectorHelper.drawDebugBox(basis.add(pos.x, pos.y, pos.z), 0.05f, Color.BLACK, "point_" + hashIndex);
        }

        public int getLight(Vec3 basis, BlockAndTintGetter world) {
            BlockPos pos = new BlockPos((int)Math.floor(this.pos.x + basis.x), (int)Math.floor(this.pos.y + basis.y), (int)Math.floor(this.pos.z + basis.z));
            return LightTexture.pack(world.getBrightness(LightLayer.BLOCK, pos), world.getBrightness(LightLayer.SKY, pos));
        }
    }

    /**
     * A physical link connecting two {@link Point points}
     * Designed as a way for PBD/particle simulations to
     * apprixmimate the behaviour of chains by representing
     * an arbitrary volume as a length.
     * <h2>Important Note:</h2>
     * All coordinates for both Points and Sticks fall within the parent
     * wire's Local frame of reference. For more information, 
     * read the javadoc attached to {@link WireModel}
     */
    public static class Stick {

        public final Point start, end;
        public final float length;
        public Vector3f facing;
        public Vector3f center;

        public Stick(Point start, Point end) {
            this.start = start;
            this.end = end;
            this.facing = new Vector3f();
            this.center = new Vector3f();
            this.length = start.pos.distance(end.pos);
        }

        public Vector3f computeCenter() {
            center.x = (start.pos.x + end.pos.x) / 2f;
            center.y = (start.pos.y + end.pos.y) / 2f;
            center.z = (start.pos.z + end.pos.z) / 2f;
            return center;
        }

        public Vector3f computeForward() {
            facing.x = (float)(start.pos.x - end.pos.x);
            facing.y = (float)(start.pos.y - end.pos.y);
            facing.z = (float)(start.pos.z - end.pos.z);
            return facing.normalize();
        }

        public Vector3f getForward() {
            return facing;
        }

        public Vector3f start(float pTicks) {
            return start.lastPos.lerp(start.pos, pTicks, new Vector3f());
        }

        public Vector3f end(float pTicks) {
            return end.lastPos.lerp(end.pos, pTicks, new Vector3f());
        }

        public String toString() {
            return "(" + String.format("%.2f", start.pos.x) + ", " + String.format("%.2f", start.pos.y) + ", " + String.format("%.2f", start.pos.z) + "  ->  " + String.format("%.2f", end.pos.x) + ", " + String.format("%.2f", end.pos.y) + ", " + String.format("%.2f", end.pos.z) + ")";
        }
    }

}
