package com.quattage.mechano.api.grid.component.impl;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.component.StampingComponent;
import com.quattage.mechano.api.grid.topology.GridDomain;
import com.quattage.mechano.api.grid.topology.landmark.Terminal;

public class Resistor extends StampingComponent {

    private final float ohms;

    public Resistor(float ohms) {
        super("Resistor");
        this.ohms = ohms;
    }

    protected Resistor(String name, float ohms) {
        super(name);
        this.ohms = ohms;
    }

    @Override
    protected Terminal[] defineTerminals() {
        return Terminal.pair(this);
    }
    
    /**
     * @return The resistance (in ohms) of this resistor
     */
    public float getResistance() {
        return ohms;
    }

    @Override
    public void stamp(ServerGrid grid, GridDomain domain) {
        double g = 1d / (double)ohms;
        int aI = indexOf(domain, pinA());
        int bI = indexOf(domain, pinB());
        if(aI >= 0) domain.stampA(aI, aI, g);
        if(bI >= 0) domain.stampA(bI, bI, g);
        if(aI >= 0 && bI >= 0) {
            domain.stampA(aI, bI, -g);
            domain.stampA(bI, aI, -g);
        }
    }

    @Override
    public @Nullable Terminal pinB() {
        return terminals[1];
    }
}
