package com.quattage.mechano.api.switchboard.task;

import java.util.Set;

import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.GridTracking;
import com.quattage.mechano.api.grid.GridUUID;
import com.quattage.mechano.api.grid.topology.NodeUnionSet.NodePair;
import com.quattage.mechano.api.grid.topology.landmark.AncillaryPair;
import com.quattage.mechano.api.grid.topology.landmark.Node;
import com.quattage.mechano.api.switchboard.action.GridAction;

import io.netty.buffer.ByteBuf;

public class NodeLinkDestroyTask extends NodeLinkCreateTask {

    @Override
    public Class<?>[] getArgumentTemplate() {
        return new Class<?>[] {
            GridUUID.class,
            GridUUID.class
        };
    }

    @Override
    public void dynamicEncode(Object[] args, ByteBuf buffer) {
        GridTracking.write((GridUUID<?>)args[0], buffer);
        GridTracking.write((GridUUID<?>)args[1], buffer);
    }

    @Override
    public Object[] dynamicDecode(ByteBuf buffer) {
        return new Object[] {
            GridTracking.read(buffer),
            GridTracking.read(buffer)
        };
    }

    @Override
    public GridAction executeAsClient(ClientGrid grid, Object... args) {
        GridUUID<?> startID = (GridUUID<?>)args[0];
        GridUUID<?> endID = (GridUUID<?>)args[1];
        grid.lookup().removeAsymmetric(grid, startID.copyAndClearBindings(), endID);
        GridAction eR = grid.lookup().removeAsymmetric(grid, endID.copyAndClearBindings(), startID);
        return eR;
    }

    @Override
    public GridAction executeTopological(ServerGrid grid, Set<Node> removedNodes, Set<NodePair> disjoints, Object[] args) {
        AncillaryPair link = (AncillaryPair) args[0];
        disjoints.add(link.asNodePair());
        return GridAction.RESPONSE_SUCCESS;
    }
}
