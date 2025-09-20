

package com.quattage.mechano.foundation.api.catenary;

import java.util.Locale;
import java.util.function.BiFunction;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.transmitter.MechanoTransmissionTypes;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;
import com.quattage.mechano.foundation.helper.VectorHelper;

import net.createmod.catnip.theme.Color;
import net.minecraft.Util;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.data.models.blockstates.PropertyDispatch.QuadFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

/**
 * A container class for various catenary-related things
 */
public class CatenaryAttributes {

    public static final boolean TEX_USE_MIPS = false;
    public static final int[] TEX_DIMS = new int[] { 16, 32 };

    public static final Vector3f UP = new Vector3f(0, 1, 0);
    public static final float POINT_MASS = 3f;
    public static final float TENSION_EPSILON = 1e-3f;
    public static final SimulationFeatureset FEATURESET = SimulationFeatureset.DISPLACED_CORRECTED_HIGH;
    public static final int SOLVER_STEPS = 64;
    public static final float DETACH_THRESHOLD = 0.6f;
    public static final float RESTITUTION_VELOCITY = 0.1f;
    

    public static final int DRAW_MIN = 5;
    public static final int DRAW_MAX = 32;

    // TODO switch to custom shader using more optimized vertex format
    public static final BiFunction<TransmitterType<?>, Boolean, RenderType> SOLID_MATERIAL 
        = Util.memoize((trns, chunk) -> {
            if(chunk) return RenderType.SOLID;
            RenderType.CompositeState composite = RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_SOLID_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(trns.getTextureLocation(), false, TEX_USE_MIPS))
                .setOverlayState(RenderType.OVERLAY)
                .setLightmapState(RenderType.LIGHTMAP)
                .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                .setOutputState(RenderType.MAIN_TARGET)
                .setCullState(RenderType.CULL)
                .createCompositeState(false); 
            return RenderType.create("catenary_solid", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, DRAW_MAX * 8, true, false, composite);
        });

    public static final BiFunction<TransmitterType<?>, Boolean, RenderType> CUTOUT_MATERIAL 
        = Util.memoize((trns, chunk) -> {
            if(chunk) return RenderType.CUTOUT;
            RenderType.CompositeState composite = RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_CUTOUT_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(trns.getTextureLocation(), false, TEX_USE_MIPS))
                .setOverlayState(RenderType.OVERLAY)
                .setLightmapState(RenderType.LIGHTMAP)
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setOutputState(RenderType.TRANSLUCENT_TARGET)
                .setCullState(RenderType.CULL)
                .createCompositeState(false);
            return RenderType.create("catenary_cutout", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, DRAW_MAX * 8, true, false, composite);
        });

    public static enum Initializer {
        FRESH_SIMULATION((world, start, end, trns) -> {
            CatenaryModel<?> output = new SimulatedCatenary();
            output.maxLength = trns.getMaximumSpan();
            output.setOrderedOffset(world, start.getAddress(), end.getAddress(), 1)
                .initializeSpan()
                .calculateSegmentation()
                .pinEndpoints();
            output.update();
            return output;
        }),
        FRESH_SIMULATION_EXPRESSIVE((world, start, end, trns) -> {
            SimulatedCatenary output = new SimulatedCatenary();
            output.maxLength = trns.getMaximumSpan();
            output.setOrderedOffset(world, start.getAddress(), end.getAddress(), 1)
                .initializeSpan()
                .calculateSegmentation()
                .pinEndpoints();
            output.update();
            output.kick(0.35f);
            return output;
        }),
        RESTING_SIMULATION((world, start, end, trns) -> {
            CatenaryModel<?> output = new SimulatedCatenary();
            output.maxLength = trns.getMaximumSpan();
            output.setOrderedOffset(world, start.getAddress(), end.getAddress(), 1)
                .initializeSpan()
                .calculateSegmentation()
                .pinEndpoints();
            output.updateAhead(512);
            return output;
        });


        private final QuadFunction<LevelReader, AnchorPoint, AnchorPoint, TransmitterType<?>, CatenaryModel<?>> func;
        private Initializer(QuadFunction<LevelReader, AnchorPoint, AnchorPoint, TransmitterType<?>, CatenaryModel<?>> func) { 
            this.func = func; 
        }

        /**
         * Executes a simple set of method calls to create a catenary with the desired precomputed
         * characteristics. 
         * @param world
         * @param start
         * @param end
         * @param trns
         * @return
         */
        public CatenaryModel<?> make(LevelReader world, AnchorPoint start, AnchorPoint end, TransmitterType<?> trns) { 
            return func.apply(world, start, end, trns); 
        }
    }

    public static enum ModelType {

        SQUARE(SOLID_MATERIAL, (VertexConsumer buffer, Pose pose, CatenaryMesher geo, @Nullable Stick previous, Stick current, @Nullable Stick next, 
            float loftLength, boolean recomputeNormals, float pTicks) -> {
                Vector3f cDir = current.getFacing();
                if(previous == null) geo.computeMatrix(current.getFacing());
                else geo.computeMatrix(previous.getFacing(), cDir);
                if(recomputeNormals) {
                    geo.setNormalA(geo.rightX() + geo.upX(), geo.rightY() + geo.upY(), geo.rightZ() + geo.upZ())
                        .setNormalB(geo.rightX() - geo.upX(), geo.rightY() - geo.upY(), geo.rightZ() - geo.upZ());
                }
                geo.place4Verts(current.start(pTicks), 0);
                if(next != null) geo.computeMatrix(cDir, next.getFacing());
                geo.place4Verts(current.end(pTicks), 4);
                geo.walkUVs(current, loftLength);
                geo.emitQuad(buffer, pose, geo.normAX(), geo.normAY(), geo.normAZ(), 0, 4, 5, 1);
                geo.emitQuad(buffer, pose, -geo.normAX(), -geo.normAY(), -geo.normAZ(), 2, 6, 7, 3);
                geo.shiftUVs();
                geo.emitQuad(buffer, pose, geo.normBX(), geo.normBY(), geo.normBZ(), 3, 7, 4, 0);
                geo.emitQuad(buffer, pose, -geo.normBX(), -geo.normBY(), -geo.normBZ(), 1, 5, 6, 2);
        }), SQUARE_CUTOUT(CUTOUT_MATERIAL, SQUARE.extruder),

        CROSS(null, null), CROSS_CUTOUT(null, null),
        BILLBOARD(null, null), BILLBOARD_CUTOUT(null, null),
        NO_DRAW(null, null);

        public final @Nullable MeshExtruder extruder;
        private final @Nullable BiFunction<TransmitterType<?>, Boolean, RenderType> mat;

        private ModelType(BiFunction<TransmitterType<?>, Boolean, RenderType> materialGetter, MeshExtruder extruder) {
            this.extruder = extruder;
            this.mat = Util.memoize(materialGetter);
        }

        public @Nullable RenderType getMaterial(TransmitterType<?> type) {
            return getMaterial(type, false);
        }

        public @Nullable RenderType getMaterial(TransmitterType<?> type, boolean chunk) {
            if(extruder == null) return null;
            if(type == null) return SOLID_MATERIAL.apply(MechanoTransmissionTypes.PERFECT_INSULATOR, chunk);
            ResourceLocation loc = TransmitterRegistry.INSTANCE.getKey(type);
            if(mat == null) {
                Mechano.LOGGER.warn("No valid RenderType could be found for transmitter '" + loc + "' (Model type '" + this + "')");
                return SOLID_MATERIAL.apply(MechanoTransmissionTypes.PERFECT_INSULATOR, chunk);
            }
            RenderType shader = mat.apply(type, chunk);
            if(shader == null) {
                Mechano.LOGGER.warn("No valid RenderType could be found for transmitter '" + loc + "'");
                return SOLID_MATERIAL.apply(MechanoTransmissionTypes.PERFECT_INSULATOR, chunk);
            }
            return shader;
        }
    }

    public static CatenaryAttributeHolder as(ModelType type) {
        return new CatenaryAttributeHolder(type);
    }




    public static final  CatenaryAttributeHolder INVISIBLE
        = CatenaryAttributes
            .as(ModelType.NO_DRAW)
            .withThickness(Thickness.ZERO);

    public static final CatenaryAttributeHolder DEFAULT 
        = CatenaryAttributes
            .as(ModelType.SQUARE)
            .withThickness(Thickness.TRIPLE);


    public static class CatenaryAttributeHolder {

        public @Nullable ModelType model;
        protected @Nullable Thickness thick = Thickness.TRIPLE;

        protected CatenaryAttributeHolder(ModelType model) {
            this.model = model;
        }

        public CatenaryAttributeHolder withThickness(Thickness thick) {
            if(thick == null) return this;
            this.thick = thick;
            return this;
        }

        protected ModelType getModelType() {
            return model;
        }

        public Thickness getThickness() {
            return thick == null ? Thickness.TRIPLE : thick;
        }


        public @Nullable RenderType getShaderFor(TransmitterType<?> type) {
            return model == null ? RenderType.SOLID : model.getMaterial(type);
        }
    }

    

    public static enum Thickness {
        ZERO(0),
        SINGLE(1),
        DOUBLE(2),
        TRIPLE(3),
        QUADRUPLE(4);

        private final int pix;
        private final float thick;
        private Thickness(int pixels) {
            this.pix = pixels;
            this.thick = (pixels) / 16f;
        }

        public int getPixels() {
            return pix;
        }

        public float get() {
            return thick;
        }

        public float half() {
            return thick / 2f;
        }
    }


    public static enum SimulationFeatureset implements StringRepresentable {

        LOW_RES(false, false, 0.5f),
        BASIC(false, false, 1f),
        DISPLACED(false, true, 1f),
        DISPLACED_CORRECTED(true, true, 1f),
        DISPLACED_CORRECTED_HIGH(true, true, 2f);

        private final boolean clampShadows = true;
        private final boolean allowsFlipping;
        private final boolean allowsDisplacement;
        private final float res;

        private SimulationFeatureset(boolean allowsFlipping, boolean allowsProjection, float res) {
            this.allowsFlipping = allowsFlipping;
            this.allowsDisplacement = allowsProjection;
            this.res = res;
        }

        public float getResolution() {
            return res;
        }

        public boolean shouldApplyShadowClamping() {
            return clampShadows;
        }

        public boolean allowsFlipping() {
            return allowsFlipping;
        }

        public boolean allowsDisplacement() {
            return allowsDisplacement;
        }

        public ResourceLocation asResource() {
            return Mechano.asResource("catenary.featureset." + getSerializedName());
        }

        @Override
        public String getSerializedName() {
            return this.toString().toLowerCase(Locale.ROOT);
        }
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

        public Point(float x, float y, float z) {
            this.pos = new Vector3f(x, y, z);
            this.lastPos = new Vector3f(x, y, z);
            this.pinned = false;
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

        public Stick(Point start, Point end) {
            this.start = start;
            this.end = end;
        }

        public Vector3f getDir() {
            Vector3f facing = new Vector3f();
            facing.x = (float)(start.pos.x - end.pos.x);
            facing.y = (float)(start.pos.y - end.pos.y);
            facing.z = (float)(start.pos.z - end.pos.z);
            return facing;
        }

        public Vector3f getFacing() {
            return getDir().normalize();
        }

        public float getLength() {
            return getDir().length();
        }

        public Vector3f getCenter() {
            Vector3f center = new Vector3f();
            center.x = (start.pos.x + end.pos.x) / 2f;
            center.y = (start.pos.y + end.pos.y) / 2f;
            center.z = (start.pos.z + end.pos.z) / 2f;
            return center;
        }

        public Vector3f start(float pTicks) {
            return start.lastPos.lerp(start.pos, pTicks, new Vector3f());
        }

        public Vector3f end(float pTicks) {
            return end.lastPos.lerp(end.pos, pTicks, new Vector3f());
        }

        @Override
        public String toString() {
            return "(" + String.format("%.2f", start.pos.x) + ", " + String.format("%.2f", start.pos.y) + ", " + String.format("%.2f", start.pos.z) + "  ->  " + String.format("%.2f", end.pos.x) + ", " + String.format("%.2f", end.pos.y) + ", " + String.format("%.2f", end.pos.z) + ")";
        }
    }
}
