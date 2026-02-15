package com.quattage.mechano.api.switchboard.task;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.GridUUID;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.switchboard.TopologyProcessQueue.RemovalCache;
import com.quattage.mechano.api.switchboard.action.GridAction;

public class ComponentDestroyTask extends DummyTask {

    @Override
    public @Nullable Class<?>[] getArgumentTemplate() {
        return new Class[] {
            GridUUID.class
        };
    }

    @Override
    public GridAction executeTopological(ServerGrid grid, RemovalCache removals, Object[] args) {
        CircuitComponent toDestroy = (CircuitComponent) args[0];
        toDestroy.forEachNode(node -> removals.mark(node));
        return GridAction.RESPONSE_SUCCESS;
    }
}
