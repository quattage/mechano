package com.quattage.mechano.api.switchboard.task;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.GridUUID;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.component.StampingComponent;
import com.quattage.mechano.api.grid.topology.NodeUnionSet.NodePair;
import com.quattage.mechano.api.grid.topology.landmark.Node;
import com.quattage.mechano.api.switchboard.action.GridAction;

public class ComponentDestroyTask extends DummyTask {

    @Override
    public @Nullable Class<?>[] getArgumentTemplate() {
        return new Class[] {
            GridUUID.class
        };
    }

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
        return GridAction.RESPONSE_SUCCESS;
    }
}
