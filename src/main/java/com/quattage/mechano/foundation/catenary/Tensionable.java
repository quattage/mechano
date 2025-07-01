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
        if(Tension.STUPID_LOOSE.equals(trgt))
            return false;
        return setTension(trgt);
    }

    public default boolean setTension() {
        return resetTension();
    }

    public default boolean resetTension() {
        if(Tension.AVERAGE.equals(this.getTension())) return false;
        return setTension(Tension.AVERAGE);
    }

    public default boolean setTension(int tension) {
        return setTension(Tension.values()[Math.max(0, Math.min(Tension.values().length - 1, tension))]);
    }

    public abstract Tension getTension();
    public abstract boolean setTension(Tension tension);

    public float getLength();
    public float getMaxLength();

    public default void applyDistanceTension(float distance, float maxDistance) {
        float frac = distance / maxDistance;
        if(frac > 0.9) setTension(Tension.TAUT);
        else if(frac > 0.8) setTension(Tension.TIGHT);
        else if(frac > 0.6) setTension(Tension.AVERAGE);
        else if(frac > 0.4) setTension(Tension.LOOSE);
        else if(frac > 0.2) setTension(Tension.VERY_LOOSE);
    }
}
