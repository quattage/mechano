package com.quattage.mechano.core.electricity.node;

import com.mrh0.createaddition.energy.LocalNode;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class NodeLocation {

    private final Vec3 location;
    private final int maxAttachments;

    /***
     * Create a new NodeLocation. Uses full integers to derive a posiitonal offset out of 16.
     * Assumes a default orientation of NORTH, facing UP. You can use BlockBench to figure out 
     * where on your model this anchor should be.
     * @param x
     * @param y
     * @param z
     */
    public NodeLocation(int x, int y, int z) {
        this.location = new Vec3(x / 16, y / 16, z / 16);
        this.maxAttachments = 1;
    }

    public NodeLocation(int x, int y, int z, int maxAttachments) {
        this.location = new Vec3(x / 16, y / 16, z / 16);
        this.maxAttachments = maxAttachments;
    }

    /***
     * Create a new NodeAnchor. Uses floats rounded to the nearest 0.25 to derive a posiitonal offset out of 16.
     * Assumes a default orientation of NORTH, facing UP. You can use BlockBench to figure out 
     * where on your model this anchor should be.
     * @param x
     * @param y
     * @param z
     */
    public NodeLocation(float x, float y, float z) {
        this.location = new Vec3(
            getNearestQuarter(x) / 16, 
            getNearestQuarter(y) / 16,
            getNearestQuarter(z) / 16
        );
        maxAttachments = 1;
    }

    public int getMaxAttachments() {
        return maxAttachments;
    }

    private float getNearestQuarter(float in) {
        return Math.round(in * 4) / 4f;
    }

    public Vec3 get() {
        return get(Direction.NORTH);
    }

    public Vec3 get(Direction dir) {
        
    }

    public NodeLocation transform(Direction dir) {
        return new NodeLocation
    }

    public float getX() {
        return (float)location.x;
    }

    public float getY() {
        return (float)location.y;
    }

    public float getZ() {
        return (float)location.z;
    }

    /***
     * Returns the BlockPos of this node relative to the world.
     * @param pos BlockPos of the parent block
     * @return
     */
    public Vec3 getAbsolutePos(BlockPos pos) {
        return new Vec3(x + pos.getX(), y + pos.getY(), z + pos.getZ());
    }
}
