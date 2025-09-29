

package com.quattage.mechano.foundation.api.catenary;

import java.util.Locale;
import java.util.function.BiFunction;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.Griddable;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.transmitter.MechanoTransmissionTypes;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry;
import com.quattage.mechano.foundation.api.transmitter.TransmitterType;
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
    public static final SimulationFeatureset FEATURESET = SimulationFeatureset.DISPLACED_CORRECTED_HIGH;
    public static final float DETACH_THRESHOLD = 0.6f;

    /**
     * This controls how many times the constraint solver should run in verlet-based catenary simulations.
     * Unfortunately this shouldn't be altered by the player for scalability settings, since doing could
     * cause catenaries to look very different between clients. Any lower than this and the constraint solver
     * can't properly resolve tight catenaries, so its like they're always loose even if they should be tight.
     */
    public static final int SOLVER_STEPS = 64;

    /**
     * The uniform mass of every catenary. Note that longer catenaries (with more segments)
     * don't actually weigh more than shorter ones. I don't actually remember why I did this,
     * but I don't want to touch it since it's currently working.
     */
    public static final float MASS = 3f;

    /**
     * The maximum amount of speed that a catenary is able to
     * possess throughout its points that is considered still enough
     * that the simulation may pause without visual oddities.
     * The speed here is measured in meters per game tick, where
     * any catenary whose average point velocity magnitude is 
     * below this value is considered to be in a state of restitution.
     * This allows verlet simulations to approximately determine
     * whether or not they have reached a state of mimimum potential energy.
     */
    public static final float RESTITUTION_SPEED = 0.1f;

    public static final int DRAW_MIN = 5; // minimum amount of segments in a single catenary
    public static final int DRAW_MAX = 32; // maximum amount of segments in a single catenary

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

    public static enum MeshInitializer {
        FRESH_SIMULATION((world, start, end, attr) -> {
            CatenaryModel<?> output = new SimulatedCatenary();
            output.maxLength = attr.getMaximumSpan();
            output.setOrderedOffset(world, start.getAddress(), end.getAddress(), 1)
                .initializeSpan()
                .calculateSegmentation()
                .pinEndpoints();
            output.update();
            return output;
        }),
        FRESH_SIMULATION_EXPRESSIVE((world, start, end, attr) -> {
            SimulatedCatenary output = new SimulatedCatenary();
            output.maxLength = attr.getMaximumSpan();
            output.setOrderedOffset(world, start.getAddress(), end.getAddress(), 1)
                .initializeSpan()
                .calculateSegmentation()
                .pinEndpoints();
            output.update();
            output.kick(0.35f);
            return output;
        }),
        RESTING_SIMULATION((world, start, end, attr) -> {
            CatenaryModel<?> output = new SimulatedCatenary();
            output.maxLength = attr.getMaximumSpan();
            output.setOrderedOffset(world, start.getAddress(), end.getAddress(), 1)
                .initializeSpan()
                .calculateSegmentation()
                .pinEndpoints();
            output.updateAhead(512);
            return output;
        });

        private final QuadFunction<LevelReader, AnchorPoint, AnchorPoint, CatenaryAttributes.Container, CatenaryModel<?>> func;
        private MeshInitializer(QuadFunction<LevelReader, AnchorPoint, AnchorPoint, CatenaryAttributes.Container, CatenaryModel<?>> func) { 
            this.func = func; 
        }

        public CatenaryModel<?> make(LevelReader world, AnchorPoint start, AnchorPoint end, TransmitterType<?> trns) { 
            return func.apply(world, start, end, trns.getCatenaryAttributesOrThrow()); 
        }
    }

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

        CROSS(null, null), CROSS_CUTOUT(null, null),
        BILLBOARD(null, null), BILLBOARD_CUTOUT(null, null),
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



    public static class Container {

        private @NotNull ModelType model = ModelType.SQUARE;
        private @NotNull Thickness thick = Thickness.TRIPLE;
        private @NotNull PhysicalMaterial material = PhysicalMaterial.ROPE;
        private @NotNull Soundscape sounds = Soundscape.CABLE;
        private int maxSpan = 32;
        private boolean canInterconnect = false;
        private boolean ignoresRestrictions = false;

        public Container withModelType(ModelType model) {
            if(model == null) return this;
            this.model = model;
            return this;
        }

        public Container withThickness(Thickness thick) {
            if(thick == null) return this;
            this.thick = thick;
            return this;
        }

        public Container withMaterial(PhysicalMaterial material) {
            if(material == null) return this;
            this.material = material;
            return this;
        }

        public Container withSounds(Soundscape sounds) {
            if(sounds == null) return this;
            this.sounds = sounds;
            return this;
        }
        
        /**
         * Enables the ability for {@link TransmissionType transmitters}
         * inheriting from this container to make connections between two
         * {@link AnchorPoint AnchorPoints} that belong to the same
         * {@link Griddable source}. For transmitters that the player
         * can manipulate directly, it is reccomended for this to be
         * disabled, (its disabled by default) since 
         * @return This container for chaining
         */
        public Container enableInterconnectivity() {
            this.canInterconnect = true;
            return this;
        }

        /**
         * Disables all restrictions for the placability of
         * {@link TransmissionType transmitters} inheriting from
         * this container. This is useful for transmitters that 
         * shouldn't care whether or not they're compatible with
         * a particular {@link com.quattage.mechano.foundation.api.Griddable endpoint}
         * and should instead always be connectable to everything.
         * @return
         */
        public Container bypassRestrictions() {
            this.ignoresRestrictions = true;
            return this;
        }

        /**
         * The maximum spanned length (in meters) of individual
         * catenaries that inherit from this container. Spools
         * will not be permitted to produce catenaries past this length.
         * @param maxSpan
         * @return This container for chaining
         */
        public Container maxLength(int maxSpan) {
            this.maxSpan = maxSpan;
            return this;
        }

        public ModelType getModelType() {
            return model;
        }

        public Thickness getThickness() {
            return thick;
        }

        public int getMaximumSpan() {
            return maxSpan;
        }

        public float getMinimumSpan() {
            return canInterconnect ? 0 : 0.125f;
        }

        public boolean shouldApplyRestrictions() {
            return !ignoresRestrictions;
        }

        public boolean supportsInterconnectivity() {
            return canInterconnect;
        }

        public PhysicalMaterial getPhysicalMaterial() {
            return material;
        }

        public boolean renders() {
            return model != ModelType.NO_DRAW && thick != Thickness.ZERO;
        }
    }


    /**
     * Indicates that implementing classes store a CatenaryAttributes
     * container.
     */
    public static interface CatenaryAttributable {
        /**
         * Gets a {@link CatenaryAttributes.Container}
         * associated with this object with a nullcheck and
         * warning. If null, this method will return a new
         * container with default settings.
         * @return A catenary container assoicaited with this object.
         */
        public default Container getCatenaryAttributesSafe() {
            return getCatenaryAttributesOrElse(new CatenaryAttributes.Container());
        }
        /**
         * Gets a {@link CatenaryAttributes.Container}
         * associated with this object with a nullcheck and
         * warning. If null, this method will return instance passed.
         * @param fallback CatenaryAttributes container that will be returned instead
         * @return A catenary container assoicaited with this object.
         */
        public default Container getCatenaryAttributesOrElse(CatenaryAttributes.Container fallback) {
            Container c = getCatenaryAttributes();
            if(c == null)
                return fallback;
            return c;
        }
        /**
         * Gets a {@Link CatenaryAttributes.Container}
         * associated with this object
         * @return A catenary container assoicaited with this object.
         */
        public default Container getCatenaryAttributesOrThrow() {
            Container c = getCatenaryAttributes();
            if(c == null)
                throw new IllegalStateException("An operation attempted to acquire a catenary attribute container from an object that returned null!");
            return c;
        }

        public default float getMinimumSpan() { return getCatenaryAttributesOrThrow().getMinimumSpan(); }
        public default float getMaximumSpan() { return getCatenaryAttributesOrThrow().getMaximumSpan(); }

        public abstract void adjustSpan(LevelReader world, float length);
        public abstract float calculateSpan();
        public abstract Container getCatenaryAttributes();
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


    public static enum PhysicalMaterial {

        /**
         * Average tension tolerance and little stretch. Will snap.
         */
        ROPE(0.2f, 5f),
        /**
         * No stretch. Can handle high forces
         */
        CHAIN(1f, 20f),
        /**
         * Very stretchy and robust
         */
        BUNGEE(0.3f, 12f),
        /**
         * Extremely stretchy, but fragile and exerts little force on attachments
         */
        RUBBER(1f, 3f),
        /**
         * Unbreakable and infinitely stretchy.
         * This type exerts no force on attachments at all, so its as if it doesn't exist.
         */
        AIR(-1f, Float.MAX_VALUE),
        /**
         * Unbreakable with zero stretch. 
         * This type exerts maximal force on attachments to fully constrain them.
         * Basically just an infinitely rigid chain that will never break.
         */
        UNOBRANIUM(Float.MAX_VALUE, -1f);

        protected final float reboundForceMultiplier;
        protected final float maxExertionBeforeBreaking;

        private PhysicalMaterial(float reboundForceMultiplier, float maxExertionBeforeBreaking) {
            this.reboundForceMultiplier = reboundForceMultiplier;
            this.maxExertionBeforeBreaking = maxExertionBeforeBreaking;
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
    }

    public static enum Soundscape {
        AIR,
        TWINE,
        CABLE,
        CHAIN;
    }

    /**
     * Scalability settings for catenary simulations. Unfortunatley,
     * simulation steps cannot be included here, since turning
     * the iteration count down would drastically alter the perceived
     * tension of the catenary. Lower simulation step counts resolve 
     * to lazy, loose cables.
     */
    public static enum SimulationFeatureset implements StringRepresentable {

        LOW_RES(false, false, 0.25f),
        BASIC(false, false, 0.5f),
        DISPLACED(false, true, 0.5f),
        DISPLACED_CORRECTED(true, true, 0.5f),
        DISPLACED_CORRECTED_HIGH(true, true, 1f);

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
