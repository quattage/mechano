package com.quattage.mechano.foundation.api.landmark.classifier;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.RecordBuilder;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.anchor.AnchorPointable;
import com.quattage.mechano.foundation.api.anchor.DispatchedAnchorNode;
import com.quattage.mechano.foundation.helper.VectorHelper;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

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

    public abstract UUIDDiscriminator getDiscriminatorType();
    public abstract BlockPos getBlockPos(LevelReader world);
    public abstract Vec3 getPos(LevelReader world);
    public abstract Vec3 getPos(LevelReader world, float pTicks);
    public abstract Vec3 getOffsetPos(LevelReader world, float ox, float oy, float oz);
    public abstract Vec3 getOffsetPos(LevelReader world, float pTicks, float ox, float oy, float oz);
    public abstract int getIndex();
    public final GridUUID copy() { return indexedCopy(getIndex()); }
    public abstract GridUUID indexedCopy(int index);
    public abstract boolean canMoveDynamically();
    public void applyForceToAttachment(LevelReader world, Vec3 force) {}

    public abstract @Nullable AnchorPoint getAnchor(ClientLevel world);
    public abstract @Nullable AnchorPointable<?> getAnchorPoints(LevelReader world);
    public abstract @Nullable DispatchedAnchorNode getSurrogate(LevelReader world);
    public abstract @Nullable IAttachmentHolder getDataHolder(LevelReader world);
    public abstract String describeDataHolder(LevelReader world);
    public abstract float getAttachedSizeFactor(LevelReader world);

    public boolean isAttachedToPlayer(LevelReader world) {
        return false;
    }

    public boolean isApproximately(LevelReader world, GridUUID that) {
        return VectorHelper.approxEqual(this.getPos(world), that.getPos(world));
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
     * {@link UUIDDiscriminator#STREAM_CODEC internal stream codec.}
     * Enables this NodeIdentifiable to be serialized to the network.
     * @param buffer
     */
    public abstract void writeTo(ByteBuf buffer);
    /**
     * Writes this NodeIdentifiable to a RecordBuilder
     * passed by the {@link UUIDDiscriminator#CODEC internal codec.}
     * Enables this NodeIdentifiable to be written to disk.
     * @param ops
     */
    public abstract void writeTo(RecordBuilder<?> builder);

    @Override
    public int compareTo(GridUUID o) {
        return getDiscriminatorType().compareTo(o.getDiscriminatorType());
    }

    public String toString(LevelReader world) {
        return getDiscriminatorType() + "_uuid[" 
            + getBlockPos(world).getX() + ", " 
            + getBlockPos(world).getY() + ", " 
            + getBlockPos(world).getZ() + ", " 
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

    @Override
    public String toString() {
        return "GridUUID[" + getDiscriminatorType().toString() + "]";
    }
}
