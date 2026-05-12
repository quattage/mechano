package com.quattage.mechano.grid.solver;

import java.util.Locale;

public enum ConvergenceStatus {

    FINISHED_SOLVED(true),
    FINISHED_SOLVED_LATE(true),
    FINISHED_LIMIT_REACHED(true),
    ABORTED_PROBLEMATIC_DATA(false),
    ABORTED_GENERIC_ERROR(false),
    SOLVING(false),
    STAMPING(false),
    PREPASSING(false),
    CHANGES_QUEUED(false),
    IDLE(false),
    UNLOADED(false),
    DISPOSED(false);

    private final boolean success;

    ConvergenceStatus(boolean success) { this.success = success; }

    public boolean indicatesSuccess() { 
        return success; 
    }

    @Override 
    public String toString() { 
        return name().toLowerCase(Locale.ROOT);
    }
}
