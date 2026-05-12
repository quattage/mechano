package com.quattage.mechano.switchboard.task;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.grid.GridUUID;
import com.quattage.mechano.grid.api.component.CircuitComponent;
import com.quattage.mechano.switchboard.RemovalLedger;
import com.quattage.mechano.switchboard.action.GridAction;

public class ComponentDestroyTask extends DummyTask {

    @Override
    public @Nullable Class<?>[] getArgumentTemplate() {
        return new Class[] {
            GridUUID.class
        };
    }

    @Override
    public GridAction executeTopological(ServerGrid grid, RemovalLedger removals, Object[] args) {
        CircuitComponent toDestroy = (CircuitComponent) args[0];
        toDestroy.forEachNode(node -> removals.mark(node));
        return GridAction.RESPONSE_SUCCESS;
    }
}
