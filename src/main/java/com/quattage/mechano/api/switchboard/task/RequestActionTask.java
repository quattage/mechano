package com.quattage.mechano.api.switchboard.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.api.switchboard.action.GridActionTask;
import com.quattage.mechano.foundation.tracking.GridUUID;
import com.quattage.mechano.foundation.tracking.UUIDSourceDiscriminator;

import io.netty.buffer.ByteBuf;

/**
 * A task that wraps another used for invoking the immediate
 * execution of a task on the opposite side of its 
 */
public class RequestActionTask implements GridActionTask {

    @Override
    public @Nullable Class<?>[] getArgumentTemplate() {
        return new Class<?>[] {
            GridAction.class,
            List.class,
            List.class
        };
    }

    @Override
    public void encode(Object[] args, ByteBuf buffer) {
        GridAction action = (GridAction)args[0];
        buffer.writeInt(action.ordinal());
        List<GridUUID> senders = (List<GridUUID>)args[1];
        buffer.writeInt(senders.size());
        for(GridUUID addr : senders)
            UUIDSourceDiscriminator.write(addr, buffer);
        List<Object> actionArgs = (List<Object>)args[2];
        action.getTask().encode(actionArgs.toArray(), buffer);
    }

    @Override
    public @Nullable Object[] decode(ByteBuf buffer) {
        int index = buffer.readInt();
        int sendersLength = buffer.readInt();
        List<GridUUID> senders = new ArrayList<>(sendersLength);
        for(int x = 0; x < sendersLength; x++)
            senders.add(UUIDSourceDiscriminator.read(buffer));
        GridAction action = GridAction.values()[index];
        List<Object> actionArgs = Arrays.asList(action.getTask().decode(buffer));
        Object[] output = new Object[] { action, senders, actionArgs };
        return output;
    }

    @Override
    public GridAction executeAsServer(int attempt, ServerGrid grid, Object... args) {
        logExecution(grid, args);
        return GridAction.RESPONSE_SUCCESS;
    }

    @Override
    public GridAction executeAsClient(int attempt, ClientGrid grid, Object... args) {
        logExecution(grid, args);
        return GridAction.RESPONSE_SUCCESS;
    }
    
}
