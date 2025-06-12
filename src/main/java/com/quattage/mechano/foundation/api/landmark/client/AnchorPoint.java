package com.quattage.mechano.foundation.api.landmark.client;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoDataAttachments;
import com.quattage.mechano.foundation.api.PowerGridBlockEntity;
import com.quattage.mechano.foundation.api.landmark.GridNode.Tracker;
import com.quattage.mechano.foundation.api.landmark.base.NodeIdentifiable;
import com.quattage.mechano.foundation.api.landmark.base.NodeIdentifier;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;
import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.helper.VectorHelper;

import static com.quattage.mechano.Mechano.lang;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * An AnchorPoint is the client-sided mirror implementation of the
 * {@link com.quattage.mechano.foundation.api.landmark.GridNode GridNode},
 * built specifically to store transformation data. 
 * 
 * <p> The AnchorPoint
 * occupies physical space, where the GridNode does not.
 */
public class AnchorPoint extends NodeIdentifier {

    public static final int VIS_RANGE = 15;

    private byte[] data;

    private boolean enabled;
    private Vector3f offset;

    /**
     * A bitmask for determining what types of connections this anchor can support
     */
    public final int bitmask;

    public AnchorPoint(BlockPos pos, int index, float px, float py, float pz, float size, boolean enabled, int maxc) {
        super(pos, index);
        this.data = new byte[]{
            packMeasurement(px),
            packMeasurement(py),
            packMeasurement(pz),
            packMeasurement(Math.max(0.001f, size)),
            Byte.MIN_VALUE,
            (byte)(Math.max(0, Math.min(maxc, 256)) - 128)
        };
        this.offset = getRaw();
        this.enabled = enabled;
        bitmask = 0xFFFFFFFF;
    }


    /**
     * Gets an AnchorPoint at the given address
     * @param world World to operate within
     * @param address Address to find
     * @return AnchorPoint at the given address, or <code>null</code> if one couldn't be found.
     */
    public static @Nullable AnchorPoint retrieve(LevelReader world, @Nullable NodeIdentifiable address) {
        if(address == null) return null;
        if(!world.isClientSide()) {
            Mechano.LOGGER.error("Attempted to retrieve AnchorPoint " + address + " from non-permissible server context!");
            return null;
        }
        PowerGridBlockEntity pgbe = address.getHost(world);
        return pgbe == null ? null : pgbe.anchors.getByIndex(address.getIndex());
    }

    /**
     * Gets an AnchorPoint at the given address
     * @param world World to operate within
     * @param stack ItemStack to extract the address from. Expected to be stored as a {@link com.quattage.mechano.MechanoDataAttachments#ADDRESS_COMPONENT data attachment}
     * @return AnchorPoint at the given address, or <code>null</code> if one couldn't be found.
     */
    public static @Nullable AnchorPoint retrieve(LevelReader world, @Nullable ItemStack stack) {
        if(stack == null) return null;
        return retrieve(world, stack.get(MechanoDataAttachments.ADDRESS_COMPONENT));
    }

    /**
     * Evaluates the world to determine whether the block hosting this AnchorPoint
     * still exists. Note - Always returns <code>FALSE</code> on the server, as AnchorPoints
     * should <strong>only</strong> be instantiated on the client.
     * @param world
     * @return <code>true</code> if this AnchorPoint belongs to a BlockEntity that exists
     */
    public boolean existsInWorld(LevelReader world) {
        if(world == null) return false;
        if(!world.isClientSide()) return false;
        BlockEntity be = world.getBlockEntity(pos);
        if(be == null) return false;
        if(!(be instanceof PowerGridBlockEntity pgbe)) return false;
        return pgbe.anchors != null && index < pgbe.anchors.size();
    }

    public void sync(byte connections, @Nullable LevelReader refresher) {
        this.data[4] = connections;
        if(refresher != null) {
            PowerGridBlockEntity pgbe = getHost(refresher);
            if(pgbe == null) return;
            if(getCurrentConnections() > 0)
                pgbe.surrogate.sync(refresher, null);
            else pgbe.surrogate.forget(refresher);
            BlockState state = pgbe.getBlockState();
            if(state == null) return;
            updateOrientation(state);
        }
    }

    /**
     * @return The actual Vec3 position of this AnchorPoint with its offset applied
     * @see {@link #getOffset} to get this AnchorPoint's offset in local space
     */
    public Vec3 getRealPosition() {
        return new Vec3(
            getX() + offset.x,
            getY() + offset.y,
            getZ() + offset.z
        );
    }


