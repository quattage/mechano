package com.quattage.mechano.api.catenary;

import java.util.Locale;
import java.util.function.BiFunction;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoSounds;
import com.quattage.mechano.api.transmitter.TransmitterType;
import com.quattage.mechano.foundation.numeric.VectorOperations;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllSoundEvents.SoundEntry;

import net.createmod.catnip.theme.Color;
import net.minecraft.Util;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Indicates that implementing subclasses store some way
 * to refer to underlying catenary attributes, with provisions
 * made for adjusting the arclength of a catenary and querying
 * for physical properties.
 */
public class Catenaries {

    @OnlyIn(Dist.CLIENT)
    public static RenderPipeline renderPipeline() {
        return RenderPipeline.SINGLETON;
    }

    /**
     * The physical properties of a catenary, such as its elasticity and tensile strength
     */
    public enum PhysicalMaterial {
        /**
         * Average tension tolerance and little stretch. Will snap.
         */
        ROPE(0.3f, 3.0f, 2f),
        /**
         * No stretch. Can handle high forces
         */
        CHAIN(5f, 20f, 0.01f),
        /**
         * Very stretchy and robust
         */
        BUNGEE(0.15f, 12f, 15f),
        /**
         * Unbreakable and infinitely stretchy.
         * This type exerts no force on attachments at all, so its as if it doesn't exist.
         */
        AIR(-1f, Float.MAX_VALUE, Float.MAX_VALUE),
        /**
         * Unbreakable with zero stretch. 
         * This type exerts maximal force on attachments to fully constrain them.
         * Basically just an infinitely rigid chain that will never break.
         */
        UNOBRANIUM(Float.MAX_VALUE, -1f, 0);

        protected final float reboundForceMultiplier;
        protected final float maxExertionBeforeBreaking;
        protected final float maxStretch;

        PhysicalMaterial(float reboundForceMultiplier, float maxExertionBeforeBreaking, float maxStretch) {
            this.reboundForceMultiplier = reboundForceMultiplier;
            this.maxExertionBeforeBreaking = maxExertionBeforeBreaking;
            this.maxStretch = maxStretch;
        }

        public boolean exertsForce() {
            return reboundForceMultiplier > 0;
        }

        public boolean exertsElasticForce() {
            return reboundForceMultiplier > 0 && reboundForceMultiplier < Float.MAX_VALUE;
        }

        public boolean exertsRigidForce() {
            return reboundForceMultiplier >= Float.MAX_VALUE;
        }

        public boolean isBreakable() {
            return maxExertionBeforeBreaking > -0.01f;
        }

        public float getMaxExertion() {
            return maxExertionBeforeBreaking;
        }

        public float getReboundForce() {
            return reboundForceMultiplier;
        }

        public float getMaxStretch() {
            return maxStretch;
        }
    }

    /**
     * A discrete sound mode that can be selected based on what best matches
     * the desired catenary's characteristics
     */
    public enum Soundscape implements StringRepresentable {
        AIR(false),
        TWINE(true),
        CABLE(true),
        CHAIN(true);

        private final boolean makesNoise;
        private @Nullable SoundEntry[] sounds;

        Soundscape(boolean makesNoise) {
            this.makesNoise = makesNoise;
        }

