package com.quattage.mechano.api.circuit.component;

import com.quattage.mechano.api.circuit.topology.Terminal;
import com.quattage.mechano.api.griddable.Griddable;

import java.util.Arrays;
import java.util.Collection;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.circuit.topology.CircuitComponent;

public class Diode extends CircuitComponent {

    private static final float VT = 0.026f;
    private final Terminal[] terminals;
    private final float ampRating;

    public Diode(float ampRating) {
        super("Diode");
        this.terminals = Terminal.functionalPair(this);
        this.ampRating = ampRating;
    }

    @Override
    public Collection<Terminal> getTerminals() {
        return Arrays.asList(terminals);
    }

    @Override
    public double getVoltage() {
        return terminals[0].getJoint().getVoltage() - terminals[1].getJoint().getVoltage();
    }

    @Override
    public double getCurrent() {
        // unimplemented
        return 0;
    }

    @Override
    public void tick(Griddable<?> host) {
        
    }

    @Override
    public @Nullable CircuitComponent getParentComponent() {
        return terminals[0].getParentComponent();
    }
}