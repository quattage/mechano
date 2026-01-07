package com.quattage.mechano.api.grid.functional;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.topology.Circuit;
import com.quattage.mechano.api.grid.topology.CircuitComponent.StampingComponent;
import com.quattage.mechano.api.grid.topology.CircuitComponent.StampsDynamically;
import com.quattage.mechano.api.grid.topology.vertex.Terminal;

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
    public void stamp(ServerGrid grid) {
        double g = capacitance / Circuit.DELTA;
        int pI = terminals[0].getNode().getNodalIndex();
        int nI = terminals[1].getNode().getNodalIndex();
        if(pI >= 0) grid.stampA(pI, pI,  g);
        if(nI >= 0) grid.stampA(nI, nI,  g);
        if(pI >= 0 && nI >= 0) {
            grid.stampA(pI, nI, -g);
            grid.stampA(nI, pI, -g);
        }
    }

    @Override
    public void stampDynamic(ServerGrid grid) {
        double vNow = terminals[0].getNode().getVoltage(grid) - terminals[1].getNode().getVoltage(grid);
        double ieq = (capacitance / Circuit.DELTA) * prevVoltage;
        int pI = terminals[0].getNode().getNodalIndex();
        int nI = terminals[1].getNode().getNodalIndex();
        if(pI >= 0) grid.stampB(pI,  ieq);
        if(nI >= 0) grid.stampB(nI, -ieq);
        prevVoltage = vNow;
    }
}
