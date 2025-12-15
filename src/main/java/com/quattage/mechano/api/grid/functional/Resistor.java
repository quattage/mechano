package com.quattage.mechano.api.grid.functional;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.grid.solver.NodalSnapshot;
import com.quattage.mechano.api.grid.topology.Circuit;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.grid.topology.CircuitComponent.StampingComponent;
import com.quattage.mechano.api.grid.topology.vertex.Terminal;

public class Resistor extends StampingComponent {

    private final float ohms;

    public Resistor(float ohms) {
        super("Resistor");
        this.ohms = ohms;
    }

    @Override
    protected Terminal[] defineTerminals() {
        return Terminal.pair(this);
    }

    @Override
    public @Nullable CircuitComponent getParentComponent() {
        return terminals[0].getParentComponent();
    }

    @Override
    public boolean isVoltageSource() {
        return false;
    }

    @Override
    public void stamp(Circuit circuit, NodalSnapshot snapshot) {
        double g = 1d / (double)ohms;
        int aI = terminals[0].getJoint().getIndex();
        int bI = terminals[0].getJoint().getIndex();
        if(aI >= 0) snapshot.stampMatrix(aI, aI, g);
        if(bI >- 0) snapshot.stampMatrix(bI, bI, g);
        if(aI >= 0 && bI >= 0) {
            snapshot.stampMatrix(aI, bI, -g);
            snapshot.stampMatrix(bI, aI, -g);
        }
    }

    @Override
    public @Nullable Terminal pinA() {
        return terminals[0];
    }

    @Override
    public @Nullable Terminal pinB() {
        return terminals[1];
    }
}
