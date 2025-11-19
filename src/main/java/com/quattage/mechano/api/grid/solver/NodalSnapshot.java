package com.quattage.mechano.api.grid.solver;

import java.util.concurrent.atomic.AtomicReference;

import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;

import com.quattage.mechano.api.grid.solver.NodalSolver.ConvergenceStatus;
import com.quattage.mechano.api.grid.topology.Circuit;

public class NodalSnapshot {
    
    private DMatrixSparseCSC A;
    private DMatrixRMaj x;
    private DMatrixRMaj b;
    private int size = 0;
    private int sources = 0;
    protected AtomicReference<ConvergenceStatus> status = new AtomicReference<>(ConvergenceStatus.UNFINISHED_UNPOPULATED);

    public NodalSnapshot() {}

    /**
     * Initially configures this snapshot at the beginning
     * of an update tick.
     * @param circuit Circuit to load
     * @param size The total amount of individual balancing nodes (joints) in the circuit
     * @param sourceCount The total amount of voltage sources in the circuit
     * @return This NodalSnapshot, modified as a result of this call
     */
    public NodalSnapshot loadOnto(Circuit circuit, int size, int sourceCount) {
        this.size = size - 1;  // its assumed that there's always one ground joint
        this.sources = sourceCount;
        this.b = createWorkingVector();
        this.x = createWorkingVector();
        this.A = new DMatrixSparseCSC(this.size + sourceCount, this.size + sourceCount);
        return this;
    }

    public int allocateSource() {
        return size + sources++;
    }

    public int size() {
        return size;
    }

    public int totalSize() {
        return size + sources;
    }

    public int matrixArea() {
        return (size * size) + sources;
    }

    /**
     * Ax=b
     * @return A (Matrix [n * n])
     */
    public DMatrixSparseCSC termA() {
        return A;
    }

    /**
     * Ax=b
     * @return x (Vector[n])
     */
    public DMatrixRMaj termX() {
        return x;
    }

    /**
     * Ax=b
     * @return b (Vector[n])
     */
    public DMatrixRMaj termB() {
        return b;
    }

    public DMatrixRMaj createWorkingVector() {
        return new DMatrixRMaj(A.getNumRows());
    }

    public void stampMatrix(int row, int col, double value) {
        A.unsafe_set(row, col, A.get(row, col) + value);
    }

    public void stampRHS(int index, double value) {
        if(index < 0) return;
        b.set(index, value);
    }

    public ConvergenceStatus getStatus() {
        return status.get();
    }

    /**
     * Indicates that implementing subclasses stamp conductance
     * and source terms to the NodalSnapshot.
     */
    public interface Stamper {
        /**
         * "Stamping" refers to the process of an individual CircuitComponent
         * declaring its own presence in the NodalSnapshot. This method
         * is used to initialize each solver step of the {@link Circuit}
         */
        void stamp(Circuit circuit, NodalSnapshot snapshot);
        /**
         * CircuitComponent subclasses whose function is to induce an
         * external charge on the circuit are considered to be 
         * anonymous voltage sources. Batteries should return
         * true here.
         * @return <code>true</code> if this stamper object represents a source of voltage
         */
        default boolean isVoltageSource() { return false; }
    }
}
