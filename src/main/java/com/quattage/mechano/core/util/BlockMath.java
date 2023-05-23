package com.quattage.mechano.core.util;

import com.quattage.mechano.Mechano;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public class BlockMath {
    private static final float TOP_OFFSET = 0.2f;

    public static Vec3 getCenter(BlockPos root) {
        return getCenter(getVecFromPos(root));
    }


    public static Vec3 getCenter(Vec3 root) {
        double x = nearestHalf(root.x);
        double y = nearestHalf(root.y);
        double z = nearestHalf(root.z);
        Vec3 out = new Vec3(x, y, z);
        Mechano.log("getting center:  [before: " + root + " after: " + out + "]");
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
