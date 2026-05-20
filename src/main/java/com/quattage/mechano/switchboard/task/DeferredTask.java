package com.quattage.mechano.switchboard.task;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.grid.ServerGrid;
import com.quattage.mechano.grid.topology.core.MutableComponentReference;
import com.quattage.mechano.switchboard.RemovalLedger;
import com.quattage.mechano.switchboard.action.GridAction;

import net.minecraft.world.entity.Entity;

public class DeferredTask implements Comparable<DeferredTask> {

    private final GridAction action;
    private final MutableComponentReference reference;
    private final @Nullable Entity caller;
    private final int taskIndex;

    public DeferredTask(GridAction task, MutableComponentReference reference, @Nullable Entity caller, int index) {
        Objects.requireNonNull(task);
        if(index < 0) throw new IndexOutOfBoundsException("Can't instantiate a TaskWrapper with a negative index! (got " + index + ")");
        this.action = task;
        this.reference = reference;
        this.caller = caller;
        this.taskIndex = index;
    }

    public GridAction run(ServerGrid grid, RemovalLedger outdated) {
        if(GridAction.VERBOSE_LOGS) grid.debug("Initiating wrapped task (" + this.action.getTask().getClass().getSimpleName() + ")");
        GridAction output = this.action.getTask().executeDeferred(grid, outdated, reference, caller);
        return output == null ? GridAction.NONE : output;
    }

    public boolean creates() {
        return action == GridAction.TASK_LINK_CREATE;
    }

    private int getPriority() {
        return action.ordinal();
    }

    @Override
    public int compareTo(DeferredTask that) {
        int priorityCompare = Integer.compare(this.getPriority(), that.getPriority());
        if(priorityCompare != 0) return priorityCompare;
        return Integer.compare(this.taskIndex, that.taskIndex);
    }

    @Override
    public String toString() {
        return action.getTask().getClass().getSimpleName() + " (#" + taskIndex + ")";
    }
}
