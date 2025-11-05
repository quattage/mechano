package com.quattage.mechano.api.grid.solver;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.grid.topology.Circuit;

import net.minecraft.util.StringRepresentable;

/**
 * NodalSolvers 
 */
public interface NodalSolver {
    
    public static final double EPSILON = 0.001d;
    public static final int STEP_LIMIT = 255;
    public static final Stopwatch profiler = Stopwatch.createUnstarted();

    public default void printProfileResult(NodalSnapshot snapshot) {
        Mechano.LOGGER.info("Profile result for NodalSolver[" + getClass().getSimpleName() 
            + "]\n\tFootprint: ~" + estimateMemoryFootprint(snapshot) + "bytes\n\tBatch area: " + snapshot.matrixArea() 
            + "u\n\tCompute count: " + snapshot.termA().getNumRows() + "iterations\n\tReturned status '" + snapshot.getStatus() + "' in " + profiler.elapsed(TimeUnit.NANOSECONDS) + "ns");
    }

    /**
     * Runs this solver until completion, either
     * when the iteration limit exceeds <code>STEP_LIMIT</code>
     * or until the residual has been reduced to a value below
     * <code>EPSILON</code>
     * @param snapshot
     * @return <code>FINISHED_SOLVED_EARLY</code> or <code>FINISH_SOLVED_LATE</code> if convergence was reached, see {@link ConvergenceStatus}
     */
    public default ConvergenceStatus solve(NodalSnapshot snapshot) {
        snapshot.status.set(ConvergenceStatus.UNFINISHED_COMPUTING);
        ConvergenceStatus status = null;
        if(profiler != null) {
            profiler.start();
            status = run(snapshot);
            printProfileResult(snapshot);
            profiler.reset();
        } else {
            status = run(snapshot);
            if(!status.indicatesSuccess())
                Mechano.LOGGER.warn("The active NodalSolver couldn't converge in " + STEP_LIMIT + " iterations");
        }
        snapshot.status.set(status);
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
    public abstract ConvergenceStatus run(NodalSnapshot snapshot);
    public abstract void apply(Circuit circuit);

    /**
     * Used primarily for testing to force a solver instance to dump all of its presiding data
     */
    public abstract void reset();

    /**
     * Based on how many working variables and matrices this solver needs, this method
     * should return a overestimate of how many bytes are taken up by instance variables
     * plus a reference to the solver itself.
     * @return The approximate amount of bytes taken up by this solver
     */
    public default int estimateMemoryFootprint(NodalSnapshot snapshot) {
        return NodalSolver.estimateMemoryFootprint(0, 0, 0, snapshot.totalSize());
    }

    public static int estimateMemoryFootprint(int scalars, int vectors, int matrices, int size) {
        String bitDescriptor = System.getProperty("os.arch");
        int bits = (bitDescriptor != null && bitDescriptor.contains("64")) ? 64 : 32;
        return (bits + (scalars * 64) + (vectors * size * 64) + (vectors * bits) + (matrices * size * size * 64) + (matrices * bits)) / 8;
    }

    public static enum ConvergenceStatus implements StringRepresentable {
        FINISHED_SOLVED_EARLY(true),
        FINISHED_SOLVED_LATE(true),
        FINISHED_LIMIT_REACHED(false),
        UNFINISHED_PROBLEMATIC_DATA(false),
        UNFINISHED_COMPUTING(false),
        UNFINISHED_GENERIC_ERROR(false),
        UNFINISHED_UNPOPULATED(false);
        private final boolean success;
        private ConvergenceStatus(boolean success) { this.success = success; }
        public boolean indicatesSuccess() { return success; }
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
        @Override public String toString() { return getSerializedName(); }
    }
}
