package com.quattage.mechano.api.grid.functional;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.topology.Circuit;
import com.quattage.mechano.api.grid.topology.CircuitComponent.StampingComponent;
import com.quattage.mechano.api.grid.topology.vertex.Terminal;

public class Capacitor extends StampingComponent {

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
    public void stamp(ServerGrid grid) {
        prevVoltage = terminals[0].getNode().getVoltage() - terminals[1].getNode().getVoltage();
        double g = (double)capacitance / Circuit.DELTA;
        int pI = terminals[0].getNode().getIndex();
        int nI = terminals[1].getNode().getIndex();
        double ieq = g * prevVoltage;
        if(pI >= 0) {
            grid.stampMatrix(pI, pI, g);
            grid.stampRHS(pI, ieq);
        }
        if(nI >= 0) {
            grid.stampMatrix(nI, nI, g);
            grid.stampRHS(nI, ieq);
        }
        if(pI >= 0 && nI >= 0) {
            grid.stampMatrix(pI, nI, -g);
            grid.stampMatrix(nI, pI, -g);
        }
    }

    @Override
    public boolean isVoltageSource() {
        return false;
    }
}
