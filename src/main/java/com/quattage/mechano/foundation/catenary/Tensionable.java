package com.quattage.mechano.foundation.catenary;

import com.quattage.mechano.foundation.catenary.CatenaryAttributes.Tension;

public interface Tensionable {

    public default boolean increaseTension() {
        int ord = getTension().ordinal() + 1;
        if(ord >= Tension.values().length) return false;
        return setTension(Tension.values()[ord]);
    }

    public default boolean decreaseTension() {
        int ord = getTension().ordinal() - 1;
        if(ord < 0) return false;
        Tension trgt = Tension.values()[ord];
        if(trgt.equals(Tension.STUPID_LOOSE))
            return false;
        return setTension(trgt);
    }

    public default boolean setTension() {
        return resetTension();
    }

    public default boolean resetTension() {
        if(this.getTension().equals(Tension.AVERAGE)) return false;
        return setTension(Tension.AVERAGE);
    }

    public default boolean setTension(int tension) {
        return setTension(Tension.values()[Math.max(0, Math.min(Tension.values().length - 1, tension))]);
    }

    public abstract Tension getTension();
    public abstract boolean setTension(Tension tension);
}