        /**
         * {@link MechanoSounds#register}
         */
        public static void registerResources() {
            for(Soundscape scape : Soundscape.values()) {
                if(!scape.makesNoise) continue;
                String name = scape.getSerializedName();
                scape.sounds = new SoundEntry[] {
                    AllSoundEvents.create(Mechano.asResource(name + "_place")).category(SoundSource.BLOCKS).build(),
                    AllSoundEvents.create(Mechano.asResource(name + "_break")).category(SoundSource.BLOCKS).build(),
                    AllSoundEvents.create(Mechano.asResource(name + "_stretch")).category(SoundSource.BLOCKS).build(),
                    AllSoundEvents.create(Mechano.asResource(name + "_ambient")).category(SoundSource.BLOCKS).build(),
                    AllSoundEvents.create(Mechano.asResource(name + "_progress")).category(SoundSource.BLOCKS).build(),
                };
                for(int x = 0; x < scape.sounds.length; x++) scape.sounds[x].prepare();
                Mechano.LOGGER.debug("Registered sound resources for SoundScape '" + name + "'");
            }
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        protected void playPlace(LevelReader world, Vec3 pos) {
            if(sounds == null || !(world instanceof ServerLevel la)) return;
            sounds[0].play(la, null, pos.x, pos.y, pos.z, 1f, 1f);
        }

        protected void playBreak(LevelReader world, Vec3 pos) {
            if(sounds == null || !(world instanceof ServerLevel la)) return;
            sounds[1].play(la, null, pos.x, pos.y, pos.z, 1f, 1f);
        }

        protected void playStretch(LevelReader world, Vec3 pos) {
            if(sounds == null || !(world instanceof ServerLevel la)) return;
            sounds[2].play(la, null, pos.x, pos.y, pos.z, 1f, 1f);
        }

        protected void playAmbient(LevelReader world, Vec3 pos) {
            if(sounds == null || !(world instanceof ServerLevel la)) return;
            sounds[3].play(la, null, pos.x, pos.y, pos.z, 1f, 1f);
        }

        protected void playProgress(LevelReader world, Vec3 pos) {
            if(sounds == null || !(world instanceof ServerLevel la)) return;
            sounds[4].play(la, null, pos.x, pos.y, pos.z, 1f, 1f);
        }
        
        @Override
        public String toString() {
            return getSerializedName();
        }
    }

    /**
     * Represents a singular point in 3D space with a controllable position and velocity.
     * This class is intended to be used in a verlet integration to build physically accurate 
     * catenaries.
     * <h3>Important note about frames of reference:</h3>
     * All positional data within this class is stored within singular-precision floats for performance 
     * reasons. It is highly reccomended that implementers avoid working directly with real-world 
     * positional data when creating or transforming points. Catenaries should instead be simulated in 
     * their own local space to avoid significant precision losses.
     * @see Point
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
            VectorOperations.drawDebugBox(basis.add(pos.x, pos.y, pos.z), 0.05f, Color.BLACK, "point_" + hashIndex);
        }

        public int getLight(Vec3 basis, BlockAndTintGetter world) {
            BlockPos pos = new BlockPos((int)Math.floor(this.pos.x + basis.x), (int)Math.floor(this.pos.y + basis.y), (int)Math.floor(this.pos.z + basis.z));
            return LightTexture.pack(world.getBrightness(LightLayer.BLOCK, pos), world.getBrightness(LightLayer.SKY, pos));
        }
    }

    /**
     * Represents a discrete bond between two {@link Point} objects.
     * This class is intended to be used in a verlet integration to build physically accurate 
     * catenaries.
     * <h3>Important note about frames of reference:</h3>
     * All positional data within this class is stored within singular-precision floats for performance 
     * reasons. It is highly reccomended that implementers avoid working directly with real-world 
     * positional data when creating or transforming points. Catenaries should instead be simulated in 
     * their own local space to avoid significant precision losses.
     * @see Point
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
     * A collection of client-only tools and objects critical to the catenary rendering pipeline
     */
    @OnlyIn(Dist.CLIENT)
    public static class RenderPipeline {

        private static final RenderPipeline SINGLETON = new RenderPipeline();
        public final boolean TEX_USE_MIPS = false;
        public final int[] TEX_DIMS = new int[] { 16, 32 };
        public final Vector3f SIM_UP = new Vector3f(0, 1, 0);
        
        private byte minimumSegments = (byte)5;
        private short maximumSegments = (short)256;
        private float catenaryResolution = 0.5f;
        private boolean clampShadows = true;
        private float restitutionSpeed = 0.1f;
        private float restitutionEpsilon = 1e-8f;
        private int solverSteps = 64;

        // TODO switch to custom shader using more optimized vertex format
        public final BiFunction<TransmitterType, Boolean, RenderType> SOLID_MATERIAL 
            = Util.memoize((trns, chunk) -> {
                if(chunk) return RenderType.SOLID;
                RenderType.CompositeState composite = RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_SOLID_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(trns.getRenderProperties().getTextureLocation(), false, TEX_USE_MIPS))
                    .setOverlayState(RenderType.OVERLAY)
                    .setLightmapState(RenderType.LIGHTMAP)
                    .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                    .setOutputState(RenderType.MAIN_TARGET)
                    .setCullState(RenderType.CULL)
                    .createCompositeState(false); 
                return RenderType.create("catenary_solid", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 512, true, false, composite);
            });

