package com.quattage.mechano.api.catenary;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoTransmitters;
import com.quattage.mechano.api.catenary.Catenaries.RenderPipeline.Thickness;
import com.quattage.mechano.api.catenary.Catenaries.Stick;
import com.quattage.mechano.api.catenary.model.CatenaryModel;
import com.quattage.mechano.api.transmitter.TransmitterType;
import com.quattage.mechano.foundation.WorldlyObject;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent.SectionRenderingContext;

/**
 * This class wraps an array of floats for storing mutable 
 * vertex data before being pushed to a VertexConsumer.  
 * <p>
 * In order to reduce strain on the GC, this class contains
 * a large, reusable array of single-precision floats, and
 * never boxes/unboxes or reallocates these values.
 * To be completely honest, I have no idea whether or 
 * not this results in any reasonable performance uplift
 * compared to the alternative, but it's pretty cool, so yknow.
 * This class will probably be abstracted or replaced when
 * I move over to flywheel and/or make use of compute shaders.
 */
public class CatenaryMeshBuffer implements WorldlyObject {

    public static final CatenaryMeshBuffer REUSABLE = CatenaryMeshBuffer.asEmpty();
    private static final float RAD = 0.707107f;

    private @Nullable TransmitterType<?> trns = MechanoTransmitters.HOOKUP.get();
    private @Nullable Vec3 basis;
    private @NotNull MutableBlockPos lightLookup;
    private @Nullable BlockAndTintGetter world;
    private boolean useTextureAtlas = false;
    private final float[] data = new float[46];


    public static CatenaryMeshBuffer as(TransmitterType<?> type) {
        CatenaryMeshBuffer output = new CatenaryMeshBuffer();
        return output.bindTo(type);
    }

    public static CatenaryMeshBuffer asEmpty() {
        return new CatenaryMeshBuffer();
    }

    private CatenaryMeshBuffer() {
        data[0] = 0.009375f; // 3/32
        data[41] = 1;
        data[43] = 1;
        data[44] = LightTexture.FULL_BRIGHT;
        data[45] = LightTexture.FULL_BRIGHT;
        lightLookup = new MutableBlockPos();
    }

    public CatenaryMeshBuffer bindTo(TransmitterType<?> trns) {
        Objects.requireNonNull(trns);
        if(!trns.getRenderProperties().isVisible()) {
            Mechano.LOGGER.warn("Cannot bind CatenaryMesher to TransmitterType '" + trns + "' - This type is not renderable!");
            return this;
        }
        this.trns = trns;
        Thickness t = trns.getRenderProperties().getThickness();
        this.data[0] = t.half();
        this.data[41] = t.pixels();
        return this;
    }

    /**
     * Binds this CatenaryMeshBuffer to the given world,
     * so that meshes that are drawn can access
     * block collision and light properties.
     * @param world
     * @return
     */
    public CatenaryMeshBuffer in(BlockAndTintGetter world) {
        Objects.requireNonNull(world);
        this.world = world;
        return this;
    } 

    /**
     * Sets the global basis of this CatenaryMeshBuffer
     * to the given vector. The wire is drawn in its
     * own local frame, then moved to this basis vector
     * once finished.
     * @param pos
     * @return
     */
    public CatenaryMeshBuffer at(Vec3 pos) {
        this.basis = pos;
        return this;
    }

    /** 
     * Tells this CatenaryMeshBuffer that all UV math should be done
     * in atlas-space rather than local texture-space.
     * @see {@link #useLocalUVs}
     */
    public CatenaryMeshBuffer useAtlasUVs() {
        this.useTextureAtlas = true;
        return this;
    }

    /**
     * Tells this CatenaryMeshBuffer to use traditional UV coodinates
     * rather than ones that contain an atlas sprite's offset.
     * @see {@link #useAtlasUVs}
     */
    public CatenaryMeshBuffer useLocalUVs() {
        this.useTextureAtlas = false;
        return this;
    }

