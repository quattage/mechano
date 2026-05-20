package com.quattage.mechano.grid.solver;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.NormOps_DDRM;
import org.ejml.dense.row.mult.VectorVectorMult_DDRM;
import org.ejml.sparse.csc.CommonOps_DSCC;

import com.quattage.mechano.grid.Netlist;

/**
 * A SPICE-like solver based on the biconjugate gradient stabilized method.
 * https://en.wikipedia.org/wiki/Biconjugate_gradient_stabilized_method <-- this article kinda sucks for explaining but the math is solid
 * https://mathworld.wolfram.com/BiconjugateGradientStabilizedMethod.html
 * https://www.cfd-online.com/Wiki/Biconjugate_gradient_stabilized_method
 * https://arxiv.org/html/2404.13216v1
 */
public class BiCGStab implements SolverAlgorithm {

    protected DMatrixRMaj r, rHat, p, v, s, t, h, ax;
    protected double rho, rhoOld, alpha, beta, omega, norm;

    @Override
    public String describeSelf() {
        return "BiCGStab Inline";
    }

    @Override
    public void initialize(Netlist netlist) {
        r = netlist.createWorkingVector();
        rHat = netlist.createWorkingVector();
        p = netlist.createWorkingVector();
        v = netlist.createWorkingVector();
        s = netlist.createWorkingVector();
        t = netlist.createWorkingVector();
        h = netlist.createWorkingVector();
        ax = netlist.createWorkingVector();
    }

    protected double getInitialResidual() {
        rHat.setTo(r);
        return CommonOps_DDRM.dot(rHat, r);
    }

    @Override
    public ConvergenceStatus run(Netlist netlist) {

        CommonOps_DSCC.mult(netlist.matrix(), netlist.solution(), ax);
        CommonOps_DDRM.subtract(netlist.terms(), ax, r);
        rho = getInitialResidual();
        p.setTo(r);

        for(int i = 0; i < SolverAlgorithm.STEP_LIMIT; i++) {

            CommonOps_DSCC.mult(netlist.matrix(), p, v);
            alpha = rho / CommonOps_DDRM.dot(rHat, v);
            CommonOps_DDRM.add(netlist.solution(), alpha, p, h);
            CommonOps_DDRM.add(r, -alpha, v, s);
            norm = NormOps_DDRM.normP2(s);
            if(norm < SolverAlgorithm.EPSILON) {
                netlist.solution().setTo(h);
                return ConvergenceStatus.FINISHED_SOLVED;
            }

            CommonOps_DSCC.mult(netlist.matrix(), s, t);
            omega = CommonOps_DDRM.dot(t, s) / CommonOps_DDRM.dot(t, t);

            CommonOps_DDRM.add(h, omega, s, netlist.solution());
            CommonOps_DDRM.add(s, -omega, t, r);
            norm = NormOps_DDRM.normF(r);
            if(norm < SolverAlgorithm.EPSILON)
                return ConvergenceStatus.FINISHED_SOLVED_LATE;

            rhoOld = rho;
            rho = VectorVectorMult_DDRM.innerProd(rHat, r);
            beta = (rho / rhoOld) * (alpha / omega);
            CommonOps_DDRM.add(p, -omega, v, t);
            CommonOps_DDRM.add(r, beta, t, p);
        }

        return ConvergenceStatus.FINISHED_LIMIT_REACHED;
    }

    @Override
    public void dispose() {
        rho = 0; rhoOld = 0; alpha = 0; beta = 0; omega = 0; norm = 0; 
        r=  null; rHat = null; p = null; v = null; s = null; t = null; h = null; ax = null;
    }

    @Override
    public boolean hasBeenDisposed() {
        return false;
    }
    
}
