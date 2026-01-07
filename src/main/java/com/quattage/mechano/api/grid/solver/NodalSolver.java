package com.quattage.mechano.api.grid.solver;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.quattage.mechano.api.ServerGrid;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import net.minecraft.util.StringRepresentable;

/**
 * A NodalSolver is an object that can be used to solve the Grid API's 
 * voltage smatrix using a discrete implementation.
 */
public interface NodalSolver {

    double EPSILON = 0.001d;
    int STEP_LIMIT = 255;

    /** 
     * @return A string describing the method used for this solver
     */
    String describeSelf();

    /**
     * Set up initial values for this solver by allocating matrices and arrays as needed.
     * This method is only called when <code>grid</code> is modified or changes size in any way,
     * but is not called every tick.
     * @param grid
     */
    void initialize(ServerGrid grid);

    /**
     * Runs this solver until completeion, either
     * when the iteration limit exceeds <code>STEP_LIMIT</code>
     * or until the error has been reduced to a value below
     * <code>EPSILON</code>
     * @param snapshot
     * @return <code>SOLVED</code> if convergence was reached
     */
    ConvergenceStatus run(ServerGrid grid);

    /**
     * Used when a world is unloaded to ensure that the footprint of this solver is minimized.
     */
    void reset();

    public enum ConvergenceStatus implements StringRepresentable {
        FINISHED_SOLVED_EARLY(true),
        FINISHED_SOLVED_LATE(true),
        FINISHED_LIMIT_REACHED(false),
        ABORTED_PROBLEMATIC_DATA(false),
        ABORTED_GENERIC_ERROR(false),
        REFRESHING_TOPOLOGY(false),
        COMPUTING(false),
        IDLE(false),
        UNLOADED(false);
        private final boolean success;
        ConvergenceStatus(boolean success) { this.success = success; }
        public boolean indicatesSuccess() { return success; }
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
        @Override public String toString() { return getSerializedName(); }
    }

    public class ConvergenceStatusHolder implements NonNullSupplier<ConvergenceStatus> {
        
        private AtomicReference<ConvergenceStatus> status = new AtomicReference<>(ConvergenceStatus.UNLOADED);
        private AtomicLong lastUpdateTime = new AtomicLong(System.currentTimeMillis());
        
        public ConvergenceStatusHolder set(ConvergenceStatus newStatus) {
            Objects.requireNonNull(status);
            if(status.get() == newStatus) return this;
            this.status.set(newStatus);
            lastUpdateTime.set(System.currentTimeMillis());
            return this;
        }

        @Override
        public ConvergenceStatus get() {
            return status.get();
        }

        @Override
        public String toString() {
            return "'" + status.get() + "' (held for " + DurationFormatUtils.formatDuration(System.currentTimeMillis() - lastUpdateTime.get(), "ss.SSS") + "s)";
        }
    }
}