        public final BiFunction<TransmitterType, Boolean, RenderType> CUTOUT_MATERIAL 
            = Util.memoize((trns, chunk) -> {
                if(chunk) return RenderType.CUTOUT;
                RenderType.CompositeState composite = RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_CUTOUT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(trns.getRenderProperties().getTextureLocation(), false, TEX_USE_MIPS))
                    .setOverlayState(RenderType.OVERLAY)
                    .setLightmapState(RenderType.LIGHTMAP)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(RenderType.TRANSLUCENT_TARGET)
                    .setCullState(RenderType.CULL)
                    .createCompositeState(false);
                return RenderType.create("catenary_cutout", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 512, true, false, composite);
            });


        public final MeshExtruder SQUARE_EXTRUDER = (VertexConsumer buffer, Pose pose, CatenaryMeshBuffer geo, @Nullable Stick previous, Stick current, @Nullable Stick next, float loftLength, boolean recomputeNormals, float pTicks) -> {
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
        };

        public final MeshExtruder CROSS_EXTRUDER = (VertexConsumer buffer, Pose pose, CatenaryMeshBuffer geo, @Nullable Stick previous, Stick current, @Nullable Stick next, float loftLength, boolean recomputeNormals, float pTicks) -> {
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
        };

        public final MeshExtruder INVISIBLE_EXTRUDER = (VertexConsumer buffer, Pose pose, CatenaryMeshBuffer geo, @Nullable Stick previous, Stick current, @Nullable Stick next, float loftLength, boolean recomputeNormals, float pTicks) -> {};

        public boolean shouldApplyShadowClamping() {
            return clampShadows;
        }

        public float getRestitutionSpeed() {
            return restitutionSpeed;
        }

        public float getRestitutionEpsilon() {
            return restitutionEpsilon;
        }

        public int getSolverSteps() {
            return solverSteps;
        }

        /**
         * The amount of segments that a catenary should contain for a given span,
         * adjusted for resolution and clamping given the current scalability settings.
         * @param spannedLength The actual length in meters between the start and endpoints of 
         * the catenary (not to be confused with its arclength, which, for simulated catenaries, 
         * is not known until simulation is resolved.)
         * @return An int value representing the amount of segments that a catenary should contain
         * @see #getPointCount(float)
         */
        public int getSegmentCount(float spannedLength) {
            return Math.max(minimumSegments, Math.min(maximumSegments, (int)(spannedLength * catenaryResolution)));
        }

        /**
         * The amount of points that a catenary should contain for a given span,
         * adjusted for resolution and clamping given the current scalability settings.
         * @param spannedLength The actual length in meters between the start and endpoints of 
         * the catenary (not to be confused with its arclength, which, for simulated catenaries, 
         * is not known until simulation is resolved.)
         * @return An int value representing the amount of points that a catenary should contain
         * @see #getSegmentCount(float)
         */
        public int getPointCount(float spannedLength) {
            return getPointCount(spannedLength) + 1;
        }

        /**
         * Returns a gravity vector whose strength is scaled
         * appropriately for a given number of discrete mass points.
         * This helper method is designed to be used in verlet integrations,
         * and the velocity vector returned here is normalized against
         * a fixed 20-tick update cycle.
         * @param points The number of individual points in the catenary
         * @return A gravity vector
         */
        public Vector3f getGravity(int points) {
            return SIM_UP.mul((Point.MASS / (float)points) * 0.3f, new Vector3f());
        }

        

        /**
         * Pixel-unit thickness of a catenary's profile when it is constructed as a mesh.
         * Note that thickness values that don't match the texture being used will result
         * the texture being cropped or completely black texels in the untextured area.
         */
        public enum Thickness {
            ZERO(0),
            SINGLE(1),
            DOUBLE(2),
            TRIPLE(3),
            QUADRUPLE(4);

            private final int pix;

            public static Thickness byValue(int x) {
                return Thickness.values()[Mth.clamp(x, 0, Thickness.values().length - 1)];
            }

            Thickness(int pixels) {
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
        
    }
}