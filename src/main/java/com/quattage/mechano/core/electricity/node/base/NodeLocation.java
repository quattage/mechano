package com.quattage.mechano.core.electricity.node.base;

import com.mrh0.createaddition.energy.LocalNode;
import com.quattage.mechano.core.block.orientation.SimpleOrientation;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.core.block.orientation.CombinedOrientation;
import com.quattage.mechano.core.util.BlockMath;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class NodeLocation {

    /***
     * The position (relative to the center of the parent block) of this NodeLocation.
     * This one is automatically calibrated towards the NORTH direction, and cannot be changed.
     */
    private final Vec3 northOffset;

    /***
     * The position (relative to the center of the parent block) of this NodeLocation.
     * As opposed to northOffset, this one is modified continuously depending on the desired
     * facing direction, and uses northOffset as a base to do so.
     */
    private Vec3 directionalOffset;

    /***
     * The position of the parent block, used as a basis to get the in-world, absolute position
     * of this NodeLocation.
     */
    private final BlockPos root;

    /***
     * The hightlighted hitbox when a player looks near this NodeLocation.
     */
    private AABB hitbox;
    
    /***
     * The size of the highlighted hitbox when a player looks near this NodeLocation.
     */
    private float SIZE = 0.1f;

    /***
     * The default direction that this hitbox faces when it is initialzied. 
     * Usually this will match the defaultBlockState of the parent block. If you don't
     * want this behavior for any reason, just make sure this matches the direction 
     * your model faces in blockbench when you modeled it.
     */
    private final Direction defaultFacing;

    /***
     * The current direction that this hitbox is facing.
     */
    private Direction currentDirection;

    /***
     * Create a new NodeLocation. Uses full integers to derive a posiitonal offset out of 16.
     * Numbers greater than 16 or less than 0 are allowed.
     * @param x
     * @param y
     * @param z
     * @param defaultFacing Whatever direction that you're using to get the numbers should go here. Usually
     * I'd reccomend using the parent block's defaultBlockState, but this may not always be what you want.
     */
    public NodeLocation(BlockPos root, int x, int y, int z, Direction defaultFacing) {
        Vec3 offset = new Vec3(vecToPixel(x), vecToPixel(y), vecToPixel(z));
        this.root = root;
        this.defaultFacing = defaultFacing;
        this.northOffset = calibrateToNorth(offset, defaultFacing);
        this.directionalOffset = northOffset;
        this.currentDirection = Direction.NORTH;
        updateHitbox();
    }

    /***
     * Create a new NodeLocation. Uses doubles rounded to the nearest 0.25 to derive a positional
     * offset out of 16. Numbers greater than 16 or less than 0 are allowed.
     * @param x 
     * @param y
     * @param z
     * @param defaultFacing Whatever direction your model is facing in Blockbench should go here. Usually this
     * matches up with the parent block's defaultBlockState, but this may not always be what you want.
     */
    public NodeLocation(BlockPos root, double x, double y, double z, Direction defaultFacing) {
        Vec3 offset = new Vec3(vecToPixel(x), vecToPixel(y), vecToPixel(z));
        this.root = root;
        this.defaultFacing = defaultFacing;
        this.northOffset = calibrateToNorth(offset, defaultFacing);
        this.directionalOffset = northOffset;
        updateHitbox();
    }

    public CompoundTag writeTo(CompoundTag in) {
        in.putDouble("xOffset", northOffset.x);
        in.putDouble("yOffset", northOffset.y);
        in.putDouble("zOffset", northOffset.z);
        return in;
    }

    /***
     * Generates a new hitbox from this NodeLocation's x, y, & z
     * @return 
     */
    public AABB boxFromOffset() {
        Vec3 raw = get();
        return new AABB (
            raw.x - SIZE, raw.y - SIZE, raw.z - SIZE, 
            SIZE + raw.x, SIZE + raw.y, SIZE + raw.z
        );
    }

    private void updateHitbox() {
        this.hitbox = boxFromOffset();
    }

    /***
     * Converts a given Vec3 to its equivalent in the North direction.
     * This is used automatically by NodeLocation to calibrate your 
     * offset after it's stored.
     * @return
     */
    private Vec3 calibrateToNorth(Vec3 pos, Direction dir) {
        switch(dir) {
            case NORTH:
                break; // We're trying to make the offset north-oriented. If it already is, nothing needs to happen.
            case DOWN:
                return new Vec3(-pos.y, -pos.z, pos.x);
            case EAST:
                return new Vec3(pos.z, pos.y, pos.x);
            case SOUTH:
                return new Vec3(-pos.x, pos.y, pos.z);
            case UP:
                return new Vec3(pos.y, pos.z, pos.x);
            case WEST:
                return new Vec3(-pos.z, pos.y, pos.x);
        }
        return new Vec3(pos.x, pos.y, pos.z);
    }

    /*** 
     * Converts an integer value representing a pixel measurement to a Vector measurement.
     * @param x 
    */
    public static double vecToPixel(double x) {
        return (0.0625 * x);
    }

    /***
     * Rotate this NodeLocation to face a given Direction.
     * <strong>Node: This method never modifies the NodeLocation's root itself,
     * but rather a copy of the NodeLocation's root. This means that you can't
     * stack rotations. This is intentional, because rotating a NodeLocation multiple times 
     * would cause it to break. </strong>
     * @param dir Direction to rotate
     * @return this NodeLocation after rotating
     */
    public NodeLocation rotate(Direction dir) {
        directionalOffset = getRotated(northOffset, dir);
        return this;
    }

    /***
     * Rotate this NodeLocation to face a given StrictComplexDirection.
     * <strong>Node: This method never modifies the NodeLocation's root itself,
     * but rather a copy of the NodeLocation's root. This means that you can't
     * stack rotations. This is intentional, because rotating a NodeLocation multiple times 
     * would cause it to break. </strong>
     * @param dir 
     * @return this NodeLocation after rotating
     */
    public NodeLocation rotate(CombinedOrientation cDir) {
        Vec3 localUpOffset = getRotated(northOffset, cDir.getLocalUp());
        directionalOffset = getRotated(localUpOffset, cDir.getLocalForward());
        return this;
    }

    private Vec3 getRotated(Vec3 vec, Direction dir) {
        currentDirection = dir;
        switch(dir) {
            case NORTH:
                return new Vec3(
                    vec.x,
                    vec.y,
                    vec.z
                );
            case EAST:
                return new Vec3(
                    vec.z,
                    vec.y,
                    vec.x
                );
            case SOUTH:
                return new Vec3(
                    -vec.x,
                    vec.y,
                    vec.z
                );
            case WEST:
                return new Vec3(
                    vec.z,
                    vec.y,
                    -vec.x
                );
            case UP:
                return new Vec3(
                    vec.z,
                    vec.x,
                    vec.y
                );
            case DOWN:
                return new Vec3(
                    vec.z,
                    -vec.x,
                    -vec.y
                );
        }
        return new Vec3(
            vec.x,
            vec.y,
            vec.z
        );
    }

    public Direction getDefaultFacing() {
        return defaultFacing;
    }

    public AABB getHitbox() {
        updateHitbox();
        return hitbox;
    }

    /***
     * Returns a new Vec3 representing this node's location relative to the world.
     * @return A new Vec3 which is the sum of this NodeLocation's root position and directional offset.
     */
    public Vec3 get() {
        return new Vec3(directionalOffset.x + root.getX(), directionalOffset.y + root.getY(), directionalOffset.z + root.getZ());
    }

    /***
     * Returns the Vec3 of this NodeLoation's live offset position.
     * @return Vec3, relative to the center of the parent block.
     */
    public Vec3 getDirectionalOffset() {
        return directionalOffset;
    }

    /***
     * Returns the Vec3 of this NodeLoation's offset when facing north.
     * @return Vec3, relative to the center of the parent block.
     */
    public Vec3 getBaseOffset() {
        return northOffset;
    }

    /***
     * Returns the root position of this NodeLocation.
     * @return BlockPos of this NodeLocation's parent block
     */
    public BlockPos getRoot() {
        return root;
    }

    public Direction getCurrentDirection() {
        return currentDirection;
    }

    /***
     * A helper that returns this NodeLocation's location formatted as a string
     * @return
     */
    public String locationAsString() {
        Vec3 raw = get();
        return raw.x + ", " + raw.y + ", " + raw.z;
    }

    public String toString() {
        return "Location: [" + locationAsString() + " | " + currentDirection + "]";
    }
}
