
package com.quattage.mechano.api.switchboard.task;

import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.topology.ComponentLink;
import com.quattage.mechano.api.grid.topology.vertex.AncillaryNode;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.api.switchboard.action.GridActionTask;
import com.quattage.mechano.api.transmitter.SpoolItem;
import com.quattage.mechano.api.transmitter.TransmitterType;
import com.quattage.mechano.foundation.MechanoRegistrate;
import com.quattage.mechano.foundation.tracking.GridIdentifiable;
import com.quattage.mechano.foundation.tracking.GridUUID;
import com.quattage.mechano.foundation.tracking.UUIDSourceType;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class LinkJointsTask implements GridActionTask {

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
        Registry<TransmitterType<?>> registry = (Registry<TransmitterType<?>>)BuiltInRegistries.REGISTRY.getOrThrow(MechanoRegistrate.TRANSMITTER_KEY);
        buffer.writeInt(registry.getId((TransmitterType<?>)args[2]));
        buffer.writeLong(((UUID)args[3]).getMostSignificantBits());
        buffer.writeLong(((UUID)args[3]).getLeastSignificantBits());
        buffer.writeInt(((GridAction)args[4]).ordinal());
        return;
    }

    @Override
    public Object[] dynamicDecode(ByteBuf buffer) {
        return new Object[] {
            UUIDSourceType.read(buffer),
            UUIDSourceType.read(buffer),
            BuiltInRegistries.REGISTRY.getOrThrow(MechanoRegistrate.TRANSMITTER_KEY).byId(buffer.readInt()),
            new UUID(buffer.readLong(), buffer.readLong()),
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
            return GridActionTask.super.validateArguments(argsModified);
        }
        return GridActionTask.super.validateArguments(args);
    }

    @Override
    public GridAction executeAsServer(int attempt, ServerGrid grid, Object... args) {
        GridUUID startID = (GridUUID)args[0];
        GridUUID endID = (GridUUID)args[1];
        args[4] = this.link(grid, startID, endID, (TransmitterType<?>)args[2]);
        Set<ServerPlayer> trackers = GridIdentifiable.collectTrackers((ServerLevel)grid.getWorld(), startID, endID);
        Entity caller = ((ServerLevel)grid.getWorld()).getEntity((UUID)args[3]);
        if(caller instanceof ServerPlayer sp) trackers.add(sp);
        GridAction.TASK_LINK_JOINTS.broadcastBelligerent(grid, trackers, args);
        return (GridAction)args[4];
    }

    

    @Override
    @OnlyIn(Dist.CLIENT)
    public GridAction executeAsClient(int attempt, ClientGrid grid, Object... args) {
        GridAction serverResult = (GridAction)args[4];
        LocalPlayer lp = self();
        if(lp.getUUID().equals(args[3]))
            handleAsSelf(lp, grid, serverResult);
        return GridAction.RESPONSE_SUCCESS;
    }

    private void handleAsSelf(LocalPlayer player, ClientGrid grid, GridAction serverResult) {
        if(serverResult.getActionType().isConsumed()) 
            SpoolItem.wipeData(player, true);
    }

    private GridAction link(Grid grid, GridUUID startID, GridUUID endID, TransmitterType<?> trns) {
        AncillaryNode startNode = grid.findComponent(startID, AncillaryNode.class);
        AncillaryNode endNode = grid.findComponent(endID, AncillaryNode.class);
        ComponentLink<?> linkA = new ComponentLink<>(trns, startID, startNode, endID, endNode).checkValidity();
        ComponentLink<?> linkB = linkA.flippedCopy().checkValidity();
        GridAction runResult = grid.addLink(linkA);
        GridAction runResultInverted = grid.addLink(linkB);
        if(runResult.getActionType().indicatesFailure() || runResultInverted.getActionType().indicatesFailure()) {
            grid.removeLink(linkA);
            grid.removeLink(linkB);
            // always consume the failure case should one exist
            if(!runResult.getActionType().indicatesFailure())
                runResult = runResultInverted;
        }
        return runResult;
    }
}
