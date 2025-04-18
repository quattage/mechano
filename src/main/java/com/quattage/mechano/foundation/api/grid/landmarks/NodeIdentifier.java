package com.quattage.mechano.foundation.api.grid.landmarks;


import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public abstract class NodeIdentifier<T> implements NodeIdentifiable<T> {

    protected BlockPos pos;
    protected byte index;

    public NodeIdentifier(BlockPos pos, int index) {
        this.pos = pos;
        this.index = (byte)(index < 0 ? 0 : (index > 7 ? 7 : index));
    }

    public NodeIdentifier(int x, int y, int z, int index) {
        this.pos = new BlockPos(x, y, z);
        this.index = (byte)(index < 0 ? 0 : (index > 7 ? 7 : index));
    }

    @Override
    public BlockPos getPos() {
        return pos;
    }

    public String toString() {
        return "(" + getX() + "," + getY() + "," + getZ() + "," + getIndex() + ")";
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
        CompoundTag output = new CompoundTag();
        output.putInt("x", getX());
        output.putInt("y", getY());
        output.putInt("z", getZ());
        output.putByte("i", index);
        return output;
    }

    public CompoundTag writeOnlyAddress(CompoundTag tag) {
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        tag.putInt("i", index);
        return tag;
    }
}
