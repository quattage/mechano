
package com.quattage.mechano.api.switchboard.task;

import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.GridTracking;
import com.quattage.mechano.api.grid.GridUUID;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.topology.NodeUnionSet.NodePair;
import com.quattage.mechano.api.grid.topology.landmark.AncillaryNode;
import com.quattage.mechano.api.grid.topology.landmark.AncillaryPair;
import com.quattage.mechano.api.grid.topology.landmark.ComponentLink;
import com.quattage.mechano.api.grid.topology.landmark.Node;
import com.quattage.mechano.api.switchboard.action.ActionTask;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.api.transmitter.SpoolItem;
import com.quattage.mechano.api.transmitter.TransmitterType;
import com.quattage.mechano.api.transmitter.TransmitterType.UnionFactory;

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
        args[4] = grid.addLinkDeferred(newLink, caller);
        Set<ServerPlayer> trackers = GridTracking.collectPlayersTracking((ServerLevel) grid.getWorld(), startID, endID);
        if(caller instanceof ServerPlayer sp) trackers.add(sp);
        // GridAction.TASK_LINK_CREATE.broadcastBelligerent(grid, trackers, args);  
        return (GridAction)args[4];
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
        args[4] = grid.lookup().add(grid, newLink);
        UUID uuid = (UUID)args[3];
        if(uuid != null) {
            LocalPlayer lp = self();
            if(lp.getUUID().equals(uuid))
                clientSelfHandle(lp, grid, (GridAction) args[4]);
        }
        return GridAction.RESPONSE_SUCCESS;
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

    @Override
    public GridAction executeTopological(ServerGrid grid, Set<Node> removedNodes, Set<NodePair> disjoints, Object[] args) {
        AncillaryPair link = (AncillaryPair) args[0];
        Griddable<?> startSource = GridTracking.getSource(link.getStartAncillary());
        Griddable<?> endSource =  GridTracking.getSource(link.getEndAncillary());
        if(link instanceof ComponentLink<?> cl) {
            cl.applyTo(grid);
            // TODO register component with 
            grid.initiateTask(GridAction.TASK_LINK_CREATE)
                .targeting(startSource, endSource)
                .withArguments(link.getStartID(), link.getEndID(), cl.getTransmitter(), args[1] == null ? null : ((Entity)args[1]).getUUID(), null)
                .executeOnClients();
        } else {
            UnionFactory.perfectConductor(grid, link.getStartAncillary(), link.getEndAncillary());
            grid.initiateTask(GridAction.TASK_LINK_CREATE)
                .targeting(startSource, endSource)
                .withArguments(link.getStartID(), link.getEndID(), null, args[1] == null ? null : ((Entity)args[1]).getUUID(), null)
                .executeOnClients();
        }
        grid.lookup().add(grid, link);
        return GridAction.RESPONSE_SUCCESS;
    }
}
