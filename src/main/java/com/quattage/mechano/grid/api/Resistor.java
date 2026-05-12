package com.quattage.mechano.grid.api;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.GridDomain;
import com.quattage.mechano.grid.api.component.StampingComponent;
import com.quattage.mechano.grid.solver.NodalSolver;
import com.quattage.mechano.grid.topology.Terminal;

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
    public void stamp(GridDomain domain, NodalSolver solver) {
        double g = 1d / (double)ohms;
        int aI = indexOf(domain, pinA());
        int bI = indexOf(domain, pinB());
        if(aI >= 0) solver.stampA(aI, aI, g);
        if(bI >= 0) solver.stampA(bI, bI, g);
        if(aI >= 0 && bI >= 0) {
            solver.stampA(aI, bI, -g);
            solver.stampA(bI, aI, -g);
        }
    }

    @Override
    public @Nullable Terminal pinB() {
        return terminals[1];
    }
}
