package com.quattage.mechano.foundation.electricity.core.node;

import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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
    private final float size;

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
    public NodeLocation(BlockPos root, int x, int y, int z, float size, Direction defaultFacing) {
        Vec3 offset = new Vec3(pixToVec(x), pixToVec(y), pixToVec(z));
        this.root = root;
        this.defaultFacing = defaultFacing;
        this.northOffset = calibrateToNorth(offset, defaultFacing);
        this.directionalOffset = northOffset;
        this.currentDirection = Direction.NORTH;
        this.size = size;
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
    public NodeLocation(BlockPos root, double x, double y, double z, float size, Direction defaultFacing) {
        Vec3 offset = new Vec3(pixToVec(x), pixToVec(y), pixToVec(z));
        this.root = root;
        this.defaultFacing = defaultFacing;
        this.northOffset = calibrateToNorth(offset, defaultFacing);
        this.directionalOffset = northOffset;
        this.currentDirection = Direction.NORTH;
        this.size = size;
        updateHitbox();
    }

    /***
     * Create a new NodeLocation derived from values stored within an NBT tag.
     * This requires your NBT to have a properly formatted compound called "NodeLocation."
     */
    public NodeLocation(BlockEntity target, CompoundTag tag) {
        this.northOffset = new Vec3(
            tag.getDouble("oX"), 
            tag.getDouble("oY"), 
            tag.getDouble("oZ")
        );
        this.size = tag.getFloat("s");
        this.defaultFacing = Direction.NORTH;
        this.root = target.getBlockPos();
        this.directionalOffset = northOffset;

        
        doInitialRotate(target);

        updateHitbox();
    }

    public NodeLocation(NodeLocation other, float size) {
        this.root = other.root;
        this.defaultFacing = other.defaultFacing;
        this.northOffset = other.northOffset;
        this.directionalOffset = other.directionalOffset;
        this.currentDirection = other.currentDirection;
        this.size = size;
    }

    /***
     * Rotates this NodeLocation to where it should be 
     * upon world load.
     */
    public void doInitialRotate(BlockEntity target) {
        BlockState state = target.getBlockState();
        if(state != null) rotate(DirectionTransformer.extract(state));
    }

    public CompoundTag writeTo(CompoundTag in) {
        in.putDouble("oX", northOffset.x);
        in.putDouble("oY", northOffset.y);
        in.putDouble("oZ", northOffset.z);
        in.putFloat("s", size);
        return in;
    }

    /***
     * Generates a new hitbox from this NodeLocation's x, y, & z
     * @return 
     */
    public AABB boxFromOffset() {
        Vec3 raw = get();
        return new AABB (
            raw.x - size, raw.y - size, raw.z - size, 
            size + raw.x, size + raw.y, size + raw.z
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
    public static double pixToVec(double x) {
        return (0.0625 * x);
    }

    public static double vecToPix(double x) {
        return (x / 0.0625);
    }

    /***
     * Rotate this NodeLocation to face a given direction.
     * <strong>Node: This method never modifies the NodeLocation's root itself,
     * but rather a copy of the NodeLocation's root. This means that you can't
     * stack rotations. This is intentional, because rotating a NodeLocation multiple times 
     * would cause it to break. </strong>
     * @param dir CombinedOrientation to use as a basis for rotation.
     * @return this NodeLocation after rotating
     */
    public NodeLocation rotate(CombinedOrientation dir) {
        directionalOffset = getRotated(northOffset, dir);
        return this;
    }

    /***
     * we don't speak of this.
     */
    private Vec3 getRotated(Vec3 vec, CombinedOrientation cDir) {
        currentDirection = cDir.getLocalForward();
        switch(cDir) {
            case DOWN_EAST:
                return new Vec3(
                    iv(vec.z),
                    iv(vec.y),
                    iv(vec.x)
                );
            case DOWN_NORTH:
                return new Vec3(
                    iv(vec.x),
                    iv(vec.y),
                    vec.z
                );
            case DOWN_SOUTH:
                return new Vec3(
                    vec.x,
                    iv(vec.y),
                    iv(vec.z)
                );
            case DOWN_WEST:
                return new Vec3(
                    vec.z,
                    iv(vec.y),
                    vec.x
                );
            case EAST_DOWN:
                return new Vec3(
                    vec.y,
                    vec.z,
                    vec.x
                );
            case EAST_NORTH:
                return new Vec3(
                    vec.y,
                    iv(vec.x),
                    vec.z
                );
            case EAST_SOUTH:
                return new Vec3(
                    vec.y,
                    vec.x,
                    iv(vec.z)
                );
            case EAST_UP:
                return new Vec3(
                    vec.y,
                    iv(vec.z),
                    iv(vec.x)
                );
            case NORTH_DOWN:
                return new Vec3(
                    vec.x,
                    vec.z,
                    iv(vec.y)
                );
            case NORTH_EAST:
                return new Vec3(
                    iv(vec.z),
                    vec.x,
                    iv(vec.y)
                );
            case NORTH_UP:
                return new Vec3(
                    iv(vec.x),
                    iv(vec.z),
                    iv(vec.y)
                );
            case NORTH_WEST:
                return new Vec3(
                    vec.z,
                    iv(vec.x),
                    iv(vec.y)
                );
            case SOUTH_DOWN:
                return new Vec3(
                    iv(vec.x),
                    vec.z,
                    vec.y
                );
            case SOUTH_EAST:
                return new Vec3(
                    iv(vec.z),
                    iv(vec.x),
                    vec.y
                );
            case SOUTH_UP:
                return new Vec3(
                    vec.x,
                    iv(vec.z),
                    vec.y
                );
            case SOUTH_WEST:
                return new Vec3(
                    vec.z,
                    vec.x,
                    vec.y
                );
            case UP_EAST:
                return new Vec3(
                    iv(vec.z),
                    vec.y,
                    vec.x
                );
            case UP_NORTH:
                return new Vec3(
                    vec.x,
                    vec.y,
                    vec.z
                );
            case UP_SOUTH:
                return new Vec3(
                    iv(vec.x),
                    vec.y,
                    iv(vec.z)
                );
            case UP_WEST:
                return new Vec3(
                    vec.z,
                    vec.y,
                    iv(vec.x)
                );
            case WEST_DOWN:
                return new Vec3(
                    iv(vec.y),
                    vec.z,
                    iv(vec.x)
                );
            case WEST_NORTH:
                return new Vec3(
                    iv(vec.y),
                    vec.x,
                    vec.z
                );
            case WEST_SOUTH:
                return new Vec3(
                    iv(vec.y),
                    iv(vec.x),
                    iv(vec.z)
                );
            case WEST_UP:
                return new Vec3(
                    iv(vec.y),
                    iv(vec.z),
                    vec.x
                );
        }
        return new Vec3(
            vec.x,
            vec.y,
            vec.z
        );
    }

    /***
     * Inverts a double ranging from 0 to 1
     * @param in
     * @return
     */
    private double iv(double in) {
        double out = in;
        if(in < 0.5) out = in + ((0.5 - in) * 2);
        if(in > 0.5) out = in + ((0.5 - in) * 2);
        return out;
    }

    public Direction getDefaultFacing() {
        return defaultFacing;
    }

    public AABB getHitbox() {
        updateHitbox();
        return hitbox;
    }

    public float getHitSize() {
        return size;
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
     * Combines both the pixel-converted measurement and the raw measurement
     * into a user-friendly String for debugging
     * @param x double to work with
     * @return A user-friendly String.
     */
    private String describe(double x) {
        return vecToPix(x) + " (" + x + ")";
    }

    /***
     * A helper that returns this NodeLocation's location formatted as a string
     * @return
     */
    public String locationAsString() {
        Vec3 raw = getDirectionalOffset();
        return describe(raw.x) + ", " + describe(raw.y) + ", " + describe(raw.z);
    }

    public String toString() {
        return "Location: [" + locationAsString() + " | " + currentDirection + "]";
    }
}
