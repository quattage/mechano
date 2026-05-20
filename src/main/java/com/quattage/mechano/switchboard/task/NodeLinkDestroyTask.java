package com.quattage.mechano.switchboard.task;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.grid.ClientGrid;
import com.quattage.mechano.grid.GridTracking;
import com.quattage.mechano.grid.ServerGrid;
import com.quattage.mechano.grid.topology.core.GridUUID;
import com.quattage.mechano.grid.topology.core.MutableComponentReference;
import com.quattage.mechano.switchboard.RemovalLedger;
import com.quattage.mechano.switchboard.action.GridAction;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.Entity;

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
    public GridAction executeAsServer(ServerGrid grid, Object... args) {
        return GridAction.RESPONSE_SUCCESS;
    }

    @Override
    public GridAction executeDeferred(ServerGrid grid, RemovalLedger outdated, MutableComponentReference reference, @Nullable Entity caller) {
        outdated.mark(grid, reference.asNodePair());
        return GridAction.RESPONSE_SUCCESS;
    }

    @Override
    public GridAction executeAsClient(ClientGrid grid, Object... args) {
        GridUUID<?> startID = (GridUUID<?>)args[0];
        GridUUID<?> endID = (GridUUID<?>)args[1];
        return grid.removeLink(startID.copyAndClearBindings(), endID, null);
    }
}
