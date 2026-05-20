package com.quattage.mechano.grid.solver;

import java.util.Random;

import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.RandomMatrices_DDRM;

import com.quattage.mechano.grid.Netlist;

public class BiCGStabStochastic extends BiCGStab {

    private Random random;

    public BiCGStabStochastic() {}

    @Override
    public String describeSelf() {
        return "BiCGStab Inline Stochastic";
    }

    @Override
    public void initialize(Netlist netlist) {
        super.initialize(netlist);
        random = new Random();
    }

    @Override
    protected double getInitialResidual() {
        RandomMatrices_DDRM.fillUniform(rHat, random);
        double out = CommonOps_DDRM.dot(rHat, r);
        if(out == 0) {
            rHat.setTo(r);
            return 1;
        }
        return out;
    }
}
