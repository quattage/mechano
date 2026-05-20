package com.quattage.mechano.grid.solver;

import com.quattage.mechano.foundation.Disposable;
import com.quattage.mechano.grid.Netlist;

/**
 * A SolverAlgorithm is an object that can be used to solve the Grid API's 
 * voltage smatrix using a discrete implementation.
 */
public interface SolverAlgorithm extends Disposable {

    double DELTA = 0.05d;
    double EPSILON = 0.001d;
    int STEP_LIMIT = 255;
    double G_MIN = 1e-12d;

    /** 
     * @return A string describing the method used for this solver
     */
    String describeSelf();

    /**
     * Set up initial values for this solver by allocating matrices and arrays as needed.
     * This method is only called when <code>grid</code> is modified or changes size in any way,
     * but is not called every tick.
     * @param solver The coordinator to initialize onto
     */
    void initialize(Netlist netlist);

    /**
     * Runs this solver until completeion, either
     * when the iteration limit exceeds <code>STEP_LIMIT</code>
     * or until the error has been reduced to a value below
     * <code>EPSILON</code>
     * @param solver The coordinator to solve
     */
    ConvergenceStatus run(Netlist netlist);
}
