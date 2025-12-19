package com.quattage.mechano.api.switchboard.task;

import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.grid.topology.vertex.AncillaryNode;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.api.transmitter.TransmitterType;
import com.quattage.mechano.foundation.tracking.GridUUID;

import net.minecraft.client.player.LocalPlayer;

public class JointUnlinkTask extends JointLinkTask {

    @Override
    protected void clientSelfHandle(LocalPlayer player, ClientGrid grid, GridAction serverResult) {
        // special behaviours for the client caller are not implemented for manual unlinking yet
    }

    @Override
    protected GridAction unsidedHandle(Grid grid, GridUUID startID, AncillaryNode startNode, GridUUID endID, AncillaryNode endNode, TransmitterType<?> trns) {
        GridAction runResult = grid.removeLink(startID, endID);
        GridAction runResultInverted = grid.removeLink(endID, startID);
        if(runResult.getActionType().indicatesFailure() || runResultInverted.getActionType().indicatesFailure()) {
            // always consume the failure case should one exist
            if(!runResult.getActionType().indicatesFailure())
                runResult = runResultInverted;
        }
        return runResult;
    }
}
