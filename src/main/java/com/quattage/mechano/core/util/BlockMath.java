package com.quattage.mechano.core.util;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BlockMath {
    private static final float TOP_OFFSET = 0.2f;

    public static Vec3 getCenter(BlockPos root) {
        return getCenter(getVecFromPos(root));
    }


    public static double getSimpleDistance(BlockPos pos1, BlockPos pos2) {
        return Math.sqrt(Math.pow((pos1.getX() - pos2.getX()), 2) 
                        + Math.pow((pos1.getY() - pos2.getY()), 2) 
                        + Math.pow((pos1.getZ() - pos2.getZ()), 2));
    }

    public static BlockPos getNearestBlock(Vec3 root) {
        Vec3 pos = getCenter(root);
        return new BlockPos(pos);
    }

    @Nullable
    public static Direction getDirectionTo(Level world, BlockPos fromPos, BlockPos centerPos, Axis rotationAxis) {
        List<Direction> directions = getAxisDirections(rotationAxis);
		for (Direction dir : directions) {
            BlockPos checkPos = fromPos.relative(dir);
            if(checkPos.equals(centerPos)) return dir;
        }
        return null;
    }


    public static Direction getWorldlyDirection(BlockPos workingPos, BlockPos originPos) {

        // TODO NOT IMPLEMENTED
        if(workingPos == originPos)
            throw new IllegalArgumentException("cannot compare direction: workingPos and originPos are the same!");
        Axis greatest = Axis.Y;
        BlockPos difference = workingPos.subtract(originPos);
        double avg = average(difference);
        BlockPos average = difference.subtract(new BlockPos(avg, avg, avg));
        Mechano.log("average: " + average);

        return Direction.UP;
    }


    public static double average(BlockPos pos) {
        return (pos.getX() + pos.getY() + pos.getZ()) / 3;
    }


    public static boolean isNegative(Direction dir) {
        return dir == Direction.NORTH || 
            dir == Direction.WEST ||
            dir == Direction.DOWN;
    }

    public static List<Direction> getAxisDirections(Axis axis) {
        List<Direction> out = new ArrayList<Direction>();
        if(axis == Axis.Z) {
            out.add(Direction.DOWN);
            out.add(Direction.EAST);
            out.add(Direction.UP);
            out.add(Direction.WEST);
        } else if(axis == Axis.Y) {
            out.add(Direction.NORTH);
            out.add(Direction.EAST);
            out.add(Direction.SOUTH);
            out.add(Direction.WEST);
        } else {
            out.add(Direction.DOWN);
            out.add(Direction.NORTH);
            out.add(Direction.UP);
            out.add(Direction.SOUTH);
        }
        return out;
    }

    public static Vec3 getCenter(Vec3 root) {
        double x = nearestHalf(root.x);
        double y = nearestHalf(root.y);
        double z = nearestHalf(root.z);
        Vec3 out = new Vec3(x, y, z);
        return out;
    }

    public static Vec3 getTopCenter(BlockPos root) {
        Vec3 out = getCenter(root);
        return out;
    }

    public static Vec3 getVecFromPos(BlockPos pos) {
        return new Vec3(pos.getX(), pos.getY(), pos.getZ());
    }


    /***
     * Obtains the nearest 0.5 from an input.
     * <strong>Does not include whole numbers.</strong>
     * @param d input value
     * @return double rounded to nearest 0.5
     */
    public static double nearestHalf(double d) {
        return Math.floor(d) + 0.5;
    }

    /***
     * Adds randomness to a Vec3 with a variation between 0.3 and -0.3
     * @param pos Input position to modify
     * @return A new Vec3
     */
    public static Vec3 addRandomness(Vec3 pos) {
        return addRandomness(pos, 0.3f);
    }

    /***
     * Adds symmetrical randomness to a Vec3 with a user-defined dispersion value
     * @param pos Input position to modify
     * @param dispersion Strength of the randomness
     * @return A new Vec3
     */
    public static Vec3 addRandomness(Vec3 pos, float dispersion) {
        RandomSource rand = RandomSource.create();
        double randX = getSymRand(dispersion);
        double randY = getSymRand(dispersion);
        double randZ = getSymRand(dispersion);
        return new Vec3(pos.x + randX, pos.y + randY, pos.z + randZ);
    }

    /***
     * A random value whose range is centered on zero. 
     * @param dispersion width of the random range
     * @return a double between -dispersion and + dispersion
     */
    public static double getSymRand(float dispersion) {
        RandomSource rand = RandomSource.create();
        return (rand.nextFloat() * dispersion) - (dispersion / 2);
    }
}
