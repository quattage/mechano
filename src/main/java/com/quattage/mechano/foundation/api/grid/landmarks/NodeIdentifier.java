package com.quattage.mechano.foundation.api.grid.landmarks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/**
 * Basic implementation of {@link NodeIdentifiable} that provides hashing, 
 * equivalence, and serialization methods for a block position and an index value.
 * This abstract class is used by {@link GridNode}, {@link GridNode.Address}, {@link GridNode.Tracker},
 * and {@link com.quattage.mechano.foundation.api.grid.client.AnchorPoint AnchorPoint} - All of these classes
 * share the same hashing, equivalence, and serialization methods, so they can all be used to query {@link  com.quattage.mechano.foundation.api.grid.PowerGrid PowerGrid}
 * 
 */
public abstract class NodeIdentifier<T> implements NodeIdentifiable<T> {

    /**
     * The maximum amount of nodes that can occupy the same single block space
     */
    public static final int MAX_OCCUPANCY = 8; 

    protected final BlockPos pos;
    protected final byte index;

    public NodeIdentifier(BlockPos pos, int index) {
        this.pos = pos;
        this.index = (byte)(index < 0 ? 0 : (index >= NodeIdentifier.MAX_OCCUPANCY ? NodeIdentifier.MAX_OCCUPANCY - 1 : index));
    }

    public NodeIdentifier(int x, int y, int z, int index) {
        this.pos = new BlockPos(x, y, z);
        this.index = (byte)(index < 0 ? 0 : (index >= NodeIdentifier.MAX_OCCUPANCY ? NodeIdentifier.MAX_OCCUPANCY - 1 : index));
    }

    @Override
    public BlockPos getPos() {
        return this.pos;
    }

    public String toString() {
        return "(" + getX() + "," + getY() + "," + getZ() + ", " + getIndex() + " / " + NodeIdentifier.MAX_OCCUPANCY + ")";
    }

    @Override
    public int getIndex() {
        return index;
    }

    public abstract T getValue();

    public boolean isLocatedAt(BlockPos pos) {
        return this.pos.equals(pos);
    }

    public boolean isLocatedAt(int x, int y, int z) {
        return this.pos.getX() == x && this.pos.getY() == y && this.pos.getZ() == z;
    }

    public boolean isIndex(int index) {
        return this.index == getIndex();
    }

    public boolean isIndex(byte index) {
        return this.index == index;
    }


    public boolean equals(Object other) {
        if(!(other instanceof NodeIdentifier that)) return false;
        return this.pos.equals(that.pos) && this.index == that.index;
    }

    public int hashCode() {
        return pos.hashCode() * 31 + index;
    }

    public CompoundTag writeTo(CompoundTag in) {
        in.putInt("x", getX());
        in.putInt("y", getY());
        in.putInt("z", getZ());
        in.putByte("i", index);
        return in;
    }

    public CompoundTag writeOnlyAddress(CompoundTag in) {
        return writeTo(in);
    }
}
