package com.quattage.mechano.foundation.gridapi.catenary;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.gridapi.catenary.CatenaryAttributes.CatenaryAttributeHolder;
import com.quattage.mechano.foundation.gridapi.catenary.CatenaryAttributes.ModelType;
import com.quattage.mechano.foundation.gridapi.catenary.CatenaryAttributes.Thickness;
import com.quattage.mechano.foundation.gridapi.catenary.model.CatenaryModel;
import com.quattage.mechano.foundation.gridapi.transmitter.TransmitterRegistry;
import com.quattage.mechano.foundation.gridapi.transmitter.TransmitterRegistry.TransmitterType;
import com.quattage.mechano.foundation.helper.VectorHelper;

import net.createmod.catnip.theme.Color;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent.SectionRenderingContext;

/**
 * This class is a central place for managing mutable vertex data
 * before it's pushed to a VertexConsumer.  
 * <p>
 * In order to reduce strain on the GC, this class contains
 * a large, reusable array of single-precision floats, and
 * never boxes/unboxes or reallocates these values.
 * To be completely honest, I have no idea whether or 
 * not this results in any reasonable performance uplift
 * compared to the alternative, but it's pretty cool, so yknow.
 * This class will probably be abstracted or replaced when
 * I move over to flywheel and/or make use of compute shaders.
 * <p>
 * Since this is essentially just a wrapper for all kinds of data related
 * to pushing vertices, geometric operations that use this class
 * can reap the benefits of significantly smaller method headers,
 * such as {@link MeshExtruder} and {@link CatenaryModel#render}
 */
public class CatenaryMesher extends CatenaryAttributeHolder {

    /**
     * A common pool for pushing catenary meshes
     */
    public static final CatenaryMesher REUSABLE = CatenaryMesher.asEmpty();

    public static CatenaryMesher as(TransmitterType<?> type) {
        return new CatenaryMesher(type);
    }

    public static CatenaryMesher asEmpty() {
        return new CatenaryMesher();
    }

    private Vec3 basis;
    private MutableBlockPos lightLookup;
    private final float[] data = new float[46];
    private RenderType material = RenderType.SOLID;

    private @Nullable BlockAndTintGetter world;
    private @Nullable TextureAtlasSprite atlas;
    private boolean useTextureAtlas = false;

    private CatenaryMesher(ModelType model) {
        super(model);
        data[41] = 1;
        data[43] = 1;
        data[44] = LightTexture.FULL_BRIGHT;
        data[45] = LightTexture.FULL_BRIGHT;
        lightLookup = new MutableBlockPos();
    }

    private CatenaryMesher(TransmitterType<?> type) {
        super(type.defaults.model);
        lightLookup = new MutableBlockPos();
        data[41] = 1;
        data[43] = 1;
        data[44] = LightTexture.FULL_BRIGHT;
        data[45] = LightTexture.FULL_BRIGHT;
        withAppearance(type);
    }

    private CatenaryMesher() {
        super(null);
        data[41] = 1;
        data[43] = 1;
        data[44] = LightTexture.FULL_BRIGHT;
        data[45] = LightTexture.FULL_BRIGHT;
        lightLookup = new MutableBlockPos();
    }

    /**
     * Inherits all defaulted properties of the given 
     * TransmitterType, including tension, thickness, and material.
     * @param type
     * @return
     */
    public CatenaryMesher withAppearance(TransmitterType<?> type) {
        this.model = type.defaults.model;
        this.material = this.model.getMaterial(type);
        if(this.material == null)
            throw new IllegalArgumentException("Can't create a CatenaryGeometry builder from transmitter '" + TransmitterRegistry.INSTANCE.getKey(type) + "' - This type has no configured material!");
        this.thick = type.defaults.getThickness();
        if(Thickness.ZERO.equals(this.thick))
            throw new IllegalArgumentException("Can't create a CatenaryGeometry builder from transmitter '" + TransmitterRegistry.INSTANCE.getKey(type) + "' - This type has a thickness of zero!");
        this.data[0] = thick.half();
        data[41] = thick.getPixels();
        this.atlas = type.getAtlasSprite();
        return this;
    }

    /**
     * Sets the thickness of the resulting meshes
     * that are drawn by this CatenaryMesher.
     */
    @Override
    public CatenaryMesher withThickness(Thickness thick) {
        data[0] = thick.get() / 2f;
        data[41] = thick.getPixels();
        return this;
    }

    /**
     * Binds this CatenaryMesher to the given world,
     * so that meshes that are drawn can access
     * block collision and light properties.
     * @param world
     * @return
     */
    public CatenaryMesher in(BlockAndTintGetter world) {
        this.world = world;
        return this;
    } 

    /**
     * Sets the global basis of this CatenaryMesher
     * to the given vector. The wire is drawn in its
     * own local frame, then moved to this basis vector
     * once finished.
     * @param pos
     * @return
     */
    public CatenaryMesher at(Vec3 pos) {
        this.basis = pos;
        return this;
    }

