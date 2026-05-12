package com.quattage.mechano.grid.api;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.GridDomain;
import com.quattage.mechano.grid.api.component.StampingComponent.NeedsPostProcessing;
import com.quattage.mechano.grid.solver.NodalSolver;
import com.quattage.mechano.grid.topology.Terminal;

public class HeatingElement extends Resistor implements NeedsPostProcessing {

    private final @Nullable CurrentChangeCallback cc;
    private @Nullable PowerState previous;
    private final PowerState state = new PowerState();

    public HeatingElement(float ohms) { this(ohms, null); }
    public HeatingElement(float ohms, CurrentChangeCallback cc) {
        super("HeatingElement", ohms);
        this.cc = cc;
    }

    @Override
    public void postProcess(GridDomain domain, NodalSolver solver) {
        previous = state.copy();
        state.volts(voltageOf(domain, pinA()) - voltageOf(domain, pinB()));
        state.amps(state.volts() / (double)getResistance());
        if(cc != null && !state.equals(previous))
            cc.onCurrentUpdated(domain, previous, state);
    }

    @Override
    public @Nullable Terminal pinA() {
        return terminals[0];
    }

    @Override
    public @Nullable Terminal pinB() {
        return terminals[1];
    }

    public PowerState getPowerState() {
        return state;
    }
}
