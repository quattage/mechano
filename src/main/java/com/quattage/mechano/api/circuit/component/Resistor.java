package com.quattage.mechano.api.circuit.component;

import com.quattage.mechano.api.circuit.CircuitComponent;
import com.quattage.mechano.api.circuit.Terminal;

public class Resistor implements CircuitComponent {

    private final Terminal[] terminals;
    private final float ohms;

    public Resistor(Terminal negative, Terminal positive, float ohms) {
        this.terminals = Terminal.pair(negative, positive);
        this.ohms = ohms;
    }

    @Override
    public Terminal getTerminal(int index) {
        return terminals[index];
    }

    @Override
    public double getVoltage() {
        return terminals[0].getVoltage() - terminals[1].getVoltage();
    }

    @Override
    public double getCurrent() {
        return (double)ohms / (getVoltage());
    }
}