    public CatenaryMesher useAtlas() {
        if(atlas == null) {
            Mechano.LOGGER.warn("Attempt to enable CatenaryGeometry texture atlas was ignored since this instance hasn't been configured with an atlas.");
            return this;
        }
        this.useTextureAtlas = true;
        return this;
    }

    public CatenaryMesher ignoreAtlas() {
        this.useTextureAtlas = false;
        return this;
    }

    /**
     * Inherits all defaulted properties of the given 
     * TransmitterType, including tension, thickness, and material.
     * Configures atlas and material settings for compatability with
     * chunk injection.
     * @param type
     * @return
     */
    public CatenaryMesher withAppearanceForChunkRendering(TransmitterType<?> type) {
        this.model = type.defaults.model;
        this.material = this.model.getMaterial(type, true);
        if(this.material == null)
            throw new IllegalArgumentException("Can't create a CatenaryGeometry builder from transmitter '" + TransmitterRegistry.INSTANCE.getKey(type) + "' - This type has no configured material!");
        this.thick = type.defaults.getThickness();
        if(Thickness.ZERO.equals(this.thick))
            throw new IllegalArgumentException("Can't create a CatenaryGeometry builder from transmitter '" + TransmitterRegistry.INSTANCE.getKey(type) + "' - This type has a thickness of zero!");
        this.data[0] = thick.half();
        data[41] = thick.getPixels();
        this.atlas = type.getAtlasSprite();
        useAtlas();
        return this;
    }

    public void reset() {
        basis = new Vec3(0, 0, 0);
        lightLookup.set(0, 0, 0);
        data[44] = LightTexture.FULL_BRIGHT;
        data[45] = LightTexture.FULL_BRIGHT;
        resetMatrix();
        resetVerts();
        resetUVs();
        world = null;
        material = RenderType.SOLID;
        atlas = null;
        useTextureAtlas = false;
    }

