package com.quattage.mechano.foundation.numeric;

import java.util.Collection;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * my beautiful util class
 */
public class EsoMath {

    public static long hash64(Object obj) {
        if(obj instanceof Vec3i vi) return EsoMath.hash64(vi.getX(), vi.getY(), vi.getZ());
        return EsoMath.hash64(System.identityHashCode(obj));
    }
    
    public static long hash64(int... members) {
        // start with random seed and acculumate
        long h = 712857823;
        for(int x = 0; x < members.length; x++)
            h = h * 31 + members[x];
        // shift pseudorandomly to achieve overflow distribution
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);
        return h;
    }

    private static final float easeconst = (2f * (float)Math.PI) / 3f;
    public static float easeInElastic(float x) {
        return x <= 0f ? 0f : x >= 1f ? 1f : (float)Math.pow(2f, -10f * x * Math.sin((x * 10f - 0.75f) * EsoMath.easeconst)) + 1f;
    }
    public static float easeOutElastic(float x) {
        return x <= 0f ? 0f : x >= 1f ? 1f : (float)-Math.pow(2f, 10f * x - 10) * (float)Math.sin((x * 10f - 10.75f) * EsoMath.easeconst);
    }

    public static double innerProduct(double[] a, double[] b) {
        double sum = 0;
        for(int x = 0; x < a.length; x++) sum += a[x] * b[x];
        return sum;
    }

    public static long quadShort2Long(short a, short b, short c, short d) {
        return
            ((long)(a & 0xFFFF) << 48) |
            ((long)(b & 0xFFFF) << 32) |
            ((long)(c & 0xFFFF) << 16) |
            ((long)(d & 0xFFFF));
    }

    public static short long2shortA(long l) {
        return (short)(l >> 48);
    }

    public static short long2shortB(long l) {
        return (short)(l >> 32);
    }

    public static short long2shortC(long l) {
        return (short)(l >> 16);
    }

    public static short long2shortD(long l) {
        return (short)(l >> 0);
    }

    public static short[] long2QuadShort(long l) {
        return new short[] {
            EsoMath.long2shortA(l),
            EsoMath.long2shortB(l),
            EsoMath.long2shortC(l),
            EsoMath.long2shortD(l)
        };
    }

    public static int dualShort2Int(short a, short b) {
        return
            ((int)(a & 0xFFFF) << 16) |
            ((int)(b & 0xFFFF));
    }

    public static short int2shortA(int i) {
        return (short)(i >> 16);
    }

    public static short int2shortB(int i) {
        return (short)(i >> 0);
    }

    public static short[] int2DualShort(int i) {
        return new short[] {
            EsoMath.int2shortA(i),
            EsoMath.int2shortB(i)
        };
    }

    public static int randomInt(RandomSource random) {
        return EsoMath.randomInt(random, -64, 64);
    }

    public static int randomInt(RandomSource random, int min, int max) {
        return random.nextInt((max - min) + 1) + min;
    }

    public static short toShortClamped(int x) {
        return (short)Mth.clamp(x, Short.MIN_VALUE, Short.MAX_VALUE);
    }

    public static short toShortClamped(long x) {
        return (short)Mth.clamp(x, Short.MIN_VALUE, Short.MAX_VALUE);
    }

    public static <T extends Object, R extends Collection<T>> R selectiveMerge(@Nullable R a, @Nullable R b) {
        if(a == null && b != null) return b;
        if(b == null && a != null) return a;
        if(a == null && b == null) return null;
        if(a.size() < b.size()) {
            b.addAll(a);
            return b;
        }
        a.addAll(b);
        return a;
    }

    public static <T extends Object, K extends Object, R extends Map<T, K>> R selectiveMerge(@Nullable R a, @Nullable R b) {
        if(a == null && b != null) return b;
        if(b == null && a != null) return a;
        if(a == null && b == null) return null;
        if(a.size() < b.size()) {
            b.putAll(a);
            return b;
        }
        a.putAll(b);
        return a;
    }
}

