package com.quattage.mechano.switchboard.task;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.grid.ServerGrid;
import com.quattage.mechano.grid.topology.core.GridUUID;
import com.quattage.mechano.grid.topology.core.MutableComponentReference;
import com.quattage.mechano.switchboard.RemovalLedger;
import com.quattage.mechano.switchboard.action.GridAction;

import net.minecraft.world.entity.Entity;

public class ComponentDestroyTask extends DummyTask {

    @Override
    public @Nullable Class<?>[] getArgumentTemplate() {
        return new Class[] {
            GridUUID.class
        };
    }

    @Override
    public GridAction executeAsServer(ServerGrid grid, Object... args) {
        return GridAction.RESPONSE_SUCCESS;
    }

    @Override
    public GridAction executeDeferred(ServerGrid grid, RemovalLedger outdated,  MutableComponentReference reference, @Nullable Entity caller) {
        grid.removeComponentDeferred(reference.asComponent(), outdated);
        return GridAction.RESPONSE_SUCCESS;
    }
}
