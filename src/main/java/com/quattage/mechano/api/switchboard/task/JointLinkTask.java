
package com.quattage.mechano.api.switchboard.task;

import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.topology.ComponentLink;
import com.quattage.mechano.api.grid.topology.vertex.AncillaryNode;
import com.quattage.mechano.api.switchboard.action.ActionTask;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.api.transmitter.SpoolItem;
import com.quattage.mechano.api.transmitter.TransmitterType;
import com.quattage.mechano.foundation.tracking.GridIdentifiable;
import com.quattage.mechano.foundation.tracking.GridUUID;
import com.quattage.mechano.foundation.tracking.UUIDSourceType;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class JointLinkTask implements ActionTask {

    @Override
    public Class<?>[] getArgumentTemplate() {
        return new Class<?>[] {
            GridUUID.class,
            GridUUID.class,
            TransmitterType.class,
            UUID.class,
            GridAction.class,
        };
    }

    @Override
    public void dynamicEncode(Object[] args, ByteBuf buffer) {
        UUIDSourceType.write((GridUUID)args[0], buffer);
        UUIDSourceType.write((GridUUID)args[1], buffer);
        buffer.writeInt(Mechano.REGISTRATE.getTransmitterRegistry().getId((TransmitterType)args[2]));
        UUID uuid = (UUID)args[3];
        if(uuid != null) {
            buffer.writeBoolean(true);
            buffer.writeLong(uuid.getMostSignificantBits());
            buffer.writeLong(uuid.getLeastSignificantBits());
        } else buffer.writeBoolean(false);
        buffer.writeInt(((GridAction)args[4]).ordinal());
        return;
    }

    @Override
    public Object[] dynamicDecode(ByteBuf buffer) {
        return new Object[] {
            UUIDSourceType.read(buffer),
            UUIDSourceType.read(buffer),
            Mechano.REGISTRATE.getTransmitterRegistry().byId(buffer.readInt()),
            buffer.readBoolean() ? new UUID(buffer.readLong(), buffer.readLong()) : null,
            GridAction.values()[buffer.readInt()]
        };
    }

    @Override
    public Object[] validateArguments(@Nullable Object... args) {
        // the response argument is allowed to be inferred with a default value here
        if(args.length == 4) {
            Object[] argsModified = new Object[args.length + 1];
            System.arraycopy(args, 0, argsModified, 0, args.length);
            argsModified[args.length] = GridAction.RESPONSE_SUCCESS;
            return ActionTask.super.validateArguments(argsModified);
        }
        return ActionTask.super.validateArguments(args);
    }


    @Override
    public GridAction executeAsServer(int attempt, ServerGrid grid, Object... args) {
        GridUUID startID = (GridUUID)args[0];
        GridUUID endID = (GridUUID)args[1];
        AncillaryNode startNode = grid.findComponent(startID, AncillaryNode.class);
        AncillaryNode endNode = grid.findComponent(endID, AncillaryNode.class);
        GridAction prematureCancel = GridAction.dualExist(startNode, endNode);
        if(prematureCancel.getActionType().indicatesFailure()) return prematureCancel;
        args[4] = this.unsidedHandle(grid, startID, startNode, endID, endNode, (TransmitterType)args[2]);
        Set<ServerPlayer> trackers = GridIdentifiable.collectTrackers((ServerLevel)grid.getWorld(), startID, endID);
        if(args[3] != null) {
            Entity caller = ((ServerLevel)grid.getWorld()).getEntity((UUID)args[3]);
            if(caller instanceof ServerPlayer sp) trackers.add(sp);
        }
        GridAction.TASK_LINK_JOINTS.broadcastBelligerent(grid, trackers, args);
        return (GridAction)args[4];
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public GridAction executeAsClient(int attempt, ClientGrid grid, Object... args) {
        GridUUID startID = (GridUUID)args[0];
        GridUUID endID = (GridUUID)args[1];
        AncillaryNode startNode = grid.findComponent(startID, AncillaryNode.class);
        AncillaryNode endNode = grid.findComponent(endID, AncillaryNode.class);
        GridAction prematureCancel = GridAction.dualExist(startNode, endNode);
        if(prematureCancel.getActionType().indicatesFailure()) return prematureCancel;
        args[4] = this.unsidedHandle(grid, startID, startNode, endID, endNode, (TransmitterType)args[2]);
        UUID uuid = (UUID)args[3];
        if(uuid != null) {
            GridAction serverResult = (GridAction)args[4];
            LocalPlayer lp = self();
            if(lp.getUUID().equals(uuid))
                clientSelfHandle(lp, grid, serverResult);
        }
        return GridAction.RESPONSE_SUCCESS;
    }

    /**
     * The actual implementation goes here and is identical between client and server. Most of 
     * the stuff further up in this class is for managing the task's serialization to a packet.
     */
    protected GridAction unsidedHandle(Grid grid, GridUUID startID, AncillaryNode startNode, GridUUID endID, AncillaryNode endNode, TransmitterType trns) {
        return grid.addLink(new ComponentLink<>(trns, startID, startNode, endID, endNode));
    }
    
    /**
     * Unique logic executed only by the client that requested the execution of this task
     * (assuming one exists - the sender's UUID is allowed to be null, so this method is
     * never called if this task wasn't requested by a player)
     */
    @OnlyIn(Dist.CLIENT)
    protected void clientSelfHandle(LocalPlayer player, ClientGrid grid, GridAction serverResult) {
        if(serverResult.getActionType().isConsumed()) 
            SpoolItem.wipeData(player, true);
    }
}
