package com.quattage.mechano.api.grid.solver;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.NormOps_DDRM;
import org.ejml.dense.row.mult.VectorVectorMult_DDRM;
import org.ejml.sparse.csc.CommonOps_DSCC;

import com.quattage.mechano.api.ServerGrid;

/**
 * A SPICE-like solver based on the biconjugate gradient stabilized method.
 * https://en.wikipedia.org/wiki/Biconjugate_gradient_stabilized_method
 * https://mathworld.wolfram.com/BiconjugateGradientStabilizedMethod.html
 * https://www.cfd-online.com/Wiki/Biconjugate_gradient_stabilized_method
 * https://arxiv.org/html/2404.13216v1
 */
public class StabilizedBiconjucateSolver implements NodalSolver {

    private DMatrixRMaj r, rHat, p, v, s, t, h, temp;
    private double rho, rhoOld, alpha, beta, omega, d, normS, normR;

    public StabilizedBiconjucateSolver() {}

    @Override
    public String describeSelf() {
        return "BiCGSTAB (no preconditioning)";
    }

    @Override
    public void initialize(ServerGrid grid) {
        r = grid.createWorkingVector();
        rHat = new DMatrixRMaj();
        p = grid.createWorkingVector();
        v = grid.createWorkingVector();
        s = grid.createWorkingVector();
        t = grid.createWorkingVector();
        h = grid.createWorkingVector();
        temp = grid.createWorkingVector();
    }

    @Override
    public ConvergenceStatus run(ServerGrid grid) {

        CommonOps_DSCC.mult(grid.getMatrix(), grid.getSolution(), temp);
        CommonOps_DDRM.subtract(grid.getVoltages(), temp, r);
        rHat.setTo(r);
        rho = VectorVectorMult_DDRM.innerProd(rHat, r);
        rhoOld = 1; alpha = 1; omega = 1;
        p.setTo(r);

        for(int i = 0; i < NodalSolver.STEP_LIMIT; i++) {

            CommonOps_DSCC.mult(grid.getMatrix(), p, v);
            d = VectorVectorMult_DDRM.innerProd(rHat, v);
            if(Math.abs(d) < (NodalSolver.EPSILON * 0.1d)) 
                return ConvergenceStatus.ABORTED_PROBLEMATIC_DATA;

            alpha = rho / d;
            CommonOps_DDRM.add(alpha, p, 1d, grid.getSolution(), h);
            CommonOps_DDRM.add(-alpha, v, 1d, r, s);
            normS = NormOps_DDRM.normF(s);
            if(normS < NodalSolver.EPSILON) {
                grid.getSolution().setTo(h);
                return ConvergenceStatus.FINISHED_SOLVED_EARLY;
            }

            CommonOps_DSCC.mult(grid.getMatrix(), s, t);
            omega = VectorVectorMult_DDRM.innerProd(t, s) / VectorVectorMult_DDRM.innerProd(t, t);

            CommonOps_DDRM.add(omega, s, 1d, h, grid.getSolution());
            CommonOps_DDRM.add(-omega, t, 1d, s, r);
            normR = NormOps_DDRM.normF(r);
            if(normR < NodalSolver.EPSILON)
                return ConvergenceStatus.FINISHED_SOLVED_LATE;

            rhoOld = rho;
            rho = VectorVectorMult_DDRM.innerProd(rHat, r);

            beta = (rho / rhoOld) * (alpha / omega);

            CommonOps_DDRM.add(-omega, v, 1d, p, temp);
            CommonOps_DDRM.add(beta, temp, 1d, r, p);
        }

        return ConvergenceStatus.FINISHED_LIMIT_REACHED;
    }

    @Override
    public void reset() {
        rho = 0; rhoOld = 0; alpha = 0; beta = 0; omega = 0; d = 0; normS = 0; normR = 0; 
        r= null; rHat = null; p = null; v = null; s = null; t = null; h = null; temp = null;
    }
}
