package com.quattage.mechano.api.grid.solver;

import java.util.Random;

import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.RandomMatrices_DDRM;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.topology.GridDomain;

public class BiCGStabRandom extends BiCGStab {

    private Random random;

    public BiCGStabRandom() {}

    @Override
    public String describeSelf() {
        return "BiCGStab (random)";
    }

    @Override
    public void initialize(ServerGrid grid, GridDomain domain) {
        super.initialize(grid, domain);
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
