package com.quattage.mechano.switchboard.action;

import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.ServerGrid;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * This event is fired just before a {@link ActionTask grid action task} is executed
 * on both sides. This event is cancellable. 
 */
public abstract class GridTaskExecuteEvent<T extends Grid> extends Event implements ICancellableEvent {

    private final Grid grid;
    private final GridAction action;

    public GridTaskExecuteEvent(Grid grid, GridAction action) {
        this.grid = grid;
        this.action = action;
    }

    @SuppressWarnings("unchecked") protected T getGrid() { return (T)grid; }
    protected GridAction getAction() { return action; }
    protected ActionType getActionType() { return action.getActionType(); }

    @Override
    public void setCanceled(boolean canceled) {
        ICancellableEvent.super.setCanceled(canceled);
    }

    /**
     * Fired every time a {@link ActionTask grid action task} is executed 
     * on the client. This event is called before the task has 
     * started executing, and can be used to modify or cancel the task.
     */
    public static class Client extends GridTaskExecuteEvent<ClientGrid> {
        public Client(ClientGrid grid, GridAction action) {
            super(grid, action);
        }
    }
    /**
     * Fired every time a {@link ActionTask grid action task} is executed 
     * on the server. This event is called before the action has 
     * started executing, and can be used to modify or cancel the task.
     */
    public static class Server extends GridTaskExecuteEvent<ServerGrid> {
        public Server(ServerGrid grid, GridAction action) {
            super(grid, action);
        }
    }
}
