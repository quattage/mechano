package com.quattage.mechano.foundation.electricity.system;

import com.quattage.mechano.foundation.electricity.NodeBank;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/***
 * A SystemVertex is a functional approximation of a NodeBank.
 * Its main purpose is to store a BlockPos representing the location of a NodeBank.
 * It also stores an index, which is usually -1.
 * The index is used to locate the specific ElectricNode that this SystemVertex refers to
 * when such a distinction is required.
 */
public class SystemVertex {
    private final BlockPos pos;
    private final int subIndex;

    // for when subIndex doesn't matter
    public SystemVertex(BlockPos pos) {
        this.pos = pos;
        this.subIndex = -1;
    }

    public SystemVertex(BlockPos pos, int subIndex) {
        this.pos = pos;
        this.subIndex = subIndex;
    }

    public SystemVertex(NodeBank<?> bank) {
        this.pos = bank.target.getBlockPos();
        this.subIndex = -1;
    }

    public SystemVertex(CompoundTag in) {
        this.pos = new BlockPos(
            in.getInt("x"),
            in.getInt("y"),
            in.getInt("z"));
        this.subIndex = in.getInt("i");
    }

    public CompoundTag writeTo(CompoundTag in) {
        in.putInt("x", pos.getX());
        in.putInt("y", pos.getY());
        in.putInt("z", pos.getZ());
        in.putInt("i", subIndex);
        return in;
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getSubIndex() {
        return subIndex;
    }

    public SystemVertex copyAndModify(BlockPos newPos) {
        return new SystemVertex(newPos, this.subIndex);
    }

    public SystemVertex copyAndModify(int newIndex) {
        return new SystemVertex(this.pos, newIndex);
    }

    public boolean equals(Object other) {
        if(other instanceof SystemVertex sl) {
            if(this.subIndex == -1 || sl.getSubIndex() == -1) 
                return this.pos.equals(sl.pos);
            return this.pos.equals(sl.pos) && this.subIndex == sl.subIndex;
        }
        return false;
    }

    public int hashCode() {
        if(subIndex < 0) return pos.hashCode();
        return pos.hashCode() + subIndex;
    }

    public String toString() {
        return "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ", i" + subIndex + "]";
    }
}
