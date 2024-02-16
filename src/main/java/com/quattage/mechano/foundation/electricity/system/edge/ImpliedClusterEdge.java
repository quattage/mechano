package com.quattage.mechano.foundation.electricity.system.edge;

import com.quattage.mechano.foundation.electricity.system.GlobalTransferNetwork;
import com.quattage.mechano.foundation.electricity.system.SVID;

import net.minecraft.core.BlockPos;

/***
 * An "implied" connection between all the nodes in a block that contains multiple nodes.
 * Normal connectors only have one node, and don't need this, but some connectors have 2+ connnections
 * This edge is created to simulate a fake connection between all nodes within the same block as needed.
 */
public class ImpliedClusterEdge extends SystemEdge {

    private final BlockPos pos;

    public ImpliedClusterEdge(GlobalTransferNetwork parent, BlockPos pos) {
        // TODO UNFUCK
        super(parent, null); // <<<<<-----------
        this.pos = pos;
    }

    public SVID asSVID() {
        return new SVID(pos, -1);
    }

    @Override
    public boolean rendersWire() {
        return false;
    }

    @Override
    public boolean isReal() {
        return false;
    }

    @Override
    public SVID getSideA() {
        return asSVID();
    }

    @Override
    public SVID getSideB() {
        return asSVID();
    }
    
}
