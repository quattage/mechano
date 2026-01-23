package com.quattage.mechano.api.switchboard.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.GridComponentTracker;
import com.quattage.mechano.api.grid.component.ComponentUUID;
import com.quattage.mechano.api.grid.component.GridConstruct.GridReferent;
import com.quattage.mechano.api.switchboard.action.ActionTask;
import com.quattage.mechano.api.switchboard.action.GridAction;

import io.netty.buffer.ByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * A task that wraps another.
 * This task is used specifically for invoking the immediate
 * execution of some other task on the opposite side of its 
 * initial call.
 */
public class RequestActionTask implements ActionTask {

    @Override
    public @Nullable Class<?>[] getArgumentTemplate() {
        return new Class<?>[] {
            GridAction.class,
            List.class,
            List.class
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public void dynamicEncode(Object[] args, ByteBuf buffer) {
        GridAction action = (GridAction)args[0];
        buffer.writeInt(action.ordinal());
        List<ComponentUUID<?>> senders = (List<ComponentUUID<?>>)args[1];
        buffer.writeInt(senders.size());
        for(ComponentUUID<?> addr : senders) GridComponentTracker.write(addr, buffer);
        Object[] taskArgs = ((List<Object>)args[2]).toArray();
        action.getTask().dynamicEncode(taskArgs, buffer);
    }

    @Override
    public @Nullable Object[] dynamicDecode(ByteBuf buffer) {
        GridAction action = GridAction.values()[buffer.readInt()];
        int sendersLength = buffer.readInt();
        List<ComponentUUID<?>> senders = new ArrayList<>(sendersLength);
        for(int x = 0; x < sendersLength; x++)
            senders.add(GridComponentTracker.read(buffer));
        List<Object> actionArgs = Arrays.asList(action.getTask().dynamicDecode(buffer));
        Object[] output = new Object[] { action, senders, actionArgs };     
        return output;
    }

    @Override
    @SuppressWarnings("unchecked")
    public GridAction executeAsServer(int attempt, ServerGrid grid, Object... args) {
        GridAction action = (GridAction)args[0];
        Set<ServerPlayer> trackers = GridComponentTracker.collectPlayersTracking((ServerLevel)grid.getWorld(), (List<GridReferent<?>>)args[1]);
        if(trackers.isEmpty()) {
            // immediately warn and fail (even if it isn't necessary) since this edge case could cause issues later
            grid.warn("Skipped sending wrapped request for " + action + " - The supplied collection of senders couldn't be re-addressed.");
            return GridAction.RESPONSE_FAIL_CANCELLED;
        }
        Object[] taskArgs = ((List<Object>)args[2]).toArray();
        ActionTask task = action.getTask();
        GridAction result = task.executeAsServer(grid, taskArgs).broadcast(grid, trackers, taskArgs);
        if(GridAction.VERBOSE_LOGS) grid.debug("Handled " + action + "(REQUEST) in " + grid.getDimensionName() + ":\n\n**Arguments: \n" + task.collectArgsAsString(taskArgs) + "\n\n** Result: \n(" + result.asResource() + ")");
        return result;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings("unchecked")
    public GridAction executeAsClient(int attempt, ClientGrid grid, Object... args) {
        GridAction action = (GridAction)args[0];
        Object[] taskArgs = ((List<Object>)args[2]).toArray();
        ActionTask task = action.getTask();
        GridAction result = task.executeAsClient(grid, taskArgs).broadcast(grid, taskArgs);
        if(GridAction.VERBOSE_LOGS) grid.debug("Handled " + action + "(REQUEST) in " + grid.getDimensionName() + ":\n\n**Arguments: \n" + task.collectArgsAsString(taskArgs) + "\n\n** Result: \n(" + result.asResource() + ")");
        return result;
    }
}
