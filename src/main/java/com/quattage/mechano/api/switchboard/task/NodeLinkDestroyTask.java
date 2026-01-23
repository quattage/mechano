package com.quattage.mechano.api.switchboard.task;

import java.util.Set;

import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.topology.AncillaryPair;
import com.quattage.mechano.api.grid.topology.netlist.NodeUnionSet.NodePair;
import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.api.switchboard.action.GridAction;

import net.minecraft.client.player.LocalPlayer;

public class NodeLinkDestroyTask extends NodeLinkCreateTask {

    @Override
    protected void clientSelfHandle(LocalPlayer player, ClientGrid grid, GridAction serverResult) {
        
    }

    @Override
    public GridAction executeTopological(ServerGrid grid, Set<Node> removedNodes, Set<NodePair> disjoints, Object[] args) {
        AncillaryPair link = (AncillaryPair) args[0];
        grid.removeLink(link);
        disjoints.add(link.asNodePair());
        return GridAction.RESPONSE_SUCCESS;
    }
}
