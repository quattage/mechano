package com.quattage.mechano.api.circuit.component;

import com.quattage.mechano.api.circuit.CircuitComponent;
import com.quattage.mechano.api.circuit.Terminal;

public class Capacitor implements CircuitComponent {
    
    private final Terminal[] terminals;
    private final float capacitance;
    private double previousVoltage;

    public Capacitor(Terminal a, Terminal b, float capacitance) {
        this.terminals = Terminal.pair(a, b);
        this.capacitance = capacitance;
    }

    @Override
    public Terminal getTerminal(int index) {
        return terminals[index];
    }

    @Override
    public double getVoltage() {
        return (terminals[0].getVoltage() - terminals[1].getVoltage());
    }

    public double getPreviousVoltage() {
        return previousVoltage;
    }

    @Override
    public double getCurrent() {
        double dV = getVoltage() - previousVoltage;
        return (double)capacitance * (dV / DELTA);
    }
    
    
}
