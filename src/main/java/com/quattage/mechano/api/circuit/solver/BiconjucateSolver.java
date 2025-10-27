package com.quattage.mechano.api.circuit.solver;

import com.quattage.mechano.foundation.numeric.SparseDoubleMatrix;

/**
 * A solver based on the biconjugate gradient stabilized method.
 * (https://en.wikipedia.org/wiki/Biconjugate_gradient_stabilized_method)
 * Uses the {@link NodalSnapshot} as a container/context object to process
 * one discrete timestep.
 */
public class BiconjucateSolver implements NodalSolver {

    @Override
    public double[] solve(NodalSnapshot snapshot) {
        int n = snapshot.totalSize();

        double[] x = new double[n]; // initial guess = 0 (no preeconditioning)
        double[] r = subtract(snapshot.rhs(), multiply(snapshot.matrix(), x));
        double[] rHat = r.clone();

        double rho = 1, alpha = 1, omega = 1;
        double[] v = new double[n];
        double[] p = new double[n];

        double normB = norm(snapshot.rhs());
        if(normB == 0) normB = 1;

        for(int iter = 0; iter < STEP_LIMIT; iter++) {
            double rhoNew = dot(rHat, r);
            if(Math.abs(rhoNew) < EPSILON) break;

            double beta = (rhoNew / rho) * (alpha / omega);
            rho = rhoNew;

            p = add(r, scale(subtract(p, scale(v, omega)), beta));
            v = multiply(snapshot.matrix(), p);
            alpha = rho / dot(rHat, v);

            double[] s = subtract(r, scale(v, alpha));
            if(norm(s) / normB < EPSILON) {
                x = add(x, scale(p, alpha));
                break;
            }

            double[] t = multiply(snapshot.matrix(), s);
            omega = dot(t, s) / dot(t, t);
            x = add(x, add(scale(p, alpha), scale(s, omega)));
            r = subtract(s, scale(t, omega));

            if(norm(r) / normB < EPSILON) break;
        }

        return x;
    }
    
}
