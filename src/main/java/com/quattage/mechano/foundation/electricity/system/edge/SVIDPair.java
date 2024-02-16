package com.quattage.mechano.foundation.electricity.system.edge;

import com.quattage.mechano.foundation.electricity.system.GlobalTransferNetwork;
import com.quattage.mechano.foundation.electricity.system.SVID;
import com.quattage.mechano.foundation.electricity.system.SystemVertex;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

/***
 * An SVID pair is literally just 2 SVIDs, which is used for hashing edges that need access to SVIDs on both ends.
 */
public class SVIDPair {
    private final SVID idA;
    private final SVID idB;

    public SVIDPair(SystemVertex vA, SystemVertex vB) {
        this.idA = new SVID(vA);
        this.idB = new SVID(vB);
    }

    public SVIDPair(SVID idA, SVID idB) {
        this.idA = idA.copy();
        this.idB = idB.copy();
    }

    public SVIDPair(BlockPos pA, BlockPos pB) {
        this.idA = new SVID(pA);
        this.idB = new SVID(pB);
    }

    public SVID getSideA() {
        return idA;
    }

    public SVID getSideB() {
        return idB;
    }

    public boolean contains(SVID check) {
        return check.equals(idA) || check.equals(idB);
    }

    public boolean equals(Object o) {
        if(o instanceof SVIDPair pO)
            return (idA.equals(pO.idA) && idB.equals(pO.idB)) ||  
                (idB.equals(pO.idA) && idA.equals(pO.idB));
        return false;
    }

    public int hashCode() {
        return (idA.hashCode() + idB.hashCode()) * 31;
    }
}
