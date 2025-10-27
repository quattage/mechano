package com.quattage.mechano.api.circuit.component;

import java.util.Arrays;
import java.util.Collection;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.circuit.topology.Circuit;
import com.quattage.mechano.api.circuit.topology.CircuitComponent;
import com.quattage.mechano.api.circuit.topology.Terminal;
import com.quattage.mechano.api.griddable.Griddable;

public class Capacitor extends CircuitComponent {
    
    private final Terminal[] terminals;
    private final float capacitance;
    private double previousVoltage;

    public Capacitor(float capacitance) {
        super("Capacitor");
        this.terminals = Terminal.polarPair(this);
        this.capacitance = capacitance;
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
        double dV = getVoltage() - previousVoltage;
        return (double)capacitance * (dV / Circuit.DELTA);
    }

	@Override
	public void tick(Griddable<?> host) {
		
	}

    @Override
    public @Nullable CircuitComponent getParentComponent() {
        return terminals[0].getParentComponent();
    }
}
