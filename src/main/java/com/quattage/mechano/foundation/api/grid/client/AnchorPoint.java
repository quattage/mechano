package com.quattage.mechano.foundation.api.grid.client;

import com.quattage.mechano.foundation.api.grid.landmarks.GridNode.Tracker;
import com.quattage.mechano.foundation.api.grid.landmarks.NodeIdentifier;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * An AnchorPoint is a mirror implementation of the
 * {@link com.quattage.mechano.foundation.api.grid.landmarks.GridNode GridNode},
 * but specifically and only to store transformation and hitbox
 * data for the client. <p> In other words, the AnchorPoint is the medium 
 * through which the player interacts with the GridNode in-game; An AnchorPoint
 * occupies physical space, where the GridNode does not.
 */
public class AnchorPoint extends NodeIdentifier<AnchorPoint> {

    private final byte[] data;
    private AABB hitbox;

    public AnchorPoint(BlockPos pos, int index, float px, float py, float pz) {
        super(pos, index);
        this.data = new byte[]{
            packMeasurement(px),
            packMeasurement(py),
            packMeasurement(pz),
            packScalar(0.5f)
        };
        refreshHitbox();
    }

    public AnchorPoint(BlockPos pos, CompoundTag tag) {
        super(pos, tag.getInt("i"));
        this.data = tag.getByteArray("xyzs");
    }

    @Override
    public CompoundTag writeTo(CompoundTag in) {
        in.putInt("i", index);
        in.putByteArray("xyzs", data);
        return in;
    }

    public Vec3 getPosition() {
        return new Vec3(
            getX() + unpackMeasurement(data[0]),
            getY() + unpackMeasurement(data[1]),
            getZ() + unpackMeasurement(data[2])
        );
    }

    public AABB refreshHitbox() {
        float size = unpackScalar(data[4]);
        Vec3 pos = getPosition();
        hitbox = new AABB(
            pos.x - size,
            pos.y - size,
            pos.z - size,
            pos.x + size,
            pos.y + size,
            pos.z + size
        );
        return hitbox;
    }

    public AABB getHitbox() {
        return this.hitbox;
    }

    private float unpackScalar(byte in) {
        return (in + 128f) / 255f;
    }

    private byte packScalar(float in) {
        return (byte)((Math.max(0, Math.min(1, in)) * 255) - 128);
    }

    private float unpackMeasurement(byte in) {
        return (in + 128) * (48f / 255f) - 16f;
    }

    private byte packMeasurement(float in) {
        in = Math.max(-16, Math.min(in, 32));
        return (byte)((Math.max(-32, Math.min(in, 32)) + 16f) * (255f / 48f) - 128f);
    }

    @Override
    public AnchorPoint getValue() {
        return this;
    }

    @Override
    public Tracker makeTrackable() {
        throw new UnsupportedOperationException("AnchorPoints aren't trackable!");
    }
}
