package com.quattage.mechano.api.switchboard.task;

import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.switchboard.action.GridActionTask;
import com.quattage.mechano.api.switchboard.action.GridActions;
import com.quattage.mechano.foundation.tracking.GridUUID;
import com.quattage.mechano.foundation.tracking.UUIDSourceDiscriminator;

import io.netty.buffer.ByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class LinkJointsTask implements GridActionTask {

    @Override
    public Class<?>[] getArgumentTemplate() {
        return new Class<?>[] {
            GridUUID.class,
            GridUUID.class
        };
    }

    @Override
    public void encode(Object[] args, ByteBuf buffer) {
        ((GridUUID)args[0]).write(buffer);
        ((GridUUID)args[1]).write(buffer);
        return;
    }

    @Override
    public Object[] decode(ByteBuf buffer) {
        return new Object[] {
            UUIDSourceDiscriminator.read(buffer),
            UUIDSourceDiscriminator.read(buffer)
        };
    }

    @Override
    public GridActions executeAsServer(int attempt, ServerGrid grid, Object... args) {
        return GridActions.RESPONSE_SUCCESS;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public GridActions executeAsClient(int attempt, ClientGrid grid, Object... args) {
        
        return GridActions.RESPONSE_SUCCESS;
    }
}
