package com.quattage.mechano.api.switchboard.task;

import java.util.Set;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.component.StampingComponent;
import com.quattage.mechano.api.grid.topology.NodeUnionSet.NodePair;
import com.quattage.mechano.api.grid.topology.landmark.Node;
import com.quattage.mechano.api.switchboard.action.GridAction;

public class ComponentCreateTask extends DummyTask {
    @Override
    public GridAction executeTopological(ServerGrid grid, Set<Node> removedNodes, Set<NodePair> disjoints, Object[] args) {
        CircuitComponent component = (CircuitComponent) args[0];
        component.forEachNode(node -> {
            if(!node.hasTerminals()) return;
            grid.netlist().add(node);
            node.forEachTerminal(term -> {
                if(term.getParentConstruct() instanceof StampingComponent sc)
                grid.indexer().allocate(sc);
            });
        });
        return GridAction.RESPONSE_SUCCESS;
    }
}
