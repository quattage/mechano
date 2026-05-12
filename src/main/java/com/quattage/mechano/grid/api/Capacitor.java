package com.quattage.mechano.grid.api;

import com.quattage.mechano.api.GridDomain;
import com.quattage.mechano.grid.api.component.StampingComponent;
import com.quattage.mechano.grid.api.component.StampingComponent.StampsDynamically;
import com.quattage.mechano.grid.solver.NodalSolver;
import com.quattage.mechano.grid.solver.SolverMethod;
import com.quattage.mechano.grid.topology.Terminal;

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
    public void stamp(GridDomain domain, NodalSolver solver) {
        double g = capacitance / SolverMethod.DELTA;
        int pI = indexOf(domain, terminals[0]);
        int nI = indexOf(domain, terminals[1]);
        if(pI >= 0) solver.stampA(pI, pI,  g);
        if(nI >= 0) solver.stampA(nI, nI,  g);
        if(pI >= 0 && nI >= 0) {
            solver.stampA(pI, nI, -g);
            solver.stampA(nI, pI, -g);
        }
    }

    @Override
    public void stampDynamic(GridDomain domain, NodalSolver solver) {
        double vNow = voltageOf(domain, terminals[0]) - voltageOf(domain, terminals[1]);
        double ieq = (capacitance / SolverMethod.DELTA) * prevVoltage;
        int pI = indexOf(domain, terminals[0]);
        int nI = indexOf(domain, terminals[1]);
        if(pI >= 0) solver.stampB(pI,  ieq);
        if(nI >= 0) solver.stampB(nI, -ieq);
        prevVoltage = vNow;
    }
}
