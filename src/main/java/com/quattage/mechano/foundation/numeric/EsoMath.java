package com.quattage.mechano.foundation.numeric;

import net.minecraft.core.Vec3i;

public class EsoMath {

    public static long hash64(Object obj) {
        if(obj instanceof Vec3i vi) return EsoMath.hash64(vi.getX(), vi.getY(), vi.getZ());
        return EsoMath.hash64(System.identityHashCode(obj));
    }
    
    public static long hash64(int... members) {
        // start with random seed
        long h = 712857823;
        for(int x = 0; x < members.length; x++)
            h = h * 31 + members[x];
        h ^= (h >>> 33);
        // shift pseudorandomly to achieve overflow distribution
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);
        return h;
    }

    private static final float easeconst = (2f * (float)Math.PI) / 3f;
    public static float easeInElastic(float x) {
        return x == 0f ? 0f : x == 1f ? 1f : (float)Math.pow(2f, -10f * x * Math.sin((x * 10f - 0.75f) * EsoMath.easeconst)) + 1f;
    }
    public static float easeOutElastic(float x) {
        return x == 0f ? 0f : x == 1f ? 1f : (float)-Math.pow(2f, 10f * x - 10) * (float)Math.sin((x * 10f - 10.75f) * EsoMath.easeconst);
    }
}
