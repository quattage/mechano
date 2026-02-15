package com.quattage.mechano.api.grid.component.impl;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.VoltageDecay;
import com.quattage.mechano.api.grid.component.StampingComponent;
import com.quattage.mechano.api.grid.component.StampingComponent.NeedsPostProcessing;
import com.quattage.mechano.api.grid.component.StampingComponent.StampsDynamically;
import com.quattage.mechano.api.grid.topology.GridDomain;
import com.quattage.mechano.api.grid.topology.landmark.Terminal;

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
    public void stamp(ServerGrid grid, GridDomain domain) {
        int a = indexOf(domain, terminals[0]);
        int b = indexOf(domain, terminals[1]);
        int i = domain.indexer().get(this);
        domain.stampA(a, i, 1);
        domain.stampA(i, a, 1);
        domain.stampA(b, i, -1);
        domain.stampA(i, b, -1);
    }

    @Override
    public void stampDynamic(ServerGrid grid, GridDomain domain) {
        domain.stampB(domain.indexer().get(this), getVolts());
    }

    @Override
    public void postProcess(ServerGrid grid, GridDomain domain) {
        int idx = domain.indexer().get(this);
        if(idx < 0) {
            grid.warn("Skipped post-process step for " + this);
            return;
        }
        previous = state.copy();
        state.volts(getVolts());
        state.amps(domain.solution().get(idx, 0));
        if(cc != null) cc.onCurrentUpdated(grid, previous, state);
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
