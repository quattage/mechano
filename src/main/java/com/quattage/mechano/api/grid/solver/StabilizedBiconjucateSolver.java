package com.quattage.mechano.api.grid.solver;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.NormOps_DDRM;
import org.ejml.dense.row.mult.VectorVectorMult_DDRM;
import org.ejml.sparse.csc.CommonOps_DSCC;

import com.quattage.mechano.api.grid.topology.Circuit;

/**
 * A SPICE-like solver based on the biconjugate gradient stabilized method.
 * https://en.wikipedia.org/wiki/Biconjugate_gradient_stabilized_method
 * https://mathworld.wolfram.com/BiconjugateGradientStabilizedMethod.html
 * https://www.cfd-online.com/Wiki/Biconjugate_gradient_stabilized_method
 * https://arxiv.org/html/2404.13216v1
 */
public class StabilizedBiconjucateSolver implements NodalSolver {

    private DMatrixRMaj r, r_hat, p, v, s, t, h, temp;
    private double rho, rho_old, alpha, beta, omega, d, normS, normR;

    public StabilizedBiconjucateSolver() {}

    @Override
    public ConvergenceStatus run(NodalSnapshot snapshot) {

        r = snapshot.createWorkingVector();
        r_hat = new DMatrixRMaj();
        p = snapshot.createWorkingVector();
        v = snapshot.createWorkingVector();
        s = snapshot.createWorkingVector();
        t = snapshot.createWorkingVector();
        h = snapshot.createWorkingVector();
        temp = snapshot.createWorkingVector();

        CommonOps_DSCC.mult(snapshot.termA(), snapshot.termX(), temp);
        CommonOps_DDRM.subtract(snapshot.termB(), temp, r);
        r_hat.setTo(r);
        rho = VectorVectorMult_DDRM.innerProd(r_hat, r);
        rho_old = 1; alpha = 1; omega = 1;
        p.setTo(r);

        for(int i = 0; i < NodalSolver.STEP_LIMIT; i++) {

            CommonOps_DSCC.mult(snapshot.termA(), p, v);
            d = VectorVectorMult_DDRM.innerProd(r_hat, v);
            if(Math.abs(d) < (NodalSolver.EPSILON * 0.1d)) 
                return ConvergenceStatus.UNFINISHED_PROBLEMATIC_DATA;

            alpha = rho / d;
            CommonOps_DDRM.add(alpha, p, 1d, snapshot.termX(), h);
            CommonOps_DDRM.add(-alpha, v, 1d, r, s);
            normS = NormOps_DDRM.normF(s);
            if(normS < NodalSolver.EPSILON) {
                snapshot.termX().setTo(h);
                return ConvergenceStatus.FINISHED_SOLVED_EARLY;
            }

            CommonOps_DSCC.mult(snapshot.termA(), s, t);
            omega = VectorVectorMult_DDRM.innerProd(t, s) / VectorVectorMult_DDRM.innerProd(t, t);

            CommonOps_DDRM.add(omega, s, 1d, h, snapshot.termX());
            CommonOps_DDRM.add(-omega, t, 1d, s, r);
            normR = NormOps_DDRM.normF(r);
            if(normR < NodalSolver.EPSILON)
                return ConvergenceStatus.FINISHED_SOLVED_LATE;

            rho_old = rho;
            rho = VectorVectorMult_DDRM.innerProd(r_hat, r);

            beta = (rho / rho_old) * (alpha / omega);

            CommonOps_DDRM.add(-omega, v, 1d, p, temp);
            CommonOps_DDRM.add(beta, temp, 1d, r, p);
        }

        return ConvergenceStatus.FINISHED_LIMIT_REACHED;
    }

    @Override
    public void apply(Circuit circuit) {
        
    }

    @Override
    public int estimateMemoryFootprint(NodalSnapshot snapshot) {
        return NodalSolver.estimateMemoryFootprint(8, 8, 0, snapshot.termA().getNumRows());
    }

    @Override
    public void reset() {
        rho = 0; rho_old = 0; alpha = 0; beta = 0; omega = 0; d = 0; normS = 0; normR = 0; 
        r= null; r_hat = null; p = null; v = null; s = null; t = null; h = null; temp = null;
    }
}
