
package com.quattage.mechano.api.switchboard.task;

import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.Grid;
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
            TransmitterType.class
        };
    }

    @Override
    public void dynamicEncode(Object[] args, ByteBuf buffer) {
        UUIDSourceDiscriminator.write((GridUUID)args[0], buffer);
        UUIDSourceDiscriminator.write((GridUUID)args[1], buffer);
        Registry<TransmitterType<?>> registry = (Registry<TransmitterType<?>>)BuiltInRegistries.REGISTRY.getOrThrow(MechanoRegistrate.TRANSMITTER_KEY);
        buffer.writeInt(registry.getId((TransmitterType<?>)args[2]));
        return;
    }

    @Override
    public Object[] dynamicDecode(ByteBuf buffer) {
        return new Object[] {
            UUIDSourceDiscriminator.read(buffer),
            UUIDSourceDiscriminator.read(buffer),
            BuiltInRegistries.REGISTRY.getOrThrow(MechanoRegistrate.TRANSMITTER_KEY).byId(buffer.readInt())
        };
    }

    @Override
    public GridAction executeAsServer(int attempt, ServerGrid grid, Object... args) {
        return run(grid, args);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public GridAction executeAsClient(int attempt, ClientGrid grid, Object... args) {
        return run(grid, args);
    }

    private GridAction run(Grid grid, Object... args) {
        TransmitterType<?> trns = (TransmitterType<?>)args[2];
        GridUUID startID = (GridUUID)args[0];
        GridUUID endID = (GridUUID)args[1];
        AncillaryNode startNode = grid.findComponent(startID, AncillaryNode.class);
        AncillaryNode endNode = grid.findComponent(endID, AncillaryNode.class);
        
        ComponentLink<?> linkA = new ComponentLink<>(trns)
            .assignStart(startID, startNode)
            .assignEnd(endID, endNode)
            .checkValidity();
        ComponentLink<?> linkB = 
            linkA.flippedCopy()
            .checkValidity();

        grid.warn(trns + ", " + startID + " -> " + endID);

        return GridAction.RESPONSE_SUCCESS;
    }
}
