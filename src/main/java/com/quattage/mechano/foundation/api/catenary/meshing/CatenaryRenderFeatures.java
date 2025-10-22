package com.quattage.mechano.foundation.api.catenary.meshing;

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
import com.quattage.mechano.foundation.api.catenary.CatenaryModel;
import com.quattage.mechano.foundation.api.catenary.SimulatedCatenary;
import com.quattage.mechano.foundation.api.landmark.GridCatenary;
import com.quattage.mechano.foundation.api.math.VectorHelper;
import com.quattage.mechano.foundation.api.transmitter.MechanoTransmissionTypes;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry;
import com.quattage.mechano.foundation.api.transmitter.TransmitterType;

import net.createmod.catnip.theme.Color;
import net.minecraft.Util;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.data.models.blockstates.PropertyDispatch.QuadFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)

/**
 * A client-sided container for hosting var
 */
public class CatenaryRenderFeatures {

    public static final boolean TEX_USE_MIPS = false;
    public static final int[] TEX_DIMS = new int[] { 16, 32 };
    public static final Vector3f UP = new Vector3f(0, 1, 0);
    public static ScalabilitySettings SETTINGS = ScalabilitySettings.DISPLACED_CORRECTED_HIGH;

    public static boolean LOG_LOCAL_CATENARIES = false;

