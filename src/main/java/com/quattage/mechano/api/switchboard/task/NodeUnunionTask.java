package com.quattage.mechano.api.switchboard.task;

import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.grid.component.ComponentUUID;
import com.quattage.mechano.api.grid.topology.vertex.AncillaryNode;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.api.transmitter.TransmitterType;

import net.minecraft.client.player.LocalPlayer;

public class NodeUnunionTask extends NodeUnionTask {

    @Override
    protected void clientSelfHandle(LocalPlayer player, ClientGrid grid, GridAction serverResult) {
        // special behaviours for the client caller are not implemented for manual unlinking yet
    }

    @Override
    protected GridAction unsidedHandle(Grid grid, ComponentUUID startID, AncillaryNode startNode, ComponentUUID endID, AncillaryNode endNode, TransmitterType trns) {
        return grid.removeLink(startID, endID);
    }
}
