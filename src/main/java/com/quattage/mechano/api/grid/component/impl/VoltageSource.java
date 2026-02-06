package com.quattage.mechano.api.grid.component.impl;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.VoltageDecay;
import com.quattage.mechano.api.grid.component.StampingComponent;
import com.quattage.mechano.api.grid.component.StampingComponent.NeedsPostProcessing;
import com.quattage.mechano.api.grid.component.StampingComponent.StampsDynamically;
import com.quattage.mechano.api.grid.topology.landmark.Terminal;

public abstract class VoltageSource extends StampingComponent implements StampsDynamically, NeedsPostProcessing {

    protected final VoltageDecay decayFunction;
    private final @Nullable CurrentChangeCallback cc;
    private double current;

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

    public double getCurrent() {
        return current;
    }

    @Override
    public int getAllocations() {
        return 1;
    }

    @Override
    public void stamp(ServerGrid grid) {
        int a = indexOf(grid, terminals[0]);
        int b = indexOf(grid, terminals[1]);
        int i = grid.indexer().get(this);
        grid.stampA(a, i, 1);
        grid.stampA(i, a, 1);
        grid.stampA(b, i, -1);
        grid.stampA(i, b, -1);
    }

    @Override
    public void stampDynamic(ServerGrid grid) {
        grid.stampB(grid.indexer().get(this), getVolts());
    }

    @Override
    public void postProcess(ServerGrid grid) {
        double newCurrent = grid.getSolution().get(grid.indexer().get(this), 0);
        if(cc != null) cc.onCurrentUpdated(grid, this.current, newCurrent);
        this.current = newCurrent;
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
}
