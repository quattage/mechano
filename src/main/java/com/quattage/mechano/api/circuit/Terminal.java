package com.quattage.mechano.api.circuit;

public interface Terminal {
    
    public static Terminal[] pair() {
        return new Terminal[] { new BasicTerminal(), new BasicTerminal() };
    }

    public static Terminal[] pair(Terminal negative, Terminal positive) {
        return new Terminal[] { negative, positive };
    }
    
    public abstract double getVoltage();
    public abstract Terminal setVoltage(double voltage);

    public static class BasicTerminal implements Terminal {

        private double voltage = 0;

        @Override
        public double getVoltage() {
            return voltage;
        }

        @Override
        public Terminal setVoltage(double voltage) {
            this.voltage = voltage;
            return this;
        }

    }
}
