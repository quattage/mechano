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
    public int getAllocations() {
        return 1;
    }

    @Override
    public void stamp(ServerGrid grid) {
        int pI = terminals[0].getNode().getNodalIndex();
        int nI = terminals[1].getNode().getNodalIndex();
        cIndex = grid.indexer().get(this);
        if(pI >= 0) {
            grid.stampA(pI, cIndex, 1);
            grid.stampA(cIndex, pI, 1);
        }
        if(nI >= 0) {
            grid.stampA(nI, cIndex, -1);
            grid.stampA(cIndex, nI, -1);
        }
    }

    @Override
    public void stampDynamic(ServerGrid grid) {
        grid.stampB(cIndex, volts.apply(getStateOfCharge()));
    }
}