    /**
     * The speed here is measured in meters per game tick, where
     * any catenary whose average point velocity magnitude is 
     * below this value is considered to be in a state of restitution.
     * This allows verlet simulations to approximately determine
     * whether or not they have accumulated minumal potential energy,
     * and catenaries moving slower than this may freeze to save
     * computation cost
     */
    public static final float RESTITUTION_SPEED = 0.1f;

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
            return RenderType.create("catenary_solid", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 512, true, false, composite);
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
            return RenderType.create("catenary_cutout", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 512, true, false, composite);
        });

    public static enum ModelType implements StringRepresentable {

        SQUARE(SOLID_MATERIAL, (VertexConsumer buffer, Pose pose, CatenaryMeshBuffer geo, @Nullable Stick previous, Stick current, @Nullable Stick next, 
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
                geo.unshiftUVs();
        }), SQUARE_CUTOUT(CUTOUT_MATERIAL, SQUARE.extruder),

        CROSS(SOLID_MATERIAL, (VertexConsumer buffer, Pose pose, CatenaryMeshBuffer geo, @Nullable Stick previous, Stick current, @Nullable Stick next, 
        float loftLength, boolean recomputeNormals, float pTicks) -> {
            Vector3f cDir = current.getFacing();
            if(previous == null) geo.computeMatrix(current.getFacing());
            else geo.computeMatrix(previous.getFacing(), cDir);
            geo.shiftMatrix();
            if(recomputeNormals) {
                geo.setNormalA(geo.rightX() + geo.upX(), geo.rightY() + geo.upY(), geo.rightZ() + geo.upZ())
                    .setNormalB(geo.rightX() - geo.upX(), geo.rightY() - geo.upY(), geo.rightZ() - geo.upZ());
            }
            geo.place4Verts(current.start(pTicks), 0);
            if(next != null) {
                geo.computeMatrix(cDir, next.getFacing());
                geo.shiftMatrix();
            }
            geo.place4Verts(current.end(pTicks), 4);
            geo.walkUVs(current, loftLength);
            geo.emitQuad(buffer, pose, geo.normAX(), geo.normAY(), geo.normAZ(), 0, 4, 6, 2);
            geo.emitQuad(buffer, pose, -geo.normAX(), -geo.normAY(), -geo.normAZ(), 2, 6, 4, 0);
            geo.shiftUVs();
            geo.emitQuad(buffer, pose, geo.normBX(), geo.normBY(), geo.normBZ(), 3, 7, 5, 1);
            geo.emitQuad(buffer, pose, -geo.normBX(), -geo.normBY(), -geo.normBZ(), 1, 5, 7, 3);
            geo.unshiftUVs();
        }), CROSS_CUTOUT(CUTOUT_MATERIAL, CROSS.extruder),

        // UNIMPLEMENTED
        BILLBOARD(SOLID_MATERIAL, null), BILLBOARD_CUTOUT(CUTOUT_MATERIAL, BILLBOARD.extruder),
        NO_DRAW(null, null);

        public final @Nullable MeshExtruder extruder;
        private final @Nullable BiFunction<TransmitterType<?>, Boolean, RenderType> mat;

        private ModelType(BiFunction<TransmitterType<?>, Boolean, RenderType> materialGetter, MeshExtruder extruder) {
            this.extruder = extruder;
            this.mat = Util.memoize(materialGetter);
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

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        @Override
        public String toString() {
            return getSerializedName();
        }
    }


    public static enum MeshInitializer {

        FRESH_SIMULATION((world, start, end, trns) -> {
            CatenaryModel<?> output = new SimulatedCatenary();
            output.setOrderedOffset(world, trns, start.getAddress(), end.getAddress(), 1)
                .initializeSpan()
                .calculateSegmentation(trns)
                .pinEndpoints();
            output.update(trns);
            return output;
        }),
        FRESH_SIMULATION_EXPRESSIVE((world, start, end, trns) -> {
            SimulatedCatenary output = new SimulatedCatenary();
            output.setOrderedOffset(world, trns, start.getAddress(), end.getAddress(), 1)
                .initializeSpan()
                .calculateSegmentation(trns)
                .pinEndpoints();
            output.update(trns);
            output.kick(0.35f);
            return output;
        }),
        RESTING_SIMULATION((world, start, end, trns) -> {
            CatenaryModel<?> output = new SimulatedCatenary();
            output.setOrderedOffset(world, trns, start.getAddress(), end.getAddress(), 1)
                .initializeSpan()
                .calculateSegmentation(trns)
                .pinEndpoints();
            output.updateAhead(trns, 512);
            return output;
        });

        public static GridCatenary applyPreexistingSpan(LevelReader world, GridCatenary cat, float span) {
            SimulatedCatenary model = new SimulatedCatenary();
            TransmitterType<?> trns = cat.getTransmitter().getType();
            model.adjustSpan(world, Mth.clamp(span, 0f, trns.getMaximumSpan()));
            cat.forceModel(world, model);
            cat.lockSpanIfNecessary(world);
            model.setOrderedOffset(world, trns, cat.getStartAnchor(), cat.getEndAnchor(), 1)
                .initializeSpan()
                .calculateSegmentation(trns);
            model.pinEndpoints();
            model.update(trns);
            model.kick(0.35f);
            return cat;
        }

        private final QuadFunction<LevelReader, AnchorPoint, AnchorPoint, TransmitterType<?>, CatenaryModel<?>> func;
        private MeshInitializer(QuadFunction<LevelReader, AnchorPoint, AnchorPoint, TransmitterType<?>, CatenaryModel<?>> func) { 
            this.func = func; 
        }

        public CatenaryModel<?> make(LevelReader world, AnchorPoint start, AnchorPoint end, TransmitterType<?> trns) { 
            return func.apply(world, start, end, trns); 
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

        /**
         * The mass of a single point within verlet-based catenaries.
         */
        public static final int MASS = 3;

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
     * catenary's Local frame of reference. For more information, 
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

    /**
     * Pixel-unit thickness of a catenary when it is drawn.
     * The rendering process does not suport UV stretching
     * (which would result in texels that are too big/small), 
     * so this must match the width of the texture you're using.
     */
    public static enum Thickness {
        ZERO(0),
        SINGLE(1),
        DOUBLE(2),
        TRIPLE(3),
        QUADRUPLE(4);

        private final int pix;

        private Thickness(int pixels) {
            this.pix = pixels;
        }

        public int pixels() {
            return pix;
        }

        public float raw() {
            return (float)pix / 16f;
        }

        public float half() {
            return (float)pix / 32f;
        }
    }
    
    public static enum ScalabilitySettings implements StringRepresentable {

        LOW_RES(false, false, new float[] {0.25f, 45, 3, 24}),
        BASIC(false, false, new float[] {0.5f, 45, 3, 24}),
        DISPLACED(false, true, new float[] {0.5f, 45, 5, 32}),
        DISPLACED_CORRECTED(true, true, new float[] {0.5f, 64, 5, 32}),
        DISPLACED_CORRECTED_HIGH(true, true, new float[] {1f, 64, 5, 32});

        private final boolean clampShadows = true;
        private final boolean allowsFlipping;
        private final boolean allowsDisplacement;
        private final float[] res;

        private ScalabilitySettings(boolean allowsFlipping, boolean allowsProjection, float[] res) {
            this.allowsFlipping = allowsFlipping;
            this.allowsDisplacement = allowsProjection;
            this.res = res;
        }

        public float getResolution() {
            return res[0];
        }

        /**
         * temporary - TODO refactor to allow multiple light sampling modes
         
         */
        public boolean shouldApplyShadowClamping() {
            return clampShadows;
        }

        /**
         * @return <code>true</code> if catenaries should keep track of whether or not
         * their primary construct has flipped within the span of one frame. disabling
         * this feature can save some frame time but may cause visual artifacting
         */
        public boolean allowsFlipping() {
            return allowsFlipping;
        }

        /**
         * @return <code>true</code> if verlet-based catenaries should attempt
         * to inherit the velocity of the things they're attached to
         * enabling this incurs a (very slight) additional computation cost.
         */
        public boolean allowsVelocityDisplacement() {
            return allowsDisplacement;
        }

        /**
         * @return how many times the constraint solver should run in verlet-based catenary simulations - 
         * increasing this number scales the complexity of the default implementation in linear time.
         */
        public int getSolverSteps() {
            return (int)res[1];
        }

        /**
         * @return the minimum amount of segments that can be hosted by a catenary simulation and still be considered valid.
         */
        public int getMinimumSegments() {
            return (int)res[2];
        }

        /**
         * @return the maximum amount of segments that a catenary simulation may support at once
         */
        public int getMaximumSegments() {
            return (int)res[3];
        }

        public ResourceLocation asResource() {
            return Mechano.asResource("catenary.featureset." + getSerializedName());
        }

        @Override
        public String getSerializedName() {
            return this.toString().toLowerCase(Locale.ROOT);
        }
    }

}