    public void resetMatrix() {
        data[1]  = 0; data[2]  = 0; data[3]  = 0; // right
        data[4]  = 0; data[5]  = 0; data[6]  = 0; // up
        data[7]  = 0; data[8]  = 0; data[9]  = 0;  // forward
        data[10] = 0; data[11] = 0; data[12] = 0; // normA
        data[13] = 0; data[14] = 0; data[15] = 0; // normB
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
    public CatenaryMesher render(MultiBufferSource buffers, PoseStack matrixStack, CatenaryModel<?> model, Vec3 localOffset, float pTicks) {
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
    public CatenaryMesher render(SectionRenderingContext ctx, CatenaryModel<?> model, Vec3 localOffset, float pTicks) {
        if(localOffset == null) localOffset = new Vec3(0, 0, 0);
        if(!model.isInitialized()) throw new IllegalStateException("Couldn't render Catenary " + model + " - This model is not initialized!");
        VertexConsumer buffer = ctx.getOrCreateChunkBuffer(material);
        PoseStack matrixStack = ctx.getPoseStack();
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
    public CatenaryMesher render(MultiBufferSource buffers, PoseStack matrixStack, CatenaryModel<?> model, float pTicks) {
        return render(buffers, matrixStack, model, null, pTicks);
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
    public BlockAndTintGetter world() { return world; }
    public Vec3 basis() { return basis; }

    /**
     * Recomputes this extruder's internal matrix by using the average
     * between the two given vectors 
     * @see {@link #computeMatrix(Vector3f)}
     */
    public CatenaryMesher computeMatrix(Vector3f forwardA, Vector3f forwardB) {
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
    public CatenaryMesher computeMatrix(Vector3f forward) {
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

    public CatenaryMesher setNormalA(float x, float y, float z) {
        data[10] = x; data[11] = y; data[12] = z;
        float s = fastinvsqrt(Math.fma(data[10], data[10], Math.fma(data[11], data[11], data[12] * data[12])));
        data[10] *= s;
        data[11] *= s;
        data[12] *= s;
        return this;
    }

    public CatenaryMesher setNormalB(float x, float y, float z) {
        data[13] = x; data[14] = y; data[15] = z;
        float s = fastinvsqrt(Math.fma(data[13], data[13], Math.fma(data[14], data[14], data[15] * data[15])));
        data[13] *= s;
        data[14] *= s;
        data[15] *= s;
        return this;
    }

    public CatenaryMesher setNormalA(Vector3f norm) {
        data[10] = norm.x; data[11] = norm.y; data[12] = norm.z;
        float s = fastinvsqrt(Math.fma(data[10], data[10], Math.fma(data[11], data[11], data[12] * data[12])));
        data[10] *= s;
        data[11] *= s;
        data[12] *= s;
        return this;
    }

    public CatenaryMesher setNormalB(Vector3f norm) {
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

    public CatenaryMesher setVert(int offset, float x, float y, float z) {
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
    public CatenaryMesher place4Verts(Vector3f pos, int offset) {
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
    public CatenaryMesher place4Verts(float x, float y, float z, int offset) {
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


    public CatenaryMesher emitQuad(VertexConsumer buffer, Pose pose, int offsetA, int offsetB, int offsetC, int offsetD) {
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


    public CatenaryMesher emitQuad(VertexConsumer buffer, Pose pose, float normX, float normY, float normZ, int offsetA, int offsetB, int offsetC, int offsetD) {
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
        return useTextureAtlas && atlas != null
            ? atlas.getU(data[40] / 16f) : data[40] / 16f;
    }

    public float u1() {
        return useTextureAtlas && atlas != null
            ? atlas.getU(data[41] / 16f) : data[41] / 16f;
    }

    public float v0() {
        return useTextureAtlas && atlas != null
            ? atlas.getV(data[42] / 16f) : data[42] / 16f;
    }

    public float v1() {
        return useTextureAtlas && atlas != null
            ? atlas.getV(data[43] / 16f) : data[43] / 16f;
    }

    public CatenaryMesher walkUVs(Stick stick, float arclength) {
        data[42] = arclength;
        data[42] %= CatenaryAttributes.TEX_DIMS[1];
        data[43] = data[42] + (stick.length) * 8;
        if(atlas == null || !useTextureAtlas) return this;
        return this;
    }

    public CatenaryMesher shiftUVs() {
        data[40] += thick.getPixels();
        data[40] %= Math.min(thick.getPixels() * 2, CatenaryAttributes.TEX_DIMS[0]);
        data[41] += thick.getPixels();
        data[41] %= Math.min(thick.getPixels() * 2, CatenaryAttributes.TEX_DIMS[0]);
        return this;
    }

    public int getLight(Vector3f pos) {
        if(world == null)
            return LightTexture.FULL_BRIGHT;
        lightLookup.setX((int)Math.round(pos.x + basis.x)); 
        lightLookup.setY((int)Math.round(pos.y + basis.y)); 
        lightLookup.setZ((int)Math.round(pos.z + basis.z));
        int blocklight = world.getBrightness(LightLayer.BLOCK, lightLookup);
        if(CatenaryAttributes.CLAMP_BLOCKLIGHT_SHADOWS)
            blocklight = Mth.clamp(blocklight, 3, 15);
        return LightTexture.pack(blocklight, world.getBrightness(LightLayer.SKY, lightLookup));
    }

    public CatenaryMesher setLight0(int light) {
        data[44] = light;
        return this;
    }

    public CatenaryMesher setLight1(int light) {
        data[45] = light;
        return this;
    }

    public CatenaryMesher walkLight() {
        data[44] = data[45];
        return this;
    }

    /**
     * Represents a singular point in 3D space
     * with a controllable position and velocity.
     * <h2>Important Note:</h2>
     * All coordinates for both Points and Sticks fall within the parent
     * wire's Local frame of reference. This point's position vector
     * does NOT represent a point in the world. For more information, 
     * read the javadoc attached to {@link CatenaryModel}
     */
    public static class Point {

        public Vector3f pos;
        public Vector3f lastPos;
        public boolean pinned;

        public Point(Vector3f pos) {
            this.pos = new Vector3f(pos);
            this.lastPos = new Vector3f(pos);
            this.pinned = false;
        }

        public void clearPos() {
            this.pos.set(0, 0, 0);
            this.lastPos.set(0, 0, 0);
        }

        public void setPos(Vector3f pos) {
            this.lastPos.set(this.pos);
            this.pos.set(pos);
        }

        @Override
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
     * read the javadoc attached to {@link CatenaryModel}
     */
    public static class Stick {

        public final Point start, end;
        public float length;
        public Vector3f facing;
        public Vector3f center;

        public Stick(Point start, Point end) {
            this.start = start;
            this.end = end;
            this.facing = new Vector3f();
            this.center = new Vector3f();
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
            this.length = facing.length() / 2f;
            return facing.normalize();
        }

        public Vector3f getForward() {
            return facing;
        }

        public Vector3f start(float pTicks) {
            if(pTicks < 0) return start.pos;
            return start.lastPos.lerp(start.pos, pTicks, new Vector3f());
        }

        public Vector3f end(float pTicks) {
            if(pTicks < 0) return end.pos;
            return end.lastPos.lerp(end.pos, pTicks, new Vector3f());
        }

        @Override
        public String toString() {
            return "(" + String.format("%.2f", start.pos.x) + ", " + String.format("%.2f", start.pos.y) + ", " + String.format("%.2f", start.pos.z) + "  ->  " + String.format("%.2f", end.pos.x) + ", " + String.format("%.2f", end.pos.y) + ", " + String.format("%.2f", end.pos.z) + ")";
        }
    }

}
