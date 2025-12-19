package com.quattage.mechano.api.grid.functional;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.VoltageDecay;
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
    public abstract int getContributionFactor();

    @Override
    public void stamp(ServerGrid grid) {
        int pI = terminals[0].getNode().getIndex();
        int nI = terminals[1].getNode().getIndex();
        cIndex = grid.allocateSource();
        if(pI >= 0) {
            grid.stampMatrix(pI, cIndex, 1);
            grid.stampMatrix(cIndex, pI, 1);
        }
        if(nI >= 0) {
            grid.stampMatrix(nI, cIndex, -1);
            grid.stampMatrix(cIndex, nI, -1);
        }
        grid.stampRHS(cIndex, volts.apply(getStateOfCharge()));
    }
}
