package com.quattage.mechano.foundation.electricity.system;

import com.quattage.mechano.Mechano;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.core.BlockPos;

/***
 * SVID (System Vertex ID) is a (not really) lightweight class for downmixing a SystemVertex 
 * object into a simple String for quick storage in datasets that require hashing.
 */
public class SVID {
    private final String ID;
    
    public SVID(SystemVertex vert) {
        if(vert == null) throw new NullPointerException("Failed to create SVID - SystemVertex is null!");
        BlockPos vP = vert.getPos();
        this.ID = vP.getX() + "," + vP.getY() + "," + vP.getZ() + "," + vert.getSubIndex();
    }

    public SVID(BlockPos pos, int subIndex) {
        if(pos == null) throw new NullPointerException("Failed to create SVID - BlockPos is null!");
        this.ID = pos.getX() + "," + pos.getY() + "," + pos.getZ() + "," + subIndex;
    }

    public String toString() {
        return "[" + ID + "]";
    }

    public SystemVertex toVertex() {
        String[] split = ID.split(",");
        BlockPos pos = new BlockPos(
            pull(split[0]), 
            pull(split[1]),
            pull(split[2])
        );
        return new SystemVertex(pos, pull(split[3]));
    }

    private int pull(String x) {
        return Integer.valueOf(x);
    }

    public boolean equals(Object o) {
        if(o instanceof SVID other) {

            String[] mem1 = this.ID.split(",");
            String[] mem2 = other.ID.split(",");

            if(Integer.valueOf(mem1[3]) >= 0 || Integer.valueOf(mem2[3]) >= 0)
                return this.ID.equals(other.ID);
            return equalsWithoutIndex(mem1, mem2);
        }
        return false;
    }


    private boolean equalsWithoutIndex(String[] mem1, String[] mem2) {
        for(int x = 0; x < 2; x++)
            if(mem1[x] != mem2[x]) return false;
        return true;
    }

    public int hashCode() {
        return ID.hashCode();
    }
}
