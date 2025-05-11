package com.quattage.mechano.foundation.api.landmarks;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.PowerGridBlockEntity;
import com.quattage.mechano.foundation.api.landmarks.GridNode.Tracker;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Barebones implementation template for hashables that need to reference
 * a block position and (optionally) an index value. 
 */
public interface NodeIdentifiable<T> {

    public abstract T getValue();
    public abstract BlockPos getPos();

    /**
     * Checks this NodeIdentifiable instance against the given BlockEntity
     * @param be BlockEntity to check
     * @return <code>true<code> if this BlockEntity hosts this NodeIdentifiable
     */
    public default boolean belongsTo(BlockEntity be) {
        if(!(be instanceof PowerGridBlockEntity pgbe)) return false;
        return belongsTo(pgbe);
    }

    /**
     * Checks this NodeIdentifiable instance against the given PowerGridBlockEntity
     * @param pgbe PowerGridBlockEntity to check
     * @return <code>true<code> if this BlockEntity hosts this NodeIdentifiable
     */
    public default boolean belongsTo(PowerGridBlockEntity pgbe) {
        return getIndex() > -1 && getIndex() < pgbe.anchors.size() && pgbe.getBlockPos().equals(getPos());
    }

    /**
     * Gets the {@link PowerGridBlockEntity} that this NodeIdentifiable points to
     * in the given world. This method will return <code>null</code> if no valid PowerGridBlockEntity
     * could be found at this NodeIdentifible's internal address, which indicates that this
     * NodeIdentifiable is either out of date or was created with bad data.
     * @param world World to operate within
     * @return The {@link PowerGridBlockEntity} that hosts this NodeIdentifiable, or <code>null</code> if this NodeIdentifiable doesn't have one.
     */
    public default @Nullable PowerGridBlockEntity getHost(LevelReader world) {
        BlockEntity be = world.getBlockEntity(getPos());
        if(!(be instanceof PowerGridBlockEntity pgbe)) return null;
        return (getIndex() >= 0 && getIndex() < pgbe.anchors.size()) ? pgbe : null;
    }

    /**
     * Serialize all data associated with this NodeIdentifiable
     * @param in The CompoundTag to serialize to
     * @return <code>in</code>, with additional data serialized to it
     * @see {@link NodeIdentifiable#writeOnlyAddress()} for serializing only hashable data
     */
    public default CompoundTag writeTo(CompoundTag in) {
        return writeOnlyAddress(in);
    }

    /**
     * Serialize ONLY the address information associated with this NodeIdentifiable.
     * Distinctly different from {@link NodeIdentifiable#writeTo} in that the CompoundTag
     * provided to this method will not include any extra serializable data.
     * @param in The CompoundTag to serialize to
     * @return <code>in</code>, with address data serialized to it
     * @see {@link NodeIdentifiable#writeTo()} for serializing additional data
     */
    public default CompoundTag writeOnlyAddress(CompoundTag in) {
        in.putInt("x", getX());
        in.putInt("y", getY());
        in.putInt("z", getZ());
        in.putByte("i", (byte)0);
        return in;
    }

    /**
     * @return Returns a new {@link GridNode.Tracker} instance 
     * that cooresponds to this NodeIdentifiable instance.
     * @throws UnsupportedOperationException if this NodeIdentifiable subclass cannot be made trackable
     */
    public abstract Tracker makeTrackable();


    /**
     * @return Returns a new {@link NodeIdentifier.Key} instance that represents the raw
     * address data contained within this NodeIdentifiable, with all additional data stripped.
     * @throws UnsupportedOperationException if this NodeIdentifiable subclass cannot be made into a generic address
     */
    public default NodeIdentifier.Key strip() {
        return new NodeIdentifier.Key(getPos(), getIndex());
    }

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
