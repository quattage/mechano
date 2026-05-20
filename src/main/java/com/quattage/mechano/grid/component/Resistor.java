package com.quattage.mechano.grid.component;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.grid.Netlist;
import com.quattage.mechano.grid.ServerGrid;
import com.quattage.mechano.grid.topology.core.StampingComponent;
import com.quattage.mechano.grid.topology.core.Terminal;

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
    public void stamp(ServerGrid grid, Netlist netlist) {
        double g = 1d / (double)ohms;
        int aI = grid.coordOf(pinA());
        int bI = grid.coordOf(pinB());
        if(aI >= 0) netlist.stampA(aI, aI, g);
        if(bI >= 0) netlist.stampA(bI, bI, g);
        if(aI >= 0 && bI >= 0) {
            netlist.stampA(aI, bI, -g);
            netlist.stampA(bI, aI, -g);
        }
    }

    @Override
    public @Nullable Terminal pinB() {
        return terminals[1];
    }
}
