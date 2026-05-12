package com.quattage.mechano.switchboard.task;

import java.util.Objects;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.switchboard.RemovalLedger;
import com.quattage.mechano.switchboard.action.GridAction;

public class TaskWrapper implements Comparable<TaskWrapper> {

    private final GridAction action;
    private final Object[] args;
    private final int index;

    public TaskWrapper(GridAction task, Object[] args) {
        this(task, -1, args);
    }

    public TaskWrapper(GridAction task, int index, Object[] args) {
        Objects.requireNonNull(task);
        this.action = task;
        this.index = index;
        if(args == null) args = new Object[0];
        this.args = args;
    }

    public GridAction run(ServerGrid grid, RemovalLedger removals) {
        if(GridAction.VERBOSE_LOGS) grid.debug("Initiating wrapped task (" + this.action.getTask().getClass().getSimpleName() + ")");
        GridAction output = this.action.getTask().executeTopological(grid, removals, args);
        return output == null ? GridAction.NONE : output;
    }

    public boolean creates() {
        return action == GridAction.TASK_LINK_CREATE;
    }

    private int getPriority() {
        return action.ordinal();
    }

    @Override
    public int compareTo(TaskWrapper that) {
        int priorityCompare = Integer.compare(this.getPriority(), that.getPriority());
        if(priorityCompare != 0) return priorityCompare;
        return Integer.compare(this.index, that.index);
    }

    @Override
    public String toString() {
        return action.getTask().getClass().getSimpleName() + " (index " + index + "), " + action.getTask().collectArgsAsString(args);
    }
}
