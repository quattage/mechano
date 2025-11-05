package com.quattage.mechano.api.grid;

import java.util.Objects;

public class Watt {

    private double volts = 0d;
    private double amps = 0d;

    public static Watt zero() {
        return new Watt(0, 0);
    }

    public Watt(double volts, double current) {
        this.volts = Math.abs(volts);
        this.amps = Math.abs(current);
    }

    public double get() {
        return volts * amps;
    }

    public double getAsJoules() {
        return get() * (1 / 20f);
    }

    public double getVoltage() {
        return volts;
    }

    public double getAmps() {
        return amps;
    }

    public boolean isZero() {
        return getVoltage() <= 0 || getAmps() <= 0; 
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(!(obj instanceof Watt that)) return false;
        return Math.abs(this.volts - that.volts) < 0.001f && Math.abs(this.amps - that.amps) < 0.001f;
    }

    @Override
    public int hashCode() {
        return Objects.hash(volts, amps);
    }

    @Override
    public String toString() {
        return "Watt[" + volts + "v, " + amps + "A]";
    }
}
