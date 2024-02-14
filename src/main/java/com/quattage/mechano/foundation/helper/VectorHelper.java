package com.quattage.mechano.foundation.helper;

import org.joml.Vector3f;

import com.quattage.mechano.Mechano;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.utility.Color;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class VectorHelper {

    public static final RandomSource rand = RandomSource.create();

    /***
     * Converts a Vec3 to a Vec3i
     */
    public static Vec3i round(Vec3 vec) {
        return new Vec3i((int)vec.x, (int)vec.y, (int)vec.z);
    }

    /***
     * Converts a Vector3f to a Vec3
     */
    public static Vec3 toVec(Vector3f vec) {
        return new Vec3(vec);
    }

    public static Vec3 getRandomVector(double dispersion) {
        return addRandomness(new Vec3(0, 0, 0), dispersion);
    }

    public static Vec3 addRandomness(Vec3 vec, double dispersion) {
        double randX = getRandom(dispersion);
        double randY = getRandom(dispersion);
        double randZ = getRandom(dispersion);
        return new Vec3(vec.x + randX, vec.y + randY, vec.z + randZ);
    }

    public static double getRandom(double dispersion) {
        return (rand.nextFloat() * dispersion) - (dispersion / 2);
    }

    public static Vec3 toNearestBlockCenter(Vec3 vec) {
        BlockPos converted = toBlockPos(vec);
        Vec3 out = new Vec3(
            converted.getX(), 
            converted.getY(), 
            converted.getZ()
        );
        return out;
    }

    public static Vec3 toVec(BlockPos pos) {
        return new Vec3(pos.getX(), pos.getY(), pos.getZ());
    }

    public static void setDiff(Vector3f root, Vector3f p0, Vector3f p1) {
        root.set(p0.x() - p1.x(), p0.y() - p1.y(), p0.z() - p1.z());
    }

    public static void setDiff(Vector3f root, Vector3f p0, Vector3f p1, float m) {
        root.set((p0.x() - p1.x()) + m, (p0.y() - p1.y()), (p0.z() - p1.z()) + m);
    }

    public static boolean isGreater(Vec3 a, Vec3 b) {
        return a.x > b.x && a.y > b.y && a.z > b.z();
    }

    /***
     * Draws a simple debug box at the given Vec3 position
     */
    public static void drawDebugBox(Vec3 pos) {
        drawDebugBox(pos, toColor(pos), "debug_" + pos.hashCode());
    }

    /***
     * Draws a simple debug box at the given Vec3 position
     */
    public static void drawDebugBox(Vec3 pos, String hash) {
        drawDebugBox(pos, toColor(pos), hash);
    }
    
    /***
     * Draws a simple debug box at the given Vec3 position
     */
    public static void drawDebugBox(Vec3 pos, Color color) {
        drawDebugBox(pos, color, "debug_" + pos.hashCode());
    }

    /***
     * Draws a simple debug box at the given Vec3 position
     */
    public static void drawDebugBox(Vec3 pos, Color color, String hash) {
        CreateClient.OUTLINER.showAABB(hash, VectorHelper.toAABB(pos, 0.2f))
            .disableLineNormals()
            .withFaceTexture(AllSpecialTextures.CUTOUT_CHECKERED)
            .lineWidth(0.06f)
            .colored(color);
    }

    /***
     * Creates a new AABB at the given Vec3.
     * @param pos Vec3 to use as a basis
     * @param s Size of the AABB
     * @return A new AABB at the given Vec3
     */
    public static AABB toAABB(Vec3 pos, float s) {
        Vec3 size = new Vec3(s, s, s);
        return new AABB(pos.subtract(size), pos.add(size));
    }

    
    /***
     * Creates a new AABB at the given BlockPos.
     * @param pos BlockPos to use as a basis
     * @param s Size of the AABB
     * @return A new AABB at the given BlockPos
     */
    public static AABB toAABB(BlockPos pos, float s) {
        return toAABB(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) , s);
    }

    /***
     * Converts an arbitrary Vec3 (non-normalized) into a percentage based color.
     * @param vec Vector 
     * @param variation (Optional, default is 8) Lower numbers result in more noticable 
     * changes in color, requiring smaller changes to the input vector
     * @return A new Color derived from the input vector
     */
    public static Color toColor(Vec3 vec) {
        return toColor(vec, 8);
    }

    /***
     * Converts an arbitrary Vec3 (non-normalized) into a percentage based color.
     * @param vec Vector 
     * @param variation (Optional, default is 8) Lower numbers result in more noticable 
     * changes in color, requiring smaller changes to the input vector
     * @return A new Color derived from the input vector
     */
    public static Color toColor(Vec3 vec, int variation) {
        if(variation < 2) variation = 2;

        Vec3i norm = new Vec3i(
            (int)Math.abs(vec.x % variation),
            (int)Math.abs(vec.y % variation),
            (int)Math.abs(vec.z % variation)
        );

        Color out = new Color(
            (int)((norm.getX() / 8f) * 255),
            (int)((norm.getY() / 8f) * 255),
            (int)((norm.getZ() / 8f) * 255)
        );

        return out;
    }

    /***
     * Casts coordinates of a Vec3 to ints and returns a BlockPos
     * @param vec Vector to cast
     * @return A converted BlockPos
     */
    public static BlockPos toBlockPos(Vec3 vec) {
        return new BlockPos(
            (int)Math.floor(vec.x), 
            (int)Math.floor(vec.y), 
            (int)Math.floor(vec.z)
        );
    }

    /***
     * Casts coordinates of a Vec3 to ints and returns a BlockPos
     * @param vec Vector to cast
     * @return A converted BlockPos
     */
    public static BlockPos toBlockPos(Vector3f vec) {
        return new BlockPos(
            (int)vec.x, 
            (int)vec.y, 
            (int)vec.z
        );
    }

    /***
     * fairy dust magic wizard gnome code, brought to you buy ConnectableChains
     */
    public static double drip2prime(double a, double x, double d, double h) {
        double p1 = a * aSinh((h / (2d * a)) * (1d / Math.sinh(d / (2d * a))));
        return Math.sinh((2 * x + 2 * p1 - d) / (2 * a));
    }

    /***
     * fairy dust magic wizard gnome code, brought to you buy ConnectableChains
     */
    public static double drip2(double a, double x, double d, double h) {
        double p1 = a * aSinh((h / (2d * a)) * (1d / Math.sinh(d / (2d * a))));
        double p2 = -a * Math.cosh((2d * p1 - d) / (2d * a));
        return p2 + a * Math.cosh((((2d * x) + (2d * p1)) - d) / (2d * a));
    }

    /***
     * fairy dust magic wizard gnome code, brought to you buy ConnectableChains
     */
    public static double drip2prime(double x, double d, double h) {
        return drip2prime(1, x, d, h);
    }

    /***
     * fairy dust magic wizard gnome code, brought to you buy ConnectableChains
     */
    public static double drip2(double x, double d, double h) {
        return drip2(1, x, d, h);
    }

    /***
     * fairy dust magic wizard gnome code, brought to you buy ConnectableChains
     */
    private static double aSinh(double r) {
        return Math.log(r + Math.sqrt(r * r + 1.0));
    }

    public static Vec3 middleOf(Vec3 a, Vec3 b) {
        double x = (a.x - b.x) / 2d + b.x;
        double y = (a.y - b.y) / 2d + b.y;
        double z = (a.z - b.z) / 2d + b.z;
        return new Vec3(x, y, z);
    }

    public static double getMagnitude(Vec3 vec) {
        double x = vec.x == 0 ? 0.001: vec.x;
        double y = vec.y == 0 ? 0.001: vec.y;
        double z = vec.z == 0 ? 0.001 : vec.z;        
        return Math.sqrt(Math.pow(x, 2.0) + Math.pow(y, 2.0) + Math.pow(z, 2.0));
    }

    public static double getNorm(Vec3 vec) {
        return Math.sqrt(vec.x * vec.x + vec.y * vec.y + vec.z * vec.z);
    } 

    public static double getGreaterMagnitude(Vec3 vec1, Vec3 vec2) {
        double fT = getMagnitude(vec1);
        double tF = getMagnitude(vec2);

        return fT > tF ? fT : tF;
    }

    public static float getLength(Vector3f vec) {
        return (float)Math.sqrt(Math.fma(vec.x(), vec.x(), Math.fma(vec.y(), vec.y(), vec.z() * vec.z())));
    }

    public static float estimateDeltaX(float s, float k) {
        return (float)(s / Math.sqrt(1 + k * k));
    }

    /***
     * Identical to Vec3.normalize() in principal, but 
     * with slightly more control when needed.
     * @param vec Vector to normalize
     * @param magnitude Magnitude to normalize to
     * @return a normalized Vector3f.
     */
    public static Vector3f normalizeToLength(Vector3f vec, float magnitude) {
        float scalar = (float)(Mth.fastInvSqrt(Math.fma(vec.x(), vec.x(), Math.fma(vec.y(), vec.y(), vec.z() * vec.z()))) * magnitude);
        return new Vector3f(
            vec.x() * scalar,
            vec.y() * scalar,
            vec.z() * scalar
        );     
    }

    /***
     * A sloppy distance calculator that just converts Vector3fs
     * to Vec3s before calculating distance. Don't use this.
     */
    public static float distanceBetween(Vector3f vec1, Vector3f vec2) {
        return (float)new Vec3(vec1).distanceTo(new Vec3(vec2));
        // TODO don't be lazy
    }

    /***
     * Gets the HitResult for the given player.
     * @param player 
     * @return HitResult describing the player's absolute look position.
     */
    public static HitResult getLookingRay(Player player) {
        return getLookingRay(player, 20);
    }

    /***
     * Gets the HitResult for the given player.
     * @param player 
     * @param dist
     * @return HitResult describing the player's absolute look position.
     */
    public static HitResult getLookingRay(Player player, float dist) {
        Vec3 viewDir = player.getViewVector(1f);
    
        Vec3 start = player.getEyePosition(1f);
        Vec3 end = start.add(viewDir.x * dist, viewDir.y * dist, viewDir.z * dist);

        return player.getCommandSenderWorld().clip(
            new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)
        );
    }

    // i cannot find an equivalent way to do this properly with joml or minecraft math so this is here now
    public static double[][] calculateRotationMatrix(Vec3 axis) {
        double cosTheta = Math.cos(getNorm(axis));
        double sinTheta = Math.sin(getNorm(axis));

        double x = axis.x;
        double y = axis.y;
        double z = axis.z;

        double[][] rotationMatrix = new double[3][3];

        rotationMatrix[0][0] = cosTheta + x * x * (1 - cosTheta);
        rotationMatrix[0][1] = x * y * (1 - cosTheta) - z * sinTheta;
        rotationMatrix[0][2] = x * z * (1 - cosTheta) + y * sinTheta;

        rotationMatrix[1][0] = y * x * (1 - cosTheta) + z * sinTheta;
        rotationMatrix[1][1] = cosTheta + y * y * (1 - cosTheta);
        rotationMatrix[1][2] = y * z * (1 - cosTheta) - x * sinTheta;

        rotationMatrix[2][0] = z * x * (1 - cosTheta) - y * sinTheta;
        rotationMatrix[2][1] = z * y * (1 - cosTheta) + x * sinTheta;
        rotationMatrix[2][2] = cosTheta + z * z * (1 - cosTheta);

        return rotationMatrix;
    }

    public static double[] multiplyMatrixVector(double[][] matrix, double[] vector) {
        double[] result = new double[3];

        for (int i = 0; i < 3; i++) {
            result[i] = 0;
            for (int j = 0; j < 3; j++) {
                result[i] += matrix[i][j] * vector[j];
            }
        }

        return result;
    }

    public static String asString(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }
}