package com.quattage.mechano.foundation.helper;

import java.lang.ref.WeakReference;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.Level;

public interface Worldly {

    /**
     * Compares both worlds based on their reference
     * and the name of their dimension. If either world
     * world is null, returns <code>false</code>
     * @param worldA
     * @param worldB
     * @return <code>true</code> if both worlds are the same, or if their dimensions share the same name.
     */
    public static boolean areWorldsEqual(Level worldA, Level worldB) {
        if(worldA == null || worldB == null) return false;
        return worldA == worldB || worldA.dimension().location().equals(worldB.dimension().location());
    }

    public abstract @Nullable Level getWorld();
    public default String getDimensionName() {
        return getWorld() == null ? "__NULL" : getWorld().dimension().location().toString();
    }

    /**
     * A weakly referenced object that contains logic for comparing against a
     * {@link Worldly} object, which considers the dimension of a {@link Level} 
     * when comparing.
     */
    public static class WorldlyReference<T extends Worldly> extends WeakReference<T> {

        public WorldlyReference(T referent) { super(referent); }

        public WorldlyReference<T> emptyCopy() {
            return new WorldlyReference<T>(null);
        }

        /**
         * Determines whether or not this WorldlyReference refers to the given
         * world, stipulating that the world has to bo belong to the same
         * dimension in order to be functionally identical.
         * {@link Worldly}
         * @param worldly
         * @return <code>true</code> if this WorldlyReference 
         */
        public boolean isAttachedTo(Worldly worldly) {
            if(worldly == null) return false;
            return isAttachedTo(worldly.getWorld());
        }

        public boolean isAttachedTo(Level world) {
            if(refersTo(null)) return false;
            return Worldly.areWorldsEqual(get().getWorld(), world);
        }
    }
}
