package com.quattage.mechano.api.catenary;

import java.util.Locale;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoSounds;
import com.quattage.mechano.api.anchor.AnchorPoint;
import com.quattage.mechano.api.catenary.meshing.CatenaryRenderFeatures.ModelType;
import com.quattage.mechano.api.catenary.meshing.CatenaryRenderFeatures.Thickness;
import com.quattage.mechano.api.griddable.Griddable;
import com.quattage.mechano.api.identifier.GridUUID;
import com.quattage.mechano.api.landmark.GridConnection;
import com.quattage.mechano.api.transmitter.TransmitterType;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllSoundEvents.SoundEntry;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Indicates that implementing subclasses store some way
 * to refer to underlying catenary attributes, with provisions
 * made for adjusting the arclength of a catenary and querying
 * for physical properties.
 */
public interface CatenaryAttributable {

    public static class Container {

        private byte modeltypeOrdinal = 6;
        private byte thicknessOrdinal = 0;
        private @NotNull PhysicalMaterial material = PhysicalMaterial.ROPE;
        private @NotNull Soundscape sounds = Soundscape.CABLE;
        private int maxSpan = 32;
        private boolean canInterconnect = false;
        private boolean ignoresRestrictions = false;

        public Container withModelType(int mtOrd) {
            this.modeltypeOrdinal = (byte)mtOrd;
            return this;
        }

        /**
         * Thickness of the catenary measured in pixels. <strong>
         * In-world pixel measurements can be thought of as 1/16th of a meter
         * </strong>
         * Expects values between 0 (no draw) and 4 (super thick)
         * @see Thickness
         */
        public Container withThickness(int pixels) {
            this.thicknessOrdinal = (byte)pixels;
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
         * a particular {@link com.quattage.mechano.api.griddable.Griddable endpoint}
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

        @OnlyIn(Dist.CLIENT)
        public ModelType getModelType() {
            return ModelType.values()[Mth.clamp(modeltypeOrdinal, 0, ModelType.values().length)];
        }

        @OnlyIn(Dist.CLIENT)
        public Thickness getThickness() {
            return Thickness.values()[Mth.clamp(thicknessOrdinal, 0, Thickness.values().length)];
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

        @OnlyIn(Dist.CLIENT)
        public boolean renders() {
            return getModelType() != ModelType.NO_DRAW && getThickness() != Thickness.ZERO;
        }

        public void playBreakEffect(LevelReader world, GridConnection connection) {
            if(world.isClientSide()) return;
            sounds.playBreak(world, connection.getMiddlePos(world));
        }
    }

    public static final float DETACH_THRESHOLD = 0.6f;

    public static enum PhysicalMaterial {
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

        private PhysicalMaterial(float reboundForceMultiplier, float maxExertionBeforeBreaking, float maxStretch) {
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

    public static enum Soundscape implements StringRepresentable {
        AIR(false),
        TWINE(true),
        CABLE(true),
        CHAIN(true);

        private final boolean makesNoise;
        private @Nullable SoundEntry[] sounds;

        private Soundscape(boolean makesNoise) {
            this.makesNoise = makesNoise;
        }

        /**
         * {@link MechanoSounds#register}
         */
        public static void registerResources() {
            for(Soundscape scape : values()) {
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
     * Gets a {@link CatenaryAttributable.Container}
     * associated with this object with a nullcheck and
     * warning. If null, this method will return a new
     * container with default settings.
     * @return A catenary container assoicaited with this object.
     */
    public default Container getCatenaryAttributableSafe() {
        return getCatenaryAttributableOrElse(new Container());
    }
    /**
     * Gets a {@link CatenaryAttributable.Container}
     * associated with this object with a nullcheck and
     * warning. If null, this method will return instance passed.
     * @param fallback CatenaryAttributable container that will be returned instead
     * @return A catenary container assoicaited with this object.
     */
    public default Container getCatenaryAttributableOrElse(Container fallback) {
        Container c = getCatenaryAttributable();
        if(c == null)
            return fallback;
        return c;
    }
    /**
     * Gets a {@Link CatenaryAttributable.Container}
     * associated with this object. If one does not exist,
     * this method will throw.
     * @return A catenary container assoicaited with this object.
     * 
     * @throws IllegalStateException if <code>null</code>
     */
    public default Container getCatenaryAttributableOrThrow() {
        Container c = getCatenaryAttributable();
        if(c == null)
            throw new IllegalStateException("An operation attempted to acquire a catenary attribute container from an object that returned null!");
        return c;
    }

    public default float getMinimumSpan() { return getCatenaryAttributableOrThrow().getMinimumSpan(); }
    public default float getMaximumSpan() { return getCatenaryAttributableOrThrow().getMaximumSpan(); }

    public abstract void adjustSpan(LevelReader world, float length);
    public abstract float calculateSpan();
    public abstract Container getCatenaryAttributable();

    public default CompoundTag writeSpan(CompoundTag in) {
        in.putShort("span", packSpan(calculateSpan()));
        return in;
    }

    public static short packSpan(float span) {
        return (short)((Mth.clamp(span, 0.01f, 1023.97) * 64f) - Short.MAX_VALUE);
    }

    public static float makeSpan(LevelReader world, GridUUID start, GridUUID end, @Nullable TransmitterType<?> trns, @Nullable CompoundTag in) {
        float read = (in != null && in.contains("span")) ? getSpanFromShort(in.getShort("span")) : ((float)start.getPos(world).distanceTo(end.getPos(world)));
        return Mth.clamp(read, 0, trns == null ? 32 : trns.getMaximumSpan());
    }

    public static float getSpanFromShort(short packed) {
        return ((float)packed + Short.MAX_VALUE) / 64f;
    }
}
