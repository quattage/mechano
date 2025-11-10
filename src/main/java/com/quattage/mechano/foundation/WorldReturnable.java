package com.quattage.mechano.foundation;

import java.lang.ref.WeakReference;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public interface WorldReturnable {

    /**
     * Compares both worlds based on their reference
     * and the name of their dimension. If either world
     * world is null, returns <code>false</code>
     * @param worldA
     * @param worldB
     * @return <code>true</code> if both worlds are the same, or if their dimensions share the same name.
     */
    static boolean areWorldsEqual(LevelAccessor worldA, LevelAccessor worldB) {
        if(worldA == null || worldB == null) return false;
        return worldA == worldB || (worldA.isClientSide() == worldB.isClientSide() 
            && worldA.getChunkSource() == worldB.getChunkSource());
    }

    @Nullable Level getWorld();
    default String getDimensionName() {
        return WorldReturnable.getDimensionName(getWorld());
    }

    static String getDimensionName(@Nullable Level world) {
        return world == null ? "NULL" : world.dimension().location().toString();
    }

    /**
     * A weakly referenced object that contains logic for comparing against a
     * {@link WorldReturnable} object, which considers the dimension of a {@link Level} 
     * when comparing.
     */
    public static class WorldlyReference<T extends WorldReturnable> extends WeakReference<T> {

        public WorldlyReference(T referent) { super(referent); }

        public WorldlyReference<T> emptyCopy() {
            return new WorldlyReference<T>(null);
        }

        /**
         * Determines whether or not this WorldlyReference refers to the given
         * world, stipulating that the world has to bo belong to the same
         * dimension in order to be functionally identical.
         * {@link WorldReturnable}
         * @param worldly
         * @return <code>true</code> if this WorldlyReference 
         */
        public boolean isAttachedTo(WorldReturnable worldly) {
            if(worldly == null) return false;
            return isAttachedTo(worldly.getWorld());
        }

        public boolean isAttachedTo(Level world) {
            if(refersTo(null)) return false;
            return WorldReturnable.areWorldsEqual(get().getWorld(), world);
        }

        public boolean isAttachedTo(LevelAccessor world) {
            if(refersTo(null)) return false;
            return WorldReturnable.areWorldsEqual(get().getWorld(), world);
        }
    }
}
