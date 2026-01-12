package com.quattage.mechano.api.grid.component.impl;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.component.StampingComponent;
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
    
    /**
     * @return The resistance (in ohms) of this resistor
     */
    public float getResistance() {
        return ohms;
    }

    @Override
    public void stamp(ServerGrid grid) {
        double g = 1d / (double)ohms;
        int aI = pinA().getAttachedNode().getNodalIndex();
        int bI = pinB().getAttachedNode().getNodalIndex();
        if(aI >= 0) grid.stampA(aI, aI, g);
        if(bI >= 0) grid.stampA(bI, bI, g);
        if(aI >= 0 && bI >= 0) {
            grid.stampA(aI, bI, -g);
            grid.stampA(bI, aI, -g);
        }
    }

    @Override
    public @Nullable Terminal pinB() {
        return terminals[1];
    }
}