    /**
     * @return The raw Vec3 offset of this AnchorPoint from its BlockPos
     * @see {@link #getRealPosition} to get the actual in-world position of this AnchorPoint
     */
    public Vec3 getOffset() {
        return new Vec3(offset.x, offset.y, offset.z);
    }

    /**
     * This method is used to determine whether or not the provided transfer transmitter
     * can be used with this AnchorPoint. Useful for enforcing tiers or types
     * of wires/connectors that should coorespond with one another.
     * @param tfp Transmitter to compare
     * @return <code>true</code> if the provided TFP can interact with this AnchorPoint
     */
    public boolean isCompatableWith(TransmitterType<?> type) {
        if(type.ignoresLimits()) return true;
        return (bitmask & type.bitmask()) != 0;
    }

    private Vector3f getRaw() {
        return new Vector3f(unpackMeasurement(data[0]) / 16f, unpackMeasurement(data[1]) / 16f, unpackMeasurement(data[2]) / 16f);
    }

    public float getSize() {
        return unpackMeasurement(data[3]) / 16f;
    }

    /** 
     * Check whether or not this AnchorPoint can support more connections
     * @return <code>true</code> if <code>current connections < max connections</code>
     * @see {@link AnchorPoint#getCurrentConnections()}
     * @see {@link AnchorPoint#getMaxConnections()}
     */
    public boolean hasRoom() {
        return getCurrentConnections() < getMaxConnections();
    }

    /**
     * @return The amount of connections that this AnchorPoint is currently hosting
     */
    public int getCurrentConnections() {
        return data[4] + 128;
    }

    /**
     * @return The maximum possible amount of connections that this AnchorPoint could host
     */
    public int getMaxConnections() {
        return data[5] + 128;
    }

    /**
     * Sets the current number of connections hosted by this
     * AnchorPoint to zero.
     */
    public void resetCurrentConnections() {
        data[4] = Byte.MIN_VALUE;
    }

    /**
     * Updates the location and hitbox of this AnchorPoint
     * to reflect the data contained within the given BlockState
     * @param state state to extract orientation data
     */
    public void updateOrientation(BlockState state) {
        offset = VectorHelper.rotate(getRaw(), DirectionTransformer.extract(state));
    }

    /**
     * Updates the location and hitbox of this AnchorPoint
     * to reflect the orientation in the provided CombinedOrientation.
     * Automatically rebuilds the hitbox as a result.
     * @param dir Orientation to use when transforming this AnchorPoint
     */
    public void updateOrientation(CombinedOrientation dir) {
        offset = VectorHelper.rotate(getRaw(), dir);
    }

    private static float unpackMeasurement(byte in) {
        return (in + 128) * (48f / 255f) - 16f;
    }

    private static byte packMeasurement(float in) {
        return (byte)((Math.max(-16, Math.min(in, 32)) + 16f) * (255f / 48f) - 128f);
    }

    /**
     * Builds a new AABB based on this hitbox's current offset 
     * and size. <p>
     * Note that this hitbox may be out of date if the AnchorPoint has moved.
     * To ensure that this is not the case, a call to {@link AnchorPoint#updateOrientation}
     * should be made at some point to reflect the change to this AnchorPoint's position.
     * @param useSize if <code>false<code>, the returned AABB will have a size of 0.
     * @return A new AABB describing this AnchorPoint's hitbox
     */
    public AABB makeHitbox(boolean useSize) {
        float size = useSize ? getSize() : 0;
        return new AABB(
            (getX() + offset.x) - size,
            (getY() + offset.y) - size,
            (getZ() + offset.z) - size,
            (getX() + offset.x) + size,
            (getY() + offset.y) + size,
            (getZ() + offset.z) + size
        );
    }


    public boolean isEnabled() {
        return this.enabled;
    }


    public float distanceTo(AnchorPoint other) {
        return (float)getRealPosition().distanceTo(other.getRealPosition());
    }

    /**
     * Enables this AnchorPoint, which allows it to be seen
     * and interacted with by the player.
     * This method, along with {@link AnchorPoint#disable}, can
     * be used to reflect BlockState or BlockEntity changes that
     * may visually or functionally obscure AnchorPoints. 
     */
    public void enable() {
        this.enabled = true;
    }