    public void reset() {
        basis = null;
        lightLookup.set(0, 0, 0);
        world = null;
        data[0] = 0.009375f; // 3/32
        resetMatrix();
        resetVerts();
        resetUVs();
        data[44] = LightTexture.FULL_BRIGHT;
        data[45] = LightTexture.FULL_BRIGHT;
        useTextureAtlas = false;
    }

    public void resetMatrix() {
        data[1]  = 0; data[2]  = 0; data[3]  = 0;  // right
        data[4]  = 0; data[5]  = 0; data[6]  = 0;  // up
        data[7]  = 0; data[8]  = 0; data[9]  = 0;  // forward
        data[10] = 0; data[11] = 0; data[12] = 0;  // normA
        data[13] = 0; data[14] = 0; data[15] = 0;  // normB
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

    public void resetUVs() {
        data[40] = 0;
        data[41] = 1;
        data[42] = 0;
        data[43] = 1;
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
    public CatenaryMeshBuffer render(MultiBufferSource buffers, PoseStack matrixStack, CatenaryModel<?> model, @Nullable Vec3 localOffset, float pTicks) {
        if(!model.isInitialized()) throw new IllegalStateException("Couldn't render Catenary " + model + " - This model is not initialized!");
        VertexConsumer buffer = buffers.getBuffer(trns.getRenderProperties().getMaterialForDynamicMeshing());
        matrixStack.pushPose();
        if(localOffset != null) matrixStack.translate(localOffset.x, localOffset.y, localOffset.z);
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
    public CatenaryMeshBuffer render(SectionRenderingContext ctx, CatenaryModel<?> model, @Nullable Vec3 localOffset, float pTicks) {
        if(!model.isInitialized()) throw new IllegalStateException("Couldn't render Catenary " + model + " - This model is not initialized!");
        VertexConsumer buffer = ctx.getOrCreateChunkBuffer(trns.getRenderProperties().getMaterialForSectionMeshing());
        PoseStack matrixStack = ctx.getPoseStack();
        matrixStack.pushPose();
        if(localOffset != null) matrixStack.translate(localOffset.x, localOffset.y, localOffset.z);
        model.render(buffer, matrixStack.last(), this, pTicks);
        matrixStack.popPose();
        return this;
    }

    public float radius() { return data[0]; }
    public float rightX() { return data[1]; }
    public float rightY() { return data[2]; }
    public float rightZ() { return data[3]; }
    public float upX() { return data[4]; }
    public float upY() { return data[5]; }
    public float upZ() { return data[6]; }
    public float normAX() { return data[10]; }
    public float normAY() { return data[11]; }
    public float normAZ() { return data[12]; }
    public float normBX() { return data[13]; }
    public float normBY() { return data[14]; }
    public float normBZ() { return data[15]; }
    public int light0() { return (int)data[44]; }
    public int light1() { return (int)data[45]; }
    public Vec3 basis() { return basis; }

    /**
     * Recomputes this extruder's internal matrix by using the average
     * between the two given vectors 
     * @see {@link #computeMatrix(Vector3f)}
     */
    public CatenaryMeshBuffer computeMatrix(Vector3f forwardA, Vector3f forwardB) {
        return computeMatrix(forwardA.add(forwardB, new Vector3f()).normalize());
    }

    /**
     * Recomputes this extruder's internal matrix with the
     * given forward vector. Local up is assumed to be 
     * {@link Catenaries#UP globally-oriented.}
     * <p>
     * All resulting vectors are normalized automatically,
     * except for the given input vector. You must normalze
     * <code>forward</code> yourself.
     * @param forward
     */
    public CatenaryMeshBuffer computeMatrix(Vector3f forward) {
        data[4] = Catenaries.renderPipeline().SIM_UP.x; 
        data[5] = Catenaries.renderPipeline().SIM_UP.y;
        data[6] = Catenaries.renderPipeline().SIM_UP.z;
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


    /**
     * Rotates the matrix along its local normal direction so that
     * the tangent is offset by 45 degrees. This efectively
     * converts the diamond shape implied by the opposed vectors into a square one.
     * @return this mesher, for chaining
     */
    public CatenaryMeshBuffer shiftMatrix() {
        float rx = data[1];
        float ry = data[2];
        float rz = data[3];
        float ux = data[4];
        float uy = data[5];
        float uz = data[6];
        data[1] = (data[1] + data[4]) * CatenaryMeshBuffer.RAD;
        data[2] = (data[2] + data[5]) * CatenaryMeshBuffer.RAD;
        data[3] = (data[3] + data[6]) * CatenaryMeshBuffer.RAD;
        data[4] = (-rx + ux) * CatenaryMeshBuffer.RAD;
        data[5] = (-ry + uy) * CatenaryMeshBuffer.RAD;
        data[6] = (-rz + uz) * CatenaryMeshBuffer.RAD;
        return this;
    }

    public CatenaryMeshBuffer setNormalA(Vector3f norm) { return setNormalA(norm.x, norm.y, norm.z); }
    public CatenaryMeshBuffer setNormalA(float x, float y, float z) {
        data[10] = x; data[11] = y; data[12] = z;
        float s = fastinvsqrt(Math.fma(data[10], data[10], Math.fma(data[11], data[11], data[12] * data[12])));
        data[10] *= s;
        data[11] *= s;
        data[12] *= s;
        return this;
    }

    public CatenaryMeshBuffer setNormalB(Vector3f norm) { return setNormalB(norm.x, norm.y, norm.z); }
    public CatenaryMeshBuffer setNormalB(float x, float y, float z) {
        data[13] = x; data[14] = y; data[15] = z;
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

    public CatenaryMeshBuffer setVert(int offset, float x, float y, float z) {
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
    public CatenaryMeshBuffer place4Verts(Vector3f pos, int offset) {
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
    public CatenaryMeshBuffer place4Verts(float x, float y, float z, int offset) {
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


    public CatenaryMeshBuffer emitQuad(VertexConsumer buffer, Pose pose, int offsetA, int offsetB, int offsetC, int offsetD) {
        buffer.addVertex(pose, getVertX(offsetA), getVertY(offsetA), getVertZ(offsetA))
            .setColor(255, 255, 255, 255)
            .setUv(u0(), v0())
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight((int)data[44])
            .setNormal(pose, normAX(), normAY(), normAZ());
        buffer.addVertex(pose, getVertX(offsetB), getVertY(offsetB), getVertZ(offsetB))
            .setColor(255, 255, 255, 255)
            .setUv(u0(), v1())
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight((int)data[45])
            .setNormal(pose, normAX(), normAY(), normAZ());
        buffer.addVertex(pose, getVertX(offsetC), getVertY(offsetC), getVertZ(offsetC))
            .setColor(255, 255, 255, 255)
            .setUv(u1(), v1())
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight((int)data[45])
            .setNormal(pose, normAX(), normAY(), normAZ());
        buffer.addVertex(pose, getVertX(offsetD), getVertY(offsetD), getVertZ(offsetD))
            .setColor(255, 255, 255, 255)
            .setUv(u1(), v0())
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight((int)data[44])
            .setNormal(pose, normAX(), normAY(), normAZ());
        return this;
    }


    public CatenaryMeshBuffer emitQuad(VertexConsumer buffer, Pose pose, float normX, float normY, float normZ, int offsetA, int offsetB, int offsetC, int offsetD) {
        buffer.addVertex(pose, getVertX(offsetA), getVertY(offsetA), getVertZ(offsetA))
            .setColor(255, 255, 255, 255)
            .setUv(u0(), v0())
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight((int)data[44])
            .setNormal(pose, normX, normY, normZ);
        buffer.addVertex(pose, getVertX(offsetB), getVertY(offsetB), getVertZ(offsetB))
            .setColor(255, 255, 255, 255)
            .setUv(u0(), v1())
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight((int)data[45])
            .setNormal(pose, normX, normY, normZ);
        buffer.addVertex(pose, getVertX(offsetC), getVertY(offsetC), getVertZ(offsetC))
            .setColor(255, 255, 255, 255)
            .setUv(u1(), v1())
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight((int)data[45])
            .setNormal(pose, normX, normY, normZ);
        buffer.addVertex(pose, getVertX(offsetD), getVertY(offsetD), getVertZ(offsetD))
            .setColor(255, 255, 255, 255)
            .setUv(u1(), v0())
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight((int)data[44])
            .setNormal(pose, normX, normY, normZ);
        return this;
    }

    public float u0() {
        if(!useTextureAtlas) return data[40] / 16f;
        TextureAtlasSprite sprite = trns.getRenderProperties().getAtlasSprite();
        if(sprite == null) return data[40] / 16f;
        return sprite.getU(data[40] / 16f);
    }

    public float u1() {
        if(!useTextureAtlas) return data[41] / 16f;
        TextureAtlasSprite sprite = trns.getRenderProperties().getAtlasSprite();
        if(sprite == null) return data[41] / 16f;
        return sprite.getU(data[41] / 16f);
    }

    public float v0() {
        if(!useTextureAtlas) return data[42] / 16f;
        TextureAtlasSprite sprite = trns.getRenderProperties().getAtlasSprite();
        if(sprite == null) return data[42] / 16f;
        return sprite.getV(data[42] / 16f);
    }

    public float v1() {
        if(!useTextureAtlas) return data[43] / 16f;
        TextureAtlasSprite sprite = trns.getRenderProperties().getAtlasSprite();
        if(sprite == null) return data[43] / 16f;
        return sprite.getV(data[43] / 16f);
    }

    public CatenaryMeshBuffer walkUVs(Stick stick, float arclength) {
        data[42] += arclength;
        data[43] = data[42] + stick.getLength() * 8f;
        if (data[43] > Catenaries.renderPipeline().TEX_DIMS[1]) {
            data[42] = 0;
            data[43] = arclength;
        }
        return this;
    }

    public CatenaryMeshBuffer shiftUVs() {
        int pix = trns.getRenderProperties().getThickness().pixels();
        int mod = Math.min(pix * 2, Catenaries.renderPipeline().TEX_DIMS[0]);
        data[40] += pix;
        data[40] %= mod;
        data[41] += pix;
        data[41] %= mod;
        return this;
    }

    public CatenaryMeshBuffer unshiftUVs() {
        int pix = trns.getRenderProperties().getThickness().pixels();
        data[40] -= pix;
        data[41] -= pix;
        if(data[40] < 0 || data[41] < 0) {
            data[40] = 0;
            data[41] = pix;
        }
        return this;
    }

    public int getLight(Vector3f pos) {
        if(world == null) return LightTexture.FULL_BRIGHT;
        lightLookup.setX((int)Math.round(pos.x + basis.x)); 
        lightLookup.setY((int)Math.round(pos.y + basis.y)); 
        lightLookup.setZ((int)Math.round(pos.z + basis.z));
        int blocklight = world.getBrightness(LightLayer.BLOCK, lightLookup);
        if(Catenaries.renderPipeline().shouldApplyShadowClamping())
            blocklight = Mth.clamp(blocklight, 3, 15);
        return LightTexture.pack(blocklight, world.getBrightness(LightLayer.SKY, lightLookup));
    }

    public CatenaryMeshBuffer setLight0(int light) {
        data[44] = light;
        return this;
    }

    public CatenaryMeshBuffer setLight1(int light) {
        data[45] = light;
        return this;
    }

    public CatenaryMeshBuffer walkLight() {
        data[44] = data[45];
        return this;
    }

    public TransmitterType<?> getCurrentlyBoundType() {
        return trns;
    }

    @Override
    public @Nullable Level getWorld() {
        return world instanceof Level cast ? cast : null;
    }
}
