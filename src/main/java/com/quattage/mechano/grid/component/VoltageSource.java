package com.quattage.mechano.grid.component;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.grid.Netlist;
import com.quattage.mechano.grid.ServerGrid;
import com.quattage.mechano.grid.topology.core.StampingComponent;
import com.quattage.mechano.grid.topology.core.StampingComponent.NeedsPostProcessing;
import com.quattage.mechano.grid.topology.core.StampingComponent.StampsDynamically;
import com.quattage.mechano.grid.topology.core.Terminal;

public abstract class VoltageSource extends StampingComponent implements StampsDynamically, NeedsPostProcessing {

    protected final VoltageDecay decayFunction;
    private final @Nullable CurrentChangeCallback cc;
    private @Nullable PowerState previous;
    private final PowerState state = new PowerState();

    public VoltageSource(String name, VoltageDecay decayFunction) { this(name, decayFunction, null); }
    public VoltageSource(String name, VoltageDecay decayFunction, @Nullable CurrentChangeCallback cc) {
        super(name);
        this.decayFunction = decayFunction;
        this.cc = cc;
    }

    @Override
    protected Terminal[] defineTerminals() {
        return Terminal.polarPair(this);
    }

    /**
     * State of charge is described as a percentage of remaining energy
     * in this voltage source.
     * @return A scalar value representing fullness percent, like a fuel gauge but for electricity.
     */
    public abstract double getStateOfCharge();

    /**
     * This method uses this VoltageSource's internal {@link Voltage decay function}
     * to determine the voltage that exists between its two terminals at the current
     * {@link #getStateOfCharge() state of charge}.
     * @return Volts, how much voltage this battery will stamp into the system at the time of invocation.
     */
    public double getVolts() {
        return decayFunction.apply(getStateOfCharge());
    }

    @Override
    public int getAllocations() {
        return 1;
    }

    @Override
    public void stamp(ServerGrid grid, Netlist netlist) {
        int a = grid.coordOf(terminals[0]);
        int b = grid.coordOf(terminals[1]);
        int i = grid.coordOf(this);
        netlist.stampA(a, i, 1);
        netlist.stampA(i, a, 1);
        netlist.stampA(b, i, -1);
        netlist.stampA(i, b, -1);
    }

    @Override
    public void stampDynamic(ServerGrid grid, Netlist netlist) {
        netlist.stampB(grid.coordOf(this), getVolts());
    }

    @Override
    public void postProcess(ServerGrid grid, Netlist netlist) {
        int idx = grid.coordOf(this);
        if(idx < 0) return;
        previous = state.copy();
        state.volts(getVolts());
        state.amps(netlist.solution().get(idx, 0));
        if(cc != null) cc.onCurrentUpdated(netlist, previous, state);
    }

    @Override
    public @Nullable Terminal positive() {
        return terminals[0];
    }

    @Override
    public @Nullable Terminal negative() {
        return terminals[1];
    }

    @Override
    public @Nullable Terminal anode() {
        return positive();
    }

    @Override
    public @Nullable Terminal cathode() {
        return negative();
    }

    @Override
    public @Nullable Terminal pinA() {
        return positive();
    }
    
    @Override
    public @Nullable Terminal pinB() {
        return negative();
    }

    public PowerState getPowerState() {
        return state;
    }
}
