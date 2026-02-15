package com.quattage.mechano.api.grid.component.impl;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.component.StampingComponent;
import com.quattage.mechano.api.grid.component.StampingComponent.StampsDynamically;
import com.quattage.mechano.api.grid.solver.NodalSolver;
import com.quattage.mechano.api.grid.topology.GridDomain;
import com.quattage.mechano.api.grid.topology.landmark.Terminal;

public class Capacitor extends StampingComponent implements StampsDynamically {

    private final float capacitance;
    private double prevVoltage;

    public Capacitor(float capacitance) {
        super("Capacitor");
        this.capacitance = capacitance;
    }

    @Override
    protected Terminal[] defineTerminals() {
        return Terminal.polarPair(this);
    }

    @Override
    public int getAllocations() {
        return 0;
    }

    @Override
    public void stamp(ServerGrid grid, GridDomain domain) {
        double g = capacitance / NodalSolver.DELTA;
        int pI = indexOf(domain, terminals[0]);
        int nI = indexOf(domain, terminals[1]);
        if(pI >= 0) domain.stampA(pI, pI,  g);
        if(nI >= 0) domain.stampA(nI, nI,  g);
        if(pI >= 0 && nI >= 0) {
            domain.stampA(pI, nI, -g);
            domain.stampA(nI, pI, -g);
        }
    }

    @Override
    public void stampDynamic(ServerGrid grid, GridDomain domain) {
        double vNow = voltageOf(domain, terminals[0]) - voltageOf(domain, terminals[1]);
        double ieq = (capacitance / NodalSolver.DELTA) * prevVoltage;
        int pI = indexOf(domain, terminals[0]);
        int nI = indexOf(domain, terminals[1]);
        if(pI >= 0) domain.stampB(pI,  ieq);
        if(nI >= 0) domain.stampB(nI, -ieq);
        prevVoltage = vNow;
    }
}
