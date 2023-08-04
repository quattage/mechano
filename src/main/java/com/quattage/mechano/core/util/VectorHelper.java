package com.quattage.mechano.core.util;

import org.joml.Vector3f;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class VectorHelper {

    public static Vec3i round(Vec3 vec) {
        return new Vec3i((int)vec.x, (int)vec.y, (int)vec.z);
    }

    public static Vec3 toVec(Vector3f vec) {
        return new Vec3(vec);
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

    public static BlockPos toBlockPos(Vec3 vec) {
        return new BlockPos(
            (int)vec.x, 
            (int)vec.y, 
            (int)vec.z
        );
    }

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

    public static double drip2(double a, double x, double d, double h) {
        double p1 = a * aSinh((h / (2d * a)) * (1d / Math.sinh(d / (2d * a))));
        double p2 = -a * Math.cosh((2d * p1 - d) / (2d * a));
        return p2 + a * Math.cosh((((2d * x) + (2d * p1)) - d) / (2d * a));
    }

    public static double drip2prime(double x, double d, double h) {
        return drip2prime(1, x, d, h);
    }

    public static double drip2(double x, double d, double h) {
        return drip2(1, x, d, h);
    }

    private static double aSinh(double r) {
        return Math.log(r + Math.sqrt(r * r + 1.0));
    }

    public static Vec3 middleOf(Vec3 a, Vec3 b) {
        double x = (a.x - b.x) / 2d + b.x;
        double y = (a.y - b.y) / 2d + b.y;
        double z = (a.z - b.z) / 2d + b.z;
        return new Vec3(x, y, z);
    }

    public static float getMagnitude(Vec3 vec) {
        return (float) Math.sqrt(Math.pow(vec.x, 2) + Math.pow(vec.y, 2) + Math.pow(vec.z, 2));
    }

    public static float getGreaterMagnitude(Vec3 vec1, Vec3 vec2) {
        float fT = getMagnitude(vec1);
        float tF = getMagnitude(vec2);

        return fT > tF ? fT : tF;
    }

    public static float distanceBetween(Vector3f vec1, Vector3f vec2) {
        return (float)new Vec3(vec1).distanceTo(new Vec3(vec2));
    }

    public static float getLength(Vector3f vec) {
        return (float)Math.sqrt(Math.fma(vec.x(), vec.x(), Math.fma(vec.y(), vec.y(), vec.z() * vec.z())));
    }

    public static float estimateDeltaX(float s, float k) {
        return (float)(s / Math.sqrt(1 + k * k));
    }

    public static Vector3f normalizeToLength(Vector3f vec, float length) {
        float scalar = (float)(Mth.fastInvSqrt(Math.fma(vec.x(), vec.x(), Math.fma(vec.y(), vec.y(), vec.z() * vec.z()))) * length);
        return new Vector3f(
            vec.x() * scalar,
            vec.y() * scalar,
            vec.z() * scalar
        );     
    }
}
