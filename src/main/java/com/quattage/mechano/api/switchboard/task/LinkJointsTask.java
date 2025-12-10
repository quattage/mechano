
package com.quattage.mechano.api.switchboard.task;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.topology.ComponentLink;
import com.quattage.mechano.api.grid.topology.ancillary.AncillaryNode;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.api.switchboard.action.GridActionTask;
import com.quattage.mechano.api.transmitter.TransmitterType;
import com.quattage.mechano.foundation.MechanoRegistrate;
import com.quattage.mechano.foundation.tracking.GridUUID;
import com.quattage.mechano.foundation.tracking.UUIDSourceDiscriminator;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class LinkJointsTask implements GridActionTask {

    @Override
    public Class<?>[] getArgumentTemplate() {
        return new Class<?>[] {
            GridUUID.class,
            GridUUID.class,
            TransmitterType.class,
            GridAction.class
        };
    }

    @Override
    public void dynamicEncode(Object[] args, ByteBuf buffer) {
        UUIDSourceDiscriminator.write((GridUUID)args[0], buffer);
        UUIDSourceDiscriminator.write((GridUUID)args[1], buffer);
        Registry<TransmitterType<?>> registry = (Registry<TransmitterType<?>>)BuiltInRegistries.REGISTRY.getOrThrow(MechanoRegistrate.TRANSMITTER_KEY);
        buffer.writeInt(registry.getId((TransmitterType<?>)args[2]));
        buffer.writeInt(((GridAction)args[3]).ordinal());
        return;
    }

    @Override
    public Object[] dynamicDecode(ByteBuf buffer) {
        return new Object[] {
            UUIDSourceDiscriminator.read(buffer),
            UUIDSourceDiscriminator.read(buffer),
            BuiltInRegistries.REGISTRY.getOrThrow(MechanoRegistrate.TRANSMITTER_KEY).byId(buffer.readInt()),
            GridAction.values()[buffer.readInt()]
        };
    }

    @Override
    public Object[] validateArguments(@Nullable Object... args) {
        // the response argument is allowed to be inferred with a default value here
        if(args.length == 3) {
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
        TransmitterType<?> trns = (TransmitterType<?>)args[2];
        AncillaryNode startNode = grid.findComponent(startID, AncillaryNode.class);
        AncillaryNode endNode = grid.findComponent(endID, AncillaryNode.class);
        ComponentLink<?> linkA = new ComponentLink<>(trns, startID, startNode, endID, endNode).checkValidity();
        ComponentLink<?> linkB = linkA.flippedCopy().checkValidity();

        GridAction runResult = grid.addLink(linkA);
        GridAction runResultInverted = grid.addLink(linkB);
        if(runResult.getActionType().indicatesFailure() || runResultInverted.getActionType().indicatesFailure()) {
            grid.removeLink(linkA);
            grid.removeLink(linkB);
        }
        args[3] = runResult;
        GridAction.TASK_LINK_JOINTS.broadcastBelligerent(grid, args);
        return GridAction.RESPONSE_SUCCESS;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public GridAction executeAsClient(int attempt, ClientGrid grid, Object... args) {
        return GridAction.RESPONSE_SUCCESS;
    }
}
