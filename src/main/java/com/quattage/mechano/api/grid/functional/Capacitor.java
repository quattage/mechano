package com.quattage.mechano.api.grid.functional;

import com.quattage.mechano.api.grid.solver.NodalSnapshot;
import com.quattage.mechano.api.grid.topology.Circuit;
import com.quattage.mechano.api.grid.topology.CircuitComponent.StampingComponent;
import com.quattage.mechano.api.grid.topology.Terminal;

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
    public void stamp(Circuit circuit, NodalSnapshot snapshot) {
        prevVoltage = terminals[0].getJoint().getVoltage() - terminals[1].getJoint().getVoltage();
        double g = (double)capacitance / Circuit.DELTA;
        int pI = terminals[0].getJoint().getIndex();
        int nI = terminals[1].getJoint().getIndex();
        double ieq = g * prevVoltage;
        if(pI >= 0) {
            snapshot.stampMatrix(pI, pI, g);
            snapshot.stampRHS(pI, ieq);
        }
        if(nI >= 0) {
            snapshot.stampMatrix(nI, nI, g);
            snapshot.stampRHS(nI, ieq);
        }
        if(pI >= 0 && nI >= 0) {
            snapshot.stampMatrix(pI, nI, -g);
            snapshot.stampMatrix(nI, pI, -g);
        }
    }

    @Override
    public boolean isVoltageSource() {
        return false;
    }
}
