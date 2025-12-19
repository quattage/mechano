package com.quattage.mechano.api.grid.solver;

import java.util.Locale;

import com.google.common.base.Stopwatch;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.topology.Circuit;

import net.minecraft.util.StringRepresentable;

/**
 * A NodalSolver is an object that can be used to solve the Grid API's 
 * voltage smatrix using a discrete implementation.
 */
public interface NodalSolver {
    
    double EPSILON = 0.001d;
    int STEP_LIMIT = 255;
    Stopwatch profiler = Stopwatch.createUnstarted();

    /**
     * Runs this solver until completion, either
     * when the iteration limit exceeds <code>STEP_LIMIT</code>
     * or until the residual has been reduced to a value below
     * <code>EPSILON</code>
     * @param snapshot
     * @return <code>FINISHED_SOLVED_EARLY</code> or <code>FINISH_SOLVED_LATE</code> if convergence was reached, see {@link ConvergenceStatus}
     */
    default ConvergenceStatus solve(ServerGrid grid) {
        grid.setStatus(ConvergenceStatus.UNFINISHED_COMPUTING);
        ConvergenceStatus status = null;
        if(NodalSolver.profiler != null) {
            NodalSolver.profiler.start();
            status = run(grid);
            NodalSolver.profiler.reset();
        } else {
            status = run(grid);
            if(!status.indicatesSuccess())
                Mechano.LOGGER.warn("The active NodalSolver couldn't converge in " + NodalSolver.STEP_LIMIT + " iterations");
        }
        grid.setStatus(status);
        return status;
    }

    /**
     * Runs this solver until completeion, either
     * when the iteration limit exceeds <code>STEP_LIMIT</code>
     * or until the error has been reduced to a value below
     * <code>EPSILON</code>
     * @param snapshot
     * @return <code>SOLVED</code> if convergence was reached
     */
    ConvergenceStatus run(ServerGrid grid);
    void apply(Circuit circuit);

    /**
     * Used when a world is unloaded to ensure that the footprint of this solver is minimized.
     */
    void reset();

    public enum ConvergenceStatus implements StringRepresentable {
        FINISHED_SOLVED_EARLY(true),
        FINISHED_SOLVED_LATE(true),
        FINISHED_LIMIT_REACHED(false),
        UNFINISHED_PROBLEMATIC_DATA(false),
        UNFINISHED_COMPUTING(false),
        UNFINISHED_GENERIC_ERROR(false),
        UNFINISHED_UNPOPULATED(false);
        private final boolean success;
        ConvergenceStatus(boolean success) { this.success = success; }
        public boolean indicatesSuccess() { return success; }
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
        @Override public String toString() { return getSerializedName(); }
    }
}
