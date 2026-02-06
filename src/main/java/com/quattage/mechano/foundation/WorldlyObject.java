package com.quattage.mechano.foundation;

import java.lang.ref.WeakReference;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkSource;

/**
 * Subclasses of WorldlyObject have some kind of direct or indirect relationship 
 * with the Minecraft {@link Level}. This interface makes it easier to access
 * and compare worlds, even when working with arbitrary objects outside of the 
 * Entity/BlockEntity context.
 */
public interface WorldlyObject {

    /**
     * Compares both worlds based on reference equivalence and their {@link ChunkSource}
     * @param worldA LevelAccessor to compare
     * @param worldB LevelAccessor to compare
     * @return <code>true</code> if both worlds are functionally identical.
     */
    static boolean areWorldsEqual(LevelAccessor worldA, LevelAccessor worldB) {
        if(worldA == null || worldB == null) return false;
        return worldA == worldB || (worldA.isClientSide() == worldB.isClientSide() 
            && worldA.getChunkSource() == worldB.getChunkSource());
    }

    /**
     * Gets a human-readable, non-formatted name for the dimension
     * that the provided <code>world</code> represents. This method is 
     * primarily used for debugging purposes.
     * @param world World to get the name from
     * @return A new String.
     */
    static String getDimensionName(@Nullable Level world) {
        return world == null ? "null" : world.dimension().location().toString();
    }

    /**
     * Gets the {@link Level} associated with this object. The exact relationship
     * that this object has with this world is not known or enforced. Objects may
     * return <code>null</code> to indicate that a world could not be provided, or
     * the object is in some state that prevents the returned world from being valid.
     * Non-null values returned from this method will always be valid, loaded, and
     * reachable.
     * @return The level this object belongs to, or <code>null</code> if this
     * object failed to provide a level for whatever reason. You're likely
     * to encounter null values here if you're trying to get the world
     * from rendering code for the brief period after an entity or block
     * entity has been removed from the world.
     */
    @Nullable Level getWorld();

    /**
     * Gets a human-readable, non-formatted name for the dimension
     * that this object is currently located in. This method is primarily
     * used for debugging purposes.
     * @param world World to get the name from
     * @return A new String.
     */
    default String getDimensionName() {
        return WorldlyObject.getDimensionName(getWorld());
    }

    /**
     * A weakly referenced object that contains logic for comparing against a
     * {@link WorldlyObject} object, which compares worlds for equivalence
     * by reference and {@link ChunkSource}.
     */
    public static class WorldlyReference<T extends WorldlyObject> extends WeakReference<T> {

        public WorldlyReference(T referent) { super(referent); }

        public WorldlyReference<T> emptyCopy() {
            return new WorldlyReference<T>(null);
        }

        /**
         * Determines whether or not this WorldlyReference refers to the given
         * world, stipulating that the world has to belong to the same
         * dimension in order to be functionally identical.
         * {@link WorldlyObject}
         * @param worldly
         * @return <code>true</code> if this WorldlyReference 
         */
        public boolean isAttachedTo(WorldlyObject worldly) {
            if(worldly == null) return false;
            return isAttachedTo(worldly.getWorld());
        }

        public boolean isAttachedTo(Level world) {
            if(refersTo(null)) return false;
            return WorldlyObject.areWorldsEqual(get().getWorld(), world);
        }

        public boolean isAttachedTo(LevelAccessor world) {
            if(refersTo(null)) return false;
            return WorldlyObject.areWorldsEqual(get().getWorld(), world);
        }
    }
}
