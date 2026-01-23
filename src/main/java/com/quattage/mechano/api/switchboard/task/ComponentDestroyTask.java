package com.quattage.mechano.api.switchboard.task;

import java.util.Set;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.component.StampingComponent;
import com.quattage.mechano.api.grid.topology.netlist.NodeUnionSet.NodePair;
import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.foundation.Disposable;

public class ComponentDestroyTask extends DummyTask {
    @Override
    public GridAction executeTopological(ServerGrid grid, Set<Node> removedNodes, Set<NodePair> disjoints, Object[] args) {
        CircuitComponent toDestroy = (CircuitComponent) args[0];
        toDestroy.forEachNode(node -> {
            node.forEachTerminal(term -> {
                if(term.getParentConstruct() instanceof StampingComponent sc) {
                    grid.indexer().forget(sc);
                    sc.dispose();
                }
                term.dispose();
            });
            removedNodes.add(node);
        });
        Disposable.dispose(toDestroy);
        return GridAction.RESPONSE_SUCCESS;
    }
}
