package com.quattage.mechano.foundation.electricity.system;

import net.minecraft.core.BlockPos;

/***
 * A SystemLink represents a connection between two SystemNodes
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
