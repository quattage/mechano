package com.quattage.mechano.api.switchboard.task;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.GridTracking;
import com.quattage.mechano.api.grid.GridUUID;
import com.quattage.mechano.api.grid.topology.landmark.AncillaryNode;
import com.quattage.mechano.api.grid.topology.landmark.AncillaryPair;
import com.quattage.mechano.api.grid.topology.landmark.ComponentLink;
import com.quattage.mechano.api.switchboard.action.ActionTask;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.api.transmitter.TransmitterType;

import io.netty.buffer.ByteBuf;

public class NodeLinkSyncTask implements ActionTask {

    @Override
    public @Nullable Class<?>[] getArgumentTemplate() {
        return new Class<?>[] {
            GridUUID.class,
            GridUUID.class,
            TransmitterType.class,
        };
    }

    @Override
    public void dynamicEncode(Object[] args, ByteBuf buffer) {
        GridTracking.write((GridUUID<?>)args[0], buffer);
        GridTracking.write((GridUUID<?>)args[1], buffer);
        buffer.writeInt(Mechano.REGISTRATE.getTransmitterRegistry().getId((TransmitterType)args[2]));
    }

    @Override
    public @Nullable Object[] dynamicDecode(ByteBuf buffer) {
        return new Object[] {
            GridTracking.read(buffer), 
            GridTracking.read(buffer),
            Mechano.REGISTRATE.getTransmitterRegistry().byId(buffer.readInt())
        };
    }

    @Override
    public GridAction executeAsServer(int attempt, ServerGrid grid, Object... args) {
        throw new UnsupportedOperationException("link syncing isn't executable on the server");
    }

    @Override
    public GridAction executeAsClient(int attempt, ClientGrid grid, Object... args) {
        GridUUID<?> startID = (GridUUID<?>) args[0];
        GridUUID<?> endID = (GridUUID<?>) args[1];
        AncillaryNode<?> startNode = (AncillaryNode<?>) GridTracking.getComponent(grid, startID);
        AncillaryNode<?> endNode = (AncillaryNode<?>) GridTracking.getComponent(grid, endID);
        GridAction acquireResult = GridAction.ofNullcheck(startNode, endNode);
        if(acquireResult.getActionType().indicatesFailure()) return acquireResult;
        TransmitterType trns = (TransmitterType) args[2];
        AncillaryPair newLink = trns == null 
            ? new AncillaryPair(startID, startNode, endID, endNode) 
            : new ComponentLink<>(trns, startID, startNode, endID, endNode);
        if(newLink instanceof ComponentLink<?> cl)
            cl.saturateCatenary();
        return grid.lookup().add(grid, newLink);
    }
}
