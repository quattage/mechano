package com.quattage.mechano.grid.solver;

import java.util.Objects;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.Disposable;
import com.quattage.mechano.grid.topology.Node;

public class NodalSolver implements Disposable {
        
    private long lastUpdateTime = -1L;
    private ConvergenceStatus status = ConvergenceStatus.UNLOADED;
    
    private @Nullable Node referenceNode;
    private @Nullable DMatrixSparseCSC matrixA;
    private @Nullable DMatrixRMaj matrixX, matrixB;

    public NodalSolver setStatus(@Nullable ConvergenceStatus newStatus) {
        Objects.requireNonNull(newStatus);
        if(status == newStatus) return this;
        this.status = newStatus;
        this.lastUpdateTime = System.currentTimeMillis();
        return this;
    }

    public ConvergenceStatus getStatus() {
        return status;
    }

    /**
     * Prepares this ConvergenceCoordinator by clearing internal
     * instantiating new matrices set to the provided size.
     * This method call immediately clears this coordinator's 
     * solved matrices.
     * @param size The size to set this ConvergenceCoordinator to.
     * @return This ConvergenceCoordinator, modified as a result of this call
     */
    public NodalSolver prepareWithSize(int size) {
        if(referenceNode != null) size--;  // TODO IS THIS NECESSARY
        setStatus(ConvergenceStatus.PREPASSING);
        matrixA = new DMatrixSparseCSC(size, size);
        matrixX = new DMatrixRMaj(size, 1);
        matrixB = new DMatrixRMaj(size, 1);
        return this;
    }

    /**
     * Solves this ConvergenceCoordinator using the provided solver.
     * This method will assign values to the 
     * @param solver
     * @return
     */
    public NodalSolver run(SolverMethod solver) {
        Objects.requireNonNull(solver);
        return run(solver, Mechano.LOGGER);
    }

    /**
     * Solves this coordinator for voltages at every node and Updates
     * this ConvergenceCoordinators internal matrices and status to
     * reflect this.
     * @param Logger  Optional, defaults to {@link Mechano.LOGGER the mod logger}
     * @param solver {@link SolverMethod solver method} to use
     * @return This ConvergenceCoordinator, modified as a result of this call
     */
    public NodalSolver run(SolverMethod solver, @Nullable Logger logger) {
        assertNotDisposed();
        Objects.requireNonNull(solver);
        setStatus(ConvergenceStatus.SOLVING);
        ConvergenceStatus newStatus = solver.run(this);
        Objects.requireNonNull(newStatus);
        if(!newStatus.indicatesSuccess()) {
            if(logger != null) logger.warn("A solver run returned an invalid status value of " + newStatus 
                + ". This could indicate that the solver has exited in an invlaid state - proceed with caution.");
        }
        setStatus(newStatus);
        return this;
    }

    public @Nullable Node referenceNode() {
        return referenceNode;
    }

    public String matrixAsString() {
        String out = "";
        if(matrixA == null) return "null";
        for(int x = 0; x < matrixA.numRows; x++) {
            for(int y = 0; y < matrixA.numCols; y++) {
                out += "" + matrixA.get(x, y) + " ";
            }
            out += "\n";
        }
        return out.substring(0, out.length() - 1);
    }

    public String termsAsString() {
        String out = "";
        if(matrixB == null) return "null";
        for(int x = 0; x < matrixB.numRows; x++) {
            out += matrixB.get(x, 0) + " ";
        }
        return out.substring(0, out.length() - 1);
    }

    public String solutionAsString() {
        String out = "";
        if(matrixX == null) return "null";
        for(int x = 0; x < matrixX.numRows; x++) {
            out += matrixX.get(x, 0) + " ";
        }
        return out.substring(0, out.length() - 1);
    }

    /**
     * <code>Ax=b</code><p>
     * Calls to this method manipulate matrix <code>A</code> by
     * accumulating <code>value</code> at <code>[row, col]</code>.
     * @param row X axis value
     * @param col Y axis value
     * @param value value to stamp
     */
    public void stampA(int row, int col, double value) {
        if(row < 0 || col < 0) return;
        matrixA.unsafe_set(row, col, matrixA.get(row, col) + value);
    }

    /**
     * <code>Ax=b</code><p>
     * Sets the value at <code>index</code> of the conductance
     * vector <code>b</code> to the provided value.
     * @param index
     * @param value
     */
    public void stampB(int index, double value) {
        if(index < 0) return;
        matrixB.set(index, 0, value);
    }

    /**
     * <code>Ax = b, n = indexer length + netlist length</code><p>
     * @return <code>A (Matrix[n][n])</code>
     */
    public DMatrixSparseCSC matrix() {
        assertNotDisposed();
        return matrixA;
    }

    /**
     * <code>Ax = b, n = indexer length + netlist length</code><p>
     * @return <code>x (Vector[n][1])</code>
     */
    public DMatrixRMaj solution() {
        assertNotDisposed();
        return matrixX;
    }

    /**
     * <code>Ax = b, n = indexer length + netlist length</code><p>
     * @return <code>b (Vector[n][1])</code>
     */
    public DMatrixRMaj terms() {
        assertNotDisposed();
        return matrixB;
    }

    /**
     * Used by {@link SolverMethod solvers} to quickly create 
     * and return a one-dimensional array pre-configured to the correct size.
     * @return A {@link DMAtrixRMaj vector} whose length is the number of rows in the current matrix.
     */
    public DMatrixRMaj createWorkingVector() {
        assertNotDisposed();
        if(matrixA == null) throw new IllegalStateException("Working vector cannot be created for an uninitialized or idling GridDomain (This domain's underlying matrix is null!)");
        return new DMatrixRMaj(matrixA.getNumRows(), 1);
    }

    @Override
    public String toString() {
        return "'" + status + "' (held for " + DurationFormatUtils.formatDuration(System.currentTimeMillis() - lastUpdateTime, "ss.SSS") + "s)";
    }

    @Override
    public void dispose() {
        matrixA = null; matrixX = null; matrixB = null;
        referenceNode = null;
        setStatus(ConvergenceStatus.DISPOSED);
    }

    @Override
    public boolean hasBeenDisposed() {
        return status == ConvergenceStatus.DISPOSED;
    }
}
