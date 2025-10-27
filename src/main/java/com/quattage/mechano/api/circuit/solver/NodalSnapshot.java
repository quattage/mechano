package com.quattage.mechano.api.circuit.solver;

import com.quattage.mechano.api.circuit.topology.Circuit;
import com.quattage.mechano.api.circuit.topology.Joint;
import com.quattage.mechano.foundation.numeric.SparseDoubleMatrix;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class NodalSnapshot {
    
    private SparseDoubleMatrix matrix;
    private double[] rhs;
    private Object2IntOpenHashMap<Joint> indices;
    private int size = 0;
    private int sources = 0;

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
        this.rhs = new double[totalSize()];
        this.matrix = new SparseDoubleMatrix(this.size + sourceCount, this.size + sourceCount);
        this.indices = new Object2IntOpenHashMap<>();
        int index = 0;
        for(Joint joint : circuit.getJoints()) {
            if(joint.isStaticGround()) {
                this.markJoint(joint, -1);
                continue;
            }
            this.markJoint(joint, index);
            index++;
        }
        return this;
    }

    public int indexOf(Joint j) {
        Integer out = indices.getInt(j);
        return out == null ? -1 : out;
    }

    public void markJoint(Joint j, int index) {
        indices.put(j, index);
    }

    public int incrementSourceCount() {
        sources++;
        return sources;
    }

    public int size() {
        return size;
    }

    public int totalSize() {
        return size + sources;
    }

    public SparseDoubleMatrix matrix() {
        return matrix;
    }

    public double[] rhs() {
        return rhs;
    }

    public void stampMatrix(int row, int col, double value) {
        if(row < 0 || col < 0) return;
        matrix.setValue(row, col, matrix.get(row, col) + value);
    }

    public void stampRHS(int index, double value) {
        if(index < 0) return;
        rhs[index] = value;
    }

    /**
     * Indicates that implementing subclasses stamp conductance
     * and source terms to the NodalSnapshot.
     */
    public static interface Stamper {
        /**
         * "Stamping" refers to the process of an individual CircuitComponent
         * declaring its own presence in the NodalSnapshot. This method
         * is used to initialize each {@link Circuit#beginSolverStep
         * solver step} of the {@link Circuit}
         */
        public abstract void stamp(Circuit circuit, NodalSnapshot snapshot);
        /**
         * CircuitComponent subclasses whose function is to induce an
         * external charge on the circuit are considered to be 
         * anonymous voltage sources. Batteries should return
         * true here.
         * @return <code>true</code> if this stamper object represents a source of voltage
         */
        public default boolean isVoltageSource() { return false; }
    }
}
