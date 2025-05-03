package com.quattage.mechano.foundation.api.grid.client;

import javax.annotation.Nullable;

import org.joml.Vector3f;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.PowerGridBlockEntity;
import com.quattage.mechano.foundation.api.grid.ProtocolTransferable;
import com.quattage.mechano.foundation.api.grid.landmarks.GridNode.Tracker;
import com.quattage.mechano.foundation.api.grid.landmarks.NodeIdentifier;
import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.helper.VectorHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * An AnchorPoint is the client-sided mirror implementation of the
 * {@link com.quattage.mechano.foundation.api.grid.landmarks.GridNode GridNode},
 * built specifically to store transformation data. 
 * 
 * <p> The AnchorPoint
 * occupies physical space, where the GridNode does not.
 */
public class AnchorPoint extends NodeIdentifier<AnchorPoint> {

    public static final int VIS_RANGE = 15;

    private byte[] data;

    // the visual cube and hitbox may be null for a brief moment during chunk/world loading before updateOrientation is called
    public @Nullable AABB hitbox;
    public @Nullable VoxelShape shape;

    private boolean enabled;
    private Vector3f offset;

    /**
     * A bitmask for determining what connections this anchor can interact with
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
     * @return <code>true</code> if this AnchorPoint can currently accept more connections
     */
    public boolean hasRoom() {
        return (data[4] + 128) < (data[5] + 128);
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

    /**
     * @return The actual Vec3 position of this AnchorPoint with its offset applied
     */
    public Vec3 getRealPosition() {
        return new Vec3(
            getX() + offset.x,
            getY() + offset.y,
            getZ() + offset.z
        );
    }

    /**
     * This method is used to determine whether or not the provided transfer protocol
     * can be used with this AnchorPoint. Useful for enforcing tiers or types
     * of wires/connectors that should coorespond with one another.
     * @param tfp TransferProtocol to compare
     * @return <code>true</code> if the provided TFP can interact with this AnchorPoint
     */
    public boolean isCompatableWith(ProtocolTransferable tfp) {
        return (bitmask & tfp.bitmask()) != 0;
    }

    private Vector3f getRaw() {
        return new Vector3f(unpackMeasurement(data[0]) / 16f, unpackMeasurement(data[1]) / 16f, unpackMeasurement(data[2]) / 16f);
    }

    /**
     * Updates the location and hitbox of this AnchorPoint
     * to reflect the data contained within the given BlockState
     * @param state state to extract orientation data
     */
    public void updateOrientation(BlockState state) {
        offset = VectorHelper.rotate(getRaw(), DirectionTransformer.extract(state));
        float size = unpackMeasurement(data[3]) / 16f;
        hitbox = new AABB(
            (getX() + offset.x) - size,
            (getY() + offset.y) - size,
            (getZ() + offset.z) - size,
            (getX() + offset.x) + size,
            (getY() + offset.y) + size,
            (getZ() + offset.z) + size
        );
    }

    /**
     * Updates the location and hitbox of this AnchorPoint
     * to reflect the orientation in the provided CombinedOrientation.
     * Automatically rebuilds the hitbox as a result.
     * @param dir Orientation to use when transforming this AnchorPoint
     */
    public void updateOrientation(CombinedOrientation dir) {
        offset = VectorHelper.rotate(getRaw(), dir);
        float size = unpackMeasurement(data[3]) / 16f;
        hitbox = new AABB(
            (getX() + offset.x) - size,
            (getY() + offset.y) - size,
            (getZ() + offset.z) - size,
            (getX() + offset.x) + size,
            (getY() + offset.y) + size,
            (getZ() + offset.z) + size
        );
        shape = Shapes.create(-size, -size, -size, size, size, size);
    }

    /**
     * Rebuild the AABB for this AnchorPoint, which is necessary
     * to reflect a change in orientation or position.
     * @return the AABB that was rebuilt
     */
    public AABB rebuildHitbox() {
        float size = unpackMeasurement(data[3]) / 16f;
        hitbox = new AABB(
            (getX() + offset.x) - size,
            (getY() + offset.y) - size,
            (getZ() + offset.z) - size,
            (getX() + offset.x) + size,
            (getY() + offset.y) + size,
            (getZ() + offset.z) + size
        );
        shape = Shapes.create(-size, -size, -size, size, size, size);
        return hitbox;
    }

    private static float unpackMeasurement(byte in) {
        return (in + 128) * (48f / 255f) - 16f;
    }

    private static byte packMeasurement(float in) {
        return (byte)((Math.max(-16, Math.min(in, 32)) + 16f) * (255f / 48f) - 128f);
    }

    /**
     * Gets this AnchorPoint's hitbox. Note that
     * a call to {@link AnchorPoint#rebuildHitbox rebuildHitbox()}
     * must be made at least once to initially populate the hitbox,
     * otherwise this method will return <code>null</code>
     * @return the AABB held by this AnchorPoint
     */
    public @Nullable AABB getHitbox() {
        return this.hitbox;
    }


    @Override
    public AnchorPoint getValue() {
        return this;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    /**
     * Performs an intersection test with this 
     * AnchorPoint's hitbox to determine whether
     * the player is targeting this AnchorPoint
     * @param ray Raycast to test
     * @return <code>true</code> if <code>ray</code> is intersecting this AnchorPoint
     */
    public boolean isIntersecting(VectorHelper.Ray ray) {
        return hitbox.clip(ray.start, ray.end).isPresent();

        // manual intersection test is faster (i think) but doesn't clip on blocks
        // if(ray == null) return false;
        // double tMin = Double.NEGATIVE_INFINITY, tMax = Double.POSITIVE_INFINITY;
        // double tx1 = (hitbox.minX - ray.start().x) / ray.dir().x;
        // double tx2 = (hitbox.maxX - ray.start().x) / ray.dir().x;
        // tMin = Math.max(tMin, Math.min(tx1, tx2));
        // tMax = Math.min(tMax, Math.max(tx1, tx2));
        // double ty1 = (hitbox.minY - ray.start().y) / ray.dir().y;
        // double ty2 = (hitbox.maxY - ray.start().y) / ray.dir().y;
        // tMin = Math.max(tMin, Math.min(ty1, ty2));
        // tMax = Math.min(tMax, Math.max(ty1, ty2));
        // double tz1 = (hitbox.minZ - ray.start().z) / ray.dir().z;
        // double tz2 = (hitbox.maxZ - ray.start().z) / ray.dir().z;
        // tMin = Math.max(tMin, Math.min(tz1, tz2));
        // tMax = Math.min(tMax, Math.max(tz1, tz2));
        // return tMax >= tMin;
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
        private final AnchorPoints.Builder prev;

        public Builder(AnchorPoints.Builder prev) {
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

        public AnchorPoints.Builder make() {
            prev.add(this);
            return prev;
        }

        protected AnchorPoint instantiate(BlockPos pos, int index) {
            return new AnchorPoint(pos, index, offx, offy, offz, size, enabled, max);
        }
    }
}
