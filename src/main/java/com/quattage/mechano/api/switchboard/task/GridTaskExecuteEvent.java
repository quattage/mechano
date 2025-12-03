package com.quattage.mechano.api.switchboard.task;

import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.switchboard.action.GridActionTask;
import com.quattage.mechano.api.switchboard.action.GridActionType;
import com.quattage.mechano.api.switchboard.action.GridActions;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * This event is fired just before a {@link GridActionTask grid action task} is executed
 * on both sides. This event is cancellable. 
 */
public abstract class GridTaskExecuteEvent<T extends Grid> extends Event implements ICancellableEvent {

    private final Grid grid;
    private final GridActions action;

    public GridTaskExecuteEvent(Grid grid, GridActions action) {
        this.grid = grid;
        this.action = action;
    }

    @SuppressWarnings("unchecked") protected T getGrid() { return (T)grid; }
    protected GridActions getAction() { return action; }
    protected GridActionType getActionType() { return action.getActionType(); }

    @Override
    public void setCanceled(boolean canceled) {
        ICancellableEvent.super.setCanceled(canceled);
    }

    /**
     * Fired every time a {@link GridActionTask grid action task} is executed 
     * on the client. This event is called before the task has 
     * started executing, and can be used to modify or cancel the task.
     */
    public static class Client extends GridTaskExecuteEvent<ClientGrid> {
        public Client(ClientGrid grid, GridActions action) {
            super(grid, action);
        }
    }
    /**
     * Fired every time a {@link GridActionTask grid action task} is executed 
     * on the server. This event is called before the action has 
     * started executing, and can be used to modify or cancel the task.
     */
    public static class Server extends GridTaskExecuteEvent<ServerGrid> {
        public Server(ServerGrid grid, GridActions action) {
            super(grid, action);
        }
    }
}
