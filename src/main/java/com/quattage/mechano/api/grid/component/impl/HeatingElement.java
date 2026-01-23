package com.quattage.mechano.api.grid.component.impl;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.component.StampingComponent.NeedsPostProcessing;
import com.quattage.mechano.api.grid.solver.NodalSolver;
import com.quattage.mechano.api.grid.topology.landmark.Terminal;

public class HeatingElement extends Resistor implements NeedsPostProcessing {

    private final @Nullable CurrentChangeCallback cc;
    private double current;

    public HeatingElement(float ohms) { this(ohms, null); }
    public HeatingElement(float ohms, CurrentChangeCallback cc) {
        super(ohms);
        this.cc = cc;
    }

    @Override
    public void postProcess(ServerGrid grid) {
        double r = (double)getResistance();
        double i = (pinA().getAttachedNode().getVoltage(grid) - pinB().getAttachedNode().getVoltage(grid)) / r;
        double newCurrent = i * i * r;
        if(cc != null && Math.abs(this.current - newCurrent) < NodalSolver.EPSILON)
            cc.onCurrentUpdated(grid, newCurrent, newCurrent);
        this.current = newCurrent;
    }

    @Override
    public @Nullable Terminal pinA() {
        return terminals[0];
    }

    @Override
    public @Nullable Terminal pinB() {
        return terminals[1];
    }
}
