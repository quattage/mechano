package com.quattage.mechano.api.switchboard.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.api.switchboard.action.GridAction.ActionRunner;
import com.quattage.mechano.api.switchboard.action.GridActionTask;
import com.quattage.mechano.foundation.tracking.GridUUID;
import com.quattage.mechano.foundation.tracking.UUIDSourceDiscriminator;

import io.netty.buffer.ByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * A task that wraps another.
 * This task is used specifically for invoking the immediate
 * execution of some other task on the opposite side of its 
 * initial call.
 */
public class RequestActionTask implements GridActionTask {

    private @Nullable ActionRunner runner;

    @Override
    public @Nullable Class<?>[] getArgumentTemplate() {
        return new Class<?>[] {
            GridAction.class,
            List.class,
            List.class
        };
    }

    @Override
    public void dynamicEncode(Object[] args, ByteBuf buffer) {
        GridAction action = (GridAction)args[0];
        buffer.writeInt(action.ordinal());
        List<GridUUID> senders = (List<GridUUID>)args[1];
        buffer.writeInt(senders.size());
        for(GridUUID addr : senders) UUIDSourceDiscriminator.write(addr, buffer);
        List<Object> actionArgs = (List<Object>)args[2];
        action.getTask().dynamicEncode(actionArgs.toArray(), buffer);
    }

    @Override
    public @Nullable Object[] dynamicDecode(ByteBuf buffer) {
        GridAction action = GridAction.values()[buffer.readInt()];
        int sendersLength = buffer.readInt();
        List<GridUUID> senders = new ArrayList<>(sendersLength);
        for(int x = 0; x < sendersLength; x++)
            senders.add(UUIDSourceDiscriminator.read(buffer));
        List<Object> actionArgs = Arrays.asList(action.getTask().dynamicDecode(buffer));
        Object[] output = new Object[] { action, senders, actionArgs };     
        return output;
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

    // all the side-specific logic is already wrapped by the action being conveyed by this task
    private GridAction run(Grid grid, Object... args) {
        List<GridUUID> senderIDs = (List<GridUUID>)args[1];
        GridAction action = (GridAction)args[0];
        Set<Griddable<?>> senders = new HashSet<>();
        for(GridUUID id : senderIDs) {
            Griddable<?> source = id.getTargetSource(grid);
            if(source == null) continue;
            senders.add(source);
        }
        if(senders.isEmpty()) {
            grid.warn("Skipped sending wrapped request for " + action + " - The supplied collection of senders couldn't be re-addressed.");
            return GridAction.RESPONSE_FAIL_CANCELLED;
        }
        getRunner().in(grid).action(action)
            .from(senders.toArray(new Griddable<?>[senders.size()]))
            .withArguments((List<Object>)args[2])
            .executeImmediately();
        return GridAction.RESPONSE_SUCCESS;
    }

    private ActionRunner getRunner() {
        if(runner == null) runner = new ActionRunner();
        return runner;
    }
    
}
