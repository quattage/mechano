package com.quattage.mechano.foundation.electricity.power.features;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/***
 * GID is used as an identifier for hashing GridVertex objects
 */
public class GID {

    private final BlockPos pos;
    private final int subIndex;
    
    public GID(GridVertex vert) {
        if(vert == null) throw new NullPointerException("Failed to create SVID - SystemVertex is null!");
        this.pos = vert.getPos();
        this.subIndex = vert.getSubIndex();
    }

    public GID(BlockPos pos, int subIndex) {
        if(pos == null) throw new NullPointerException("Failed to create SVID - BlockPos is null!");
        this.pos = pos;
        this.subIndex = subIndex;
    }

    public GID(BlockPos pos) {
        if(pos == null) throw new NullPointerException("Failed to create SVID - BlockPos is null!");
        this.pos = pos;
        this.subIndex = -1;
    }

    public static GID of(CompoundTag nbt) {
        return new GID(new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z")), nbt.getInt("i"));
    }

    public static GID ofLinked(CompoundTag nbt) {
        return of(nbt);
    }

    public static boolean isValidTag(CompoundTag nbt) {
        if(nbt.isEmpty()) return false;
        return nbt.contains("x") && nbt.contains("y") && nbt.contains("z") && nbt.contains("i");
    }

    public String toString() {
        return "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ", " + subIndex + "]";
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getSubIndex() {
        return subIndex;
    }

    public GID copy() {
        return new GID(pos, subIndex);
    }

    public GIDPair combine(GID other) {
        return new GIDPair(this, other);
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof GID other) {
            return other.pos.equals(this.pos) && other.subIndex == this.subIndex;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return (pos.hashCode() + subIndex) * 31;
    }

    public CompoundTag writeTo(CompoundTag in) {
        in.putInt("x", pos.getX());
        in.putInt("y", pos.getY());
        in.putInt("z", pos.getZ());
        in.putInt("i", subIndex);
        return in;
    }
}