    /**
     * Disables this AnchorPoint, which hides it from the world
     * and prevents all player interaction with it. 
     * This method, along with {@link AnchorPoint#enable}, can
     * be used to reflect BlockState or BlockEntity changes that
     * may visually or functionally obscure AnchorPoints. 
     * Do note that calls to {@link AnchorPoint#disable} will <strong>not</strong>
     * break wires or sever connections to/from this AnchorPoint.
     */
    public void disable() {
        this.enabled = false;
    }

    /**
     * Writes information stored in this AnchorPoint to a tooltip string
     * for display to the player
     * @param tooltip
     */
    public void writeInfoToTooltip(List<Component> tooltip) {
        lang().text(getCurrentConnections() + "/" + getMaxConnections()).forGoggles(tooltip);;
    }

    /**
     * Performs an intersection test with this 
     * AnchorPoint's hitbox to determine whether
     * the player is targeting this AnchorPoint
     * @param ray Raycast to test
     * @return <code>true</code> if <code>ray</code> is intersecting this AnchorPoint
     */
    public boolean isIntersecting(VectorHelper.Ray ray) {
        return makeHitbox(true).clip(ray.start, ray.end).isPresent();
    }

    @Override
    public Tracker makeTrackable() {
        throw new UnsupportedOperationException("AnchorPoints aren't trackable!");
    }

    @Override
    public CompoundTag writeTo(CompoundTag in) {
        in.putInt("i", index);
        in.putByteArray("xyzs", data);
        return in;
    }

    @Override
    public String toString() {
        String x = String.format("%.2f", offset.x);
        String y = String.format("%.2f", offset.y);
        String z = String.format("%.2f", offset.z);
        String mask = Integer.toBinaryString(bitmask);
        return "(" + x + "," + y + "," + z + ", " + index + " / " + NodeIdentifier.MAX_OCCUPANCY + "), " + mask;
    }




















    /**
     * A fluent-ish builder for instantiating AnchorPoints in BlockEntities
     */
    public static class Builder {

        private float offx = 0f;
        private float offy = 0f;
        private float offz = 0f;
        private float size = 2;
        private boolean enabled;
        private int max = 2;
        private final AnchorArray.Builder prev;

        public Builder(AnchorArray.Builder prev) {
            this.prev = prev;
            this.enabled = true;
        }

        /**
         * The position of this anchor relative to the northern bottom corner of the block. <p>
         * This is based on pixel measurements (out of 16) rather than raw vectors. For example, 
         * if you wanted to place a node on the center of the block, you would use:
         * <pre> AnchorPointBuilder.at(8, 8, 8); </pre> 
         * <strong>Some things to note when placing anchors:</strong>
         * <ul>
         * <li>The coordinate system used by anchors mirrors the json model data structure, so you can copy coordinates directly from Blockbench.</li>
         * <li>The coordinates you supply must be in the range of (-16, 32)</li>
         * <li>Half-pixel measurements are permitted, but may be {@link AnchorPoint#packMeasurement(float) quantized unpredictably} when the anchor is constructed.</li>
         * <li>The anchor's hitbox can be partially obscured by the block itself, but not entirely, or it won't be selectable by the player.</li>
         * <li>The position you supply is automatically rotated along with the block it belongs to, so coordinates should always be supplied assuming the block is in its default blockstate.</li>
         * </ul>
         * @param x 
         * @param y 
         * @param z 
         * @return
         */
        public Builder at(float x, float y, float z) {
            this.offx = x;
            this.offy = y;
            this.offz = z;
            return this;
        }

        /**
         * Radius (in pixels, between 0 and 32) of the hitbox and anchor cuboid
         * @param size
         * @return
         */
        public Builder radius(float size) {
            this.size = size;
            return this;
        }

        /**
         * The maximum amount of connections that this
         * anchor can support at the same time, up to 256.
         * @param max
         * @return
         */
        public Builder connections(int max) {
            this.max = max;
            return this;
        }

        /**
         * <code>true</code> if this anchor is instantiated with its
         * "enabled" value set to false by default, which will make it
         * invisible and disable interactions with it.
         * @return
         */
        public Builder hiddenByDefault() {
            this.enabled = false;
            return this;
        }

        public AnchorArray.Builder make() {
            prev.add(this);
            return prev;
        }

        protected AnchorPoint instantiate(BlockPos pos, int index) {
            return new AnchorPoint(pos, index, offx, offy, offz, size, enabled, max);
        }
    }
}
