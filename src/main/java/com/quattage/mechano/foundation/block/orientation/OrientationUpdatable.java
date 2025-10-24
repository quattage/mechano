package com.quattage.mechano.foundation.block.orientation;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Indicates that implementing objects represent face-specific data that
 * rotates with the BlockState of the voxel itself.
 */
public interface OrientationUpdatable {
    /**
     * Updates the oriented data within this object to
     * match the orientation described by the given BlockState.
     * @param state state to extract orientation data
     */
    public default void updateOrientation(BlockState state) {
        updateOrientation(state);
    }
    /**
     * Updates the oriented data within this object to
     * match the given orientation
     * @param dir Orientation to use when transforming this AnchorPoint
     */
    public abstract void updateOrientation(CombinedOrientation dir);
}
