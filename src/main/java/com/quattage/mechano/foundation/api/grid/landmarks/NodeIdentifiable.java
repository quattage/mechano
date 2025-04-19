package com.quattage.mechano.foundation.api.grid.landmarks;

import com.quattage.mechano.foundation.api.grid.landmarks.GridNode.Tracker;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/**
 * Barebones implementation template for hashables that need to reference
 * a block position and (optionally) an index value. 
 */
public interface NodeIdentifiable<T> {

    public abstract T getValue();
    public abstract BlockPos getPos();
    public abstract int getIndex();

    public abstract CompoundTag writeTo(CompoundTag in);
    public abstract CompoundTag writeOnlyAddress(CompoundTag in);

    public abstract Tracker makeTrackable();

    public default int getX() {
        return getPos().getX();
    }

    public default int getY() {
        return getPos().getY();
    }

    public default int getZ() {
        return getPos().getZ();
    }
}
