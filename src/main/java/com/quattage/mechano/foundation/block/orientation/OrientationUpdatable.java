package com.quattage.mechano.foundation.block.orientation;

import org.joml.Vector3f;

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
    default void updateOrientation(BlockState state) {
        updateOrientation(state);
    }
    /**
     * Updates the oriented data within this object to
     * match the given orientation
     * @param dir Orientation to use when transforming this AnchorPoint
     */
    void updateOrientation(CombinedOrientation dir);

    // TODO impl
    default void updateOrientation(Vector3f rotation) {}
}
