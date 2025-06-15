
package com.quattage.mechano.foundation.api.landmark.uuid;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.RecordBuilder;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.anchor.AnchorPointHoldable;
import com.quattage.mechano.foundation.api.anchor.DispatchedAnchorNode;
import com.quattage.mechano.foundation.api.landmark.uuid.impl.HeuristicUUID;
import com.quattage.mechano.foundation.helper.VectorHelper;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

/**
 * Barebones implementation template for hashables that need to reference
 * a block position and (optionally) an index value. 
 */
public abstract class GridUUID implements Comparable<GridUUID> {

    public static final int MAX_SHARED_OCCUPANCY = 8; 

    public static int clampIndex(int index) {
        int out = Math.max(0, Math.min(index, MAX_SHARED_OCCUPANCY - 1));
        if(out != index)
            Mechano.LOGGER.warn("Invalid GridUUID index '" + index + "' was clamped to conform to range (0 -> " + MAX_SHARED_OCCUPANCY + ")");
        return out;
    }

    public static boolean isValidIndex(int index) {
        return index >= 0 && index < MAX_SHARED_OCCUPANCY;
    }

    public abstract GridUUIDData getDiscriminatorType();
    public abstract BlockPos getBlockPos();
    public abstract Vec3 getPos();
    public abstract Vec3 getOffsetPos(float ox, float oy, float oz);
    public abstract int getIndex();
    public final GridUUID copy() { return indexedCopy(getIndex()); }
    public abstract GridUUID indexedCopy(int index);

    public abstract @Nullable AnchorPoint getAnchor(ClientLevel world);
    public abstract @Nullable AnchorPointHoldable getHolder(LevelReader world);
    public abstract @Nullable DispatchedAnchorNode getSurrogate(LevelReader world);

    public boolean isApproximately(GridUUID that) {
        return VectorHelper.approxEqual(this.getPos(), that.getPos());
    }

    public boolean hasAnchorIn(ClientLevel world) {
        return getAnchor(world) != null;
    }

    public HeuristicUUID makeTrackable() {
        return new HeuristicUUID(this);
    }

    /**
     * Writes this NodeIdentifiable to a given CompoundTag. 
     * Enables this NodeIdentifiable to be serialized to as
     * arbitrary data in Entities or BlockEntities.
     * @param tag
     */
    public abstract void writeTo(CompoundTag tag);
    /**
     * Writes this NodeIdentifiable to a ButeBuf used by the 
     * {@link GridUUIDData#STREAM_CODEC internal stream codec.}
     * Enables this NodeIdentifiable to be serialized to the network.
     * @param buffer
     */
    public abstract void writeTo(ByteBuf buffer);
    /**
     * Writes this NodeIdentifiable to a RecordBuilder
     * passed by the {@link GridUUIDData#CODEC internal codec.}
     * Enables this NodeIdentifiable to be written to disk.
     * @param ops
     */
    public abstract void writeTo(RecordBuilder<?> builder);

    @Override
    public int compareTo(GridUUID o) {
        return getDiscriminatorType().compareTo(o.getDiscriminatorType());
    }

    @Override
    public String toString() {
        return getDiscriminatorType() + "_uuid[" 
            + getBlockPos().getX() + ", " 
            + getBlockPos().getY() + ", " 
            + getBlockPos().getZ() + ", " 
            + getIndex() 
            + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof GridUUID that)) return false;
        return this.getDiscriminatorType().equals(that.getDiscriminatorType());
    }

    @Override
    public int hashCode() {
        return this.getDiscriminatorType().ordinal();
    }
}
