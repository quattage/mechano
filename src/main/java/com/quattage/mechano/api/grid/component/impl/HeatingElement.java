package com.quattage.mechano.api.grid.component.impl;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.component.StampingComponent.NeedsPostProcessing;
import com.quattage.mechano.api.grid.topology.GridDomain;
import com.quattage.mechano.api.grid.topology.landmark.Terminal;

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
    public void postProcess(ServerGrid grid, GridDomain domain) {
        previous = state.copy();
        state.volts(voltageOf(domain, pinA()) - voltageOf(domain, pinB()));
        state.amps(state.volts() / (double)getResistance());
        if(cc != null && !state.equals(previous))
            cc.onCurrentUpdated(grid, previous, state);
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
