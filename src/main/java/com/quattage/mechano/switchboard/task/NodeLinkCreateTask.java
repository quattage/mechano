
package com.quattage.mechano.switchboard.task;

import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.transmitter.SpoolItem;
import com.quattage.mechano.api.transmitter.TransmitterType;
import com.quattage.mechano.grid.ClientGrid;
import com.quattage.mechano.grid.GridTracking;
import com.quattage.mechano.grid.ServerGrid;
import com.quattage.mechano.grid.topology.AncillaryNode;
import com.quattage.mechano.grid.topology.AncillaryPair;
import com.quattage.mechano.grid.topology.ComponentLink;
import com.quattage.mechano.grid.topology.core.GridUUID;
import com.quattage.mechano.grid.topology.core.MutableComponentReference;
import com.quattage.mechano.switchboard.RemovalLedger;
import com.quattage.mechano.switchboard.action.ActionTask;
import com.quattage.mechano.switchboard.action.GridAction;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class NodeLinkCreateTask implements ActionTask {

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
        GridTracking.write((GridUUID<?>)args[0], buffer);
        GridTracking.write((GridUUID<?>)args[1], buffer);
        if(args[2] == null)
            buffer.writeInt(-1);
        else buffer.writeInt(Mechano.REGISTRATE.getTransmitterRegistry().getId((TransmitterType)args[2]));
        UUID uuid = (UUID)args[3];
        if(uuid != null) {
            buffer.writeBoolean(true);
            buffer.writeLong(uuid.getMostSignificantBits());
            buffer.writeLong(uuid.getLeastSignificantBits());
        } else buffer.writeBoolean(false);
        if(args[4] == null) buffer.writeInt(-1);
        else buffer.writeInt(((GridAction)args[4]).ordinal());
    }

    @Override
    public Object[] dynamicDecode(ByteBuf buffer) {
        GridUUID<?> start = GridTracking.read(buffer);
        GridUUID<?> end = GridTracking.read(buffer);
        int trnsid = buffer.readInt();
        return new Object[] {
            start, end,
            trnsid < 0 ? null : Mechano.REGISTRATE.getTransmitterRegistry().byId(trnsid),
            buffer.readBoolean() ? new UUID(buffer.readLong(), buffer.readLong()) : null,
            GridAction.ofOrdinal(buffer.readInt())
        };
    }

    @Override
    public Object[] validateArguments(boolean allowNulls, @Nullable Object... args) {
        // the response argument is allowed to be inferred with a default value here
        if(args.length == 4) {
            Object[] copy = new Object[args.length + 1];
            System.arraycopy(args, 0, copy, 0, args.length);
            copy[args.length] = GridAction.RESPONSE_SUCCESS;
            return ActionTask.super.validateArguments(true, copy);
        }
        return ActionTask.super.validateArguments(true, args);
    }

    @Override
    public GridAction executeAsServer(int attempt, ServerGrid grid, Object... args) {
        GridUUID<?> startID = (GridUUID<?>)args[0];
        GridUUID<?> endID = (GridUUID<?>)args[1];
        AncillaryNode<?> startNode = (AncillaryNode<?>) GridTracking.getComponent(grid, startID);
        AncillaryNode<?> endNode = (AncillaryNode<?>) GridTracking.getComponent(grid, endID);
        GridAction acquireResult = GridAction.ofNullcheck(startNode, endNode);
        if(acquireResult.getActionType().indicatesFailure()) return acquireResult;
        TransmitterType trns = (TransmitterType) args[2];
        AncillaryPair newLink = trns == null 
            ? new AncillaryPair(startID, startNode, endID, endNode) 
            : new ComponentLink<>(trns, startID, startNode, endID, endNode);
        Entity caller = args[3] == null ? null : ((ServerLevel)grid.getWorld()).getEntity((UUID)args[3]);
        args[4] = grid.addLink(newLink, caller);
        Set<ServerPlayer> trackers = GridTracking.collectPlayersTracking((ServerLevel) grid.getWorld(), startID, endID);
        if(caller instanceof ServerPlayer sp) trackers.add(sp);
        // GridAction.TASK_LINK_CREATE.broadcastBelligerent(grid, trackers, args);  
        return (GridAction)args[4];
    }

    @Override
    public GridAction executeDeferred(ServerGrid grid, RemovalLedger outdated, MutableComponentReference reference, @Nullable Entity caller) {
        return grid.createLinkDeferred(reference.asAncillaryPair(), caller);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
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
        args[4] = grid.addLink(newLink, self());
        UUID uuid = (UUID)args[3];
        LocalPlayer lp = self();
        if(uuid != null) {
            if(lp.getUUID().equals(uuid) && ((GridAction)args[4]).getActionType().isConsumed())
                SpoolItem.wipeData(lp, true);
        }
        return grid.addLink(newLink, lp);
    }

    
}