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


    /**
     * Serialize all data associated with this NodeIdentifiable
     * @param in The CompoundTag to serialize to
     * @return <code>in</code>, with additional data serialized to it
     * @see {@link NodeIdentifiable#writeOnlyAddress()} for serializing only hashable data
     */
    public abstract CompoundTag writeTo(CompoundTag in);

    /**
     * Serialize <strong>only</strong> the block position and index
     * data o this NodeIdentifiable, and leave out additional data
     * @param in The CompoundTag to serialize to
     * @return <code>in</code>, with additional data serialized to it
     * @see {@link NodeIdentifiable#writeTo()} to serialize all data
     */
    public abstract CompoundTag writeOnlyAddress(CompoundTag in);

    /**
     * @return Returns a new {@link GridNode.Tracker} instance 
     * that cooresponds to this NodeIdentifiable instance.
     * @throws UnsupportedOperationException if this NodeIdentifiable subclass cannot be made trackable
     */
    public abstract Tracker makeTrackable();

    /**
     * @return The X position of this NodeIdentifiable's BlockPos.
     * @see {@link NodeIdentifiable#getPos()}
     */
    public default int getX() {
        return getPos().getX();
    }

    /**
     * @return The Y position of this NodeIdentifiable's BlockPos.
     * @see {@link NodeIdentifiable#getPos()}
     */
    public default int getY() {
        return getPos().getY();
    }

    /**
     * @return The Z position of this NodeIdentifiable's BlockPos.
     * @see {@link NodeIdentifiable#getPos()}
     */
    public default int getZ() {
        return getPos().getZ();
    }

    /**
     * @return The index of this NodeIdentifiable at its internal BlockPos
     */
    public abstract int getIndex();

}
