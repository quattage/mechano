package com.quattage.mechano.api.grid.functional;

import com.quattage.mechano.api.grid.VoltageDecay;
import com.quattage.mechano.api.grid.solver.NodalSnapshot;
import com.quattage.mechano.api.grid.topology.Circuit;
import com.quattage.mechano.api.grid.topology.CircuitComponent.StampingComponent;

public abstract class VoltageSource extends StampingComponent {

    private final VoltageDecay volts;
    private int cIndex = -1;

    public VoltageSource(String name, VoltageDecay decayFunction) {
        super(name);
        this.volts = decayFunction;
    }

    public abstract double getStateOfCharge();

    @Override
    public boolean isVoltageSource() {
        return true;
    }

    @Override
    public void stamp(Circuit circuit, NodalSnapshot snapshot) {
        int pI = terminals[0].getJoint().getIndex();
        int nI = terminals[1].getJoint().getIndex();
        cIndex = snapshot.allocateSource();
        if(pI >= 0) {
            snapshot.stampMatrix(pI, cIndex, 1);
            snapshot.stampMatrix(cIndex, pI, 1);
        }
        if(nI >= 0) {
            snapshot.stampMatrix(nI, cIndex, -1);
            snapshot.stampMatrix(cIndex, nI, -1);
        }
        snapshot.stampRHS(cIndex, volts.apply(getStateOfCharge()));
    }
}
