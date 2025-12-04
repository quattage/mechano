
package com.quattage.mechano.api.switchboard.task;

import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.api.switchboard.action.GridActionTask;
import com.quattage.mechano.api.transmitter.TransmitterType;
import com.quattage.mechano.foundation.tracking.GridUUID;
import com.quattage.mechano.foundation.tracking.UUIDSourceDiscriminator;

import io.netty.buffer.ByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class LinkJointsTask implements GridActionTask {

    @Override
    public Class<?>[] getArgumentTemplate() {
        return new Class<?>[] {
            GridUUID.class,
            GridUUID.class,
            ResourceLocation.class
        };
    }

    @Override
    public void encode(Object[] args, ByteBuf buffer) {
        ((GridUUID)args[0]).write(buffer);
        ((GridUUID)args[1]).write(buffer);
        ResourceLocation.STREAM_CODEC.encode(buffer, (ResourceLocation)args[2]);
        return;
    }

    @Override
    public Object[] decode(ByteBuf buffer) {
        return new Object[] {
            UUIDSourceDiscriminator.read(buffer),
            UUIDSourceDiscriminator.read(buffer),
            ResourceLocation.STREAM_CODEC.decode(buffer)
        };
    }

    @Override
    public GridAction executeAsServer(int attempt, ServerGrid grid, Object... args) {
        TransmitterType<?> trns = grid.getTransmitterAt((ResourceLocation)args[2]);
        GridUUID startID = (GridUUID)args[0];
        GridUUID endID = (GridUUID)args[1];
        logExecution(grid, args);
        return GridAction.RESPONSE_SUCCESS;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public GridAction executeAsClient(int attempt, ClientGrid grid, Object... args) {
        TransmitterType<?> trns = grid.getTransmitterAt((ResourceLocation)args[2]);
        GridUUID startID = (GridUUID)args[0];
        GridUUID endID = (GridUUID)args[1];
        logExecution(grid, args);
        return GridAction.RESPONSE_SUCCESS;
    }
}
