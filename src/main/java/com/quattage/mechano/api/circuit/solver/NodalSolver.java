package com.quattage.mechano.api.circuit.solver;

import com.quattage.mechano.foundation.numeric.SparseDoubleMatrix;

public interface NodalSolver {
    
    public static final double EPSILON = 0.00001d;
    public static final int STEP_LIMIT = 255;

    public abstract double[] solve(NodalSnapshot snapshot);

    public default double[] multiply(SparseDoubleMatrix matrix, double[] mul) {
        int rows = matrix.rows();
        double[] result = new double[rows];
        for(int i = 0; i < rows; i++) {
            var rowMap = matrix.getRow(i);
            if(rowMap == null) continue;
            for(var entry : rowMap.int2DoubleEntrySet())
                result[i] += entry.getDoubleValue() * mul[entry.getIntKey()];
        }
        return result;
    }

    public default double[] subtract(double[] a, double[] b) {
        double[] out = new double[a.length];
        for(int i = 0; i < a.length; i++) out[i] = a[i] - b[i];
        return out;
    }

    public default double[] add(double[] a, double[] b) {
        double[] out = new double[a.length];
        for(int i = 0; i < a.length; i++) out[i] = a[i] + b[i];
        return out;
    }

    public default double[] scale(double[] a, double s) {
        double[] out = new double[a.length];
        for(int i = 0; i < a.length; i++) out[i] = a[i] * s;
        return out;
    }

    public default  double dot(double[] a, double[] b) {
        double sum = 0;
        for(int i = 0; i < a.length; i++) sum += a[i] * b[i];
        return sum;
    }

    public default double norm(double[] a) {
        return Math.sqrt(dot(a, a));
    }
}
