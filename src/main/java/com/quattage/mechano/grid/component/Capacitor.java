package com.quattage.mechano.grid.component;

import com.quattage.mechano.grid.Netlist;
import com.quattage.mechano.grid.ServerGrid;
import com.quattage.mechano.grid.solver.SolverAlgorithm;
import com.quattage.mechano.grid.topology.core.StampingComponent;
import com.quattage.mechano.grid.topology.core.StampingComponent.StampsDynamically;
import com.quattage.mechano.grid.topology.core.Terminal;

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
    public void stamp(ServerGrid grid, Netlist netlist) {
        double g = capacitance / SolverAlgorithm.DELTA;
        int pI = grid.coordOf(terminals[0]);
        int nI = grid.coordOf(terminals[1]);
        if(pI >= 0) netlist.stampA(pI, pI,  g);
        if(nI >= 0) netlist.stampA(nI, nI,  g);
        if(pI >= 0 && nI >= 0) {
            netlist.stampA(pI, nI, -g);
            netlist.stampA(nI, pI, -g);
        }
    }

    @Override
    public void stampDynamic(ServerGrid grid, Netlist netlist) {
        double vNow = voltageOf(netlist, terminals[0]) - voltageOf(netlist, terminals[1]);
        double ieq = (capacitance / SolverAlgorithm.DELTA) * prevVoltage;
        int pI = grid.coordOf(terminals[0]);
        int nI = grid.coordOf(terminals[1]);
        if(pI >= 0) netlist.stampB(pI,  ieq);
        if(nI >= 0) netlist.stampB(nI, -ieq);
        prevVoltage = vNow;
    }
}
