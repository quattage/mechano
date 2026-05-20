package com.quattage.mechano.grid.component;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.grid.Netlist;
import com.quattage.mechano.grid.ServerGrid;
import com.quattage.mechano.grid.topology.core.StampingComponent.NeedsPostProcessing;
import com.quattage.mechano.grid.topology.core.Terminal;

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
    public void postProcess(ServerGrid grid, Netlist netlist) {
        previous = state.copy();
        state.volts(voltageOf(netlist, pinA()) - voltageOf(netlist, pinB()));
        state.amps(state.volts() / (double)getResistance());
        if(cc != null && !state.equals(previous))
            cc.onCurrentUpdated(netlist, previous, state);
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
