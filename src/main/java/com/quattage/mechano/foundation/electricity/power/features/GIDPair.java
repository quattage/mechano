package com.quattage.mechano.foundation.electricity.power.features;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;

public class GIDPair {
    private final GID idA;
    private final GID idB;

    public GIDPair(GridVertex vA, GridVertex vB) {
        this.idA = new GID(vA);
        this.idB = new GID(vB);
    }

    public GIDPair(GID idA, GID idB) {
        this.idA = idA.copy();
        this.idB = idB.copy();
    }

    public GIDPair(BlockPos pA, BlockPos pB) {
        this.idA = new GID(pA);
        this.idB = new GID(pB);
    }

    public static GIDPair of(CompoundTag in) {
        return new GIDPair(GID.of(in.getCompound("A")), GID.of(in.getCompound("B")));
    }

    public GID getSideA() {
        return idA;
    }

    public GID getSideB() {
        return idB;
    }

    public boolean contains(GID check) {
        return check.equals(idA) || check.equals(idB);
    }

    public CompoundTag writeTo(CompoundTag in) {
        in.put("a", idA.writeTo(new CompoundTag()));
        in.put("b", idB.writeTo(new CompoundTag()));
        return in;
    }

    public boolean equals(Object o) {
        if(o instanceof GIDPair pO)
            return (idA.equals(pO.idA) && idB.equals(pO.idB)) ||  
                (idB.equals(pO.idA) && idA.equals(pO.idB));
        return false;
    }

    public String toString() {
        return "{" + idA.toString() + " , " + idB.toString() + "}";
    }

    public int hashCode() {
        return (idA.hashCode() + idB.hashCode()) * 31;
    }
}
