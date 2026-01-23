
package com.quattage.mechano.api.switchboard.task;

import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.GridComponentTracker;
import com.quattage.mechano.api.grid.component.ComponentUUID;
import com.quattage.mechano.api.grid.topology.AncillaryPair;
import com.quattage.mechano.api.grid.topology.ComponentLink;
import com.quattage.mechano.api.grid.topology.netlist.NodeUnionSet.NodePair;
import com.quattage.mechano.api.grid.topology.vertex.AncillaryNode;
import com.quattage.mechano.api.grid.topology.vertex.Node;
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
            ComponentUUID.class,
            ComponentUUID.class,
            TransmitterType.class,
            UUID.class,
            GridAction.class,
        };
    }

    @Override
    public void dynamicEncode(Object[] args, ByteBuf buffer) {
        GridComponentTracker.write((ComponentUUID<?>)args[0], buffer);
        GridComponentTracker.write((ComponentUUID<?>)args[1], buffer);
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
            GridComponentTracker.read(buffer),
            GridComponentTracker.read(buffer),
            Mechano.REGISTRATE.getTransmitterRegistry().byId(buffer.readInt()),
            buffer.readBoolean() ? new UUID(buffer.readLong(), buffer.readLong()) : null,
            GridAction.values()[buffer.readInt()]
        };
    }

    @Override
    public Object[] validateArguments(@Nullable Object... args) {
        // the response argument is allowed to be inferred with a default value here
        if(args.length == 4) {
            Object[] copy = new Object[args.length + 1];
            System.arraycopy(args, 0, copy, 0, args.length);
            copy[args.length] = GridAction.RESPONSE_SUCCESS;
            return ActionTask.super.validateArguments(copy);
        }
        return ActionTask.super.validateArguments(args);
    }


    @Override
    public GridAction executeAsServer(int attempt, ServerGrid grid, Object... args) {
        ComponentUUID<?> startID = (ComponentUUID<?>)args[0];
        ComponentUUID<?> endID = (ComponentUUID<?>)args[1];
        AncillaryNode<?> startNode = null, endNode = null; 
        try { startNode = (AncillaryNode<?>) GridComponentTracker.findOrThrow(grid, startID); } 
        catch (Exception e) { return GridAction.RESPONSE_FAIL_START_MISSING; }
        try { endNode = (AncillaryNode<?>) GridComponentTracker.findOrThrow(grid, endID); } 
        catch (Exception e) { return GridAction.RESPONSE_FAIL_END_MISSING; }
        args[4] = grid.addLinkDeferred(new ComponentLink<>((TransmitterType)args[2], startID, startNode, endID, endNode));
        Set<ServerPlayer> trackers = GridComponentTracker.collectPlayersTracking((ServerLevel)grid.getWorld(), startID, endID);
        if(args[3] != null) {
            Entity caller = ((ServerLevel)grid.getWorld()).getEntity((UUID)args[3]);
            if(caller instanceof ServerPlayer sp) trackers.add(sp);
        }
        GridAction.TASK_LINK_CREATE.broadcastBelligerent(grid, trackers, args);
        return (GridAction)args[4];
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public GridAction executeAsClient(int attempt, ClientGrid grid, Object... args) {
        ComponentUUID<?> startID = (ComponentUUID<?>) args[0];
        ComponentUUID<?> endID = (ComponentUUID<?>) args[1];
        AncillaryNode<?> startNode = null, endNode = null; 
        try { startNode = (AncillaryNode<?>) GridComponentTracker.findOrThrow(grid, startID); } 
        catch (Exception e) { return GridAction.RESPONSE_FAIL_START_MISSING; }
        try { endNode = (AncillaryNode<?>) GridComponentTracker.findOrThrow(grid, endID); } 
        catch (Exception e) { return GridAction.RESPONSE_FAIL_END_MISSING; }
        args[4] = grid.addLink(new ComponentLink<>((TransmitterType)args[2], startID, startNode, endID, endNode));
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
        if(link instanceof ComponentLink<?> cl)
            cl.applyTo(grid);
        else UnionFactory.perfectConductor(grid, link.getStartAncillary(), link.getEndAncillary());
        grid.addLink(link);
        return GridAction.RESPONSE_SUCCESS;
    }
}
