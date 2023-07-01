package com.quattage.mechano.core.electricity.node;

import com.mrh0.createaddition.energy.LocalNode;
import com.quattage.mechano.core.placement.ComplexDirection;
import com.quattage.mechano.core.placement.StrictComplexDirection;
import com.quattage.mechano.core.util.BlockMath;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
    private final AABB hitbox;
    
    /***
     * The size of the highlighted hitbox when a player looks near this NodeLocation.
     */
    private static final float SIZE = 0.3f;

    /***
     * The default direction that this hitbox faces when it is initialzied. 
     * Usually this will match the defaultBlockState of the parent block. If you don't
     * want this behavior for any reason, just make sure this matches the direction 
     * your model faces in blockbench when you modeled it.
     */
    private final Direction defaultFacing;

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
        this.hitbox = boxFromOffset();
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
        this.hitbox = boxFromOffset();
    }

    /***
     * Generates a new hitbox from this NodeLocation's x, y, & z
     * @return 
     */
    public AABB boxFromOffset() {
        return new AABB(
            directionalOffset.x - SIZE, directionalOffset.y - SIZE, directionalOffset.z - SIZE, 
            SIZE + directionalOffset.x, SIZE + directionalOffset.y, SIZE + directionalOffset.z
        );
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
     * Converts a vector measurement (centered on the middle of a block) to a p
     * osition measurement relative to the corner of a block.
     * @param x 
    */
    public static double vecToPixel(int x) {
        return -0.5 + (0.0625 * x);
    }

    /*** 
     * Converts a vector measurement (centered on the middle of a block) to a p
     * osition measurement relative to the corner of a block.
     * @param x 
    */
    public static double vecToPixel(double x) {
        return ((Math.round(x * 4) / 4f) / 16) + 0.5;
    }

    /***
     * Rotate this NodeLocation along a given Direciton.
     * @param cDir
     * @return this NodeLocation after rotating
     */
    public NodeLocation rotate(Direction dir) {
        directionalOffset = getRotated(northOffset, dir);
        return this;
    }

    /***
     * Rotate this NodeLocation along a given StrictComplexDirection.
     * @param cDir
     * @return this NodeLocation after rotating
     */
    public NodeLocation rotate(StrictComplexDirection cDir) {
        Vec3 localUpOffset = getRotated(northOffset, cDir.getLocalUp());
        directionalOffset = getRotated(localUpOffset, cDir.getLocalForward());
        return this;
    }

    private Vec3 getRotated(Vec3 vec, Direction dir) {
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
     * Returns a new Vec3 representing this node's location relative to the world.
     * @param dir Direction to rotate this NodeLocation before getting
     * @return A new Vec3 which is the sum of this NodeLocation's root position and directional offset.
     */
    public Vec3 get(Direction dir) {
        rotate(dir);
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
    public Vec3 getNorthOffset() {
        return northOffset;
    }

    /***
     * Returns the root position of this NodeLocation.
     * @return BlockPos of this NodeLocation's parent block
     */
    public BlockPos getRoot() {
        return root;
    }
}
