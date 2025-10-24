package com.quattage.mechano.api.circuit;

import com.quattage.mechano.api.griddable.Griddable;
import com.quattage.mechano.foundation.math.Bifrucated64;

public interface CircuitComponent {
    
    /**
     * A CircuitComponent may have any number of {@link Terminal} objects
     * associated with it, which describes where and how it can interact with
     * other components. Traditionally, for components where polarity matters, 
     * the first terminal is for negative load, and the last terminal is for 
     * positive load.
     * @param index
     * @return The {@link Terminal} belonging to this circuit com
     */
    public Terminal getTerminal(int index);

    public static final double DELTA = 0.05d;
    public static final double DELTA_AH = DELTA / 3600d;


    public abstract double getVoltage();

    /**
     * Instantaneous power is the power that can be produced by 
     * or pulled from this component at this exact moment.
     * @return Watts
     * @see {@link #getStoredCharge}
     */
    public default double getInstantaneousPower() { return getCurrent() * getVoltage(); }

    /**
     * Instantaneous current is the current (in amps) that can
     * be produced or pulled from this component at this
     * exact moment.
     * @return Amps
     */
    public abstract double getCurrent();

    /**
     * In charge-based energy systems, the stored charge is the amount of power
     * currently residing in the energy store. Power != energy, so this number
     * is an amp-hours, not joules or watts. In order to get the total joules 
     * currently in this component, you'd need to integrate the voltage curve.
     * @return amp-hours currently contained within this energy store
     */
    public default Bifrucated64 getStoredCharge() { return Bifrucated64.ZERO.mutableCopy(); }

    /**
     * @return The resistance (in ohms) that exists between all nodes
     * in this component. A single component is assumed to have uniform
     * reisistance across all states and all terminals.
     */
    public default double getResistance() { return 0; }

    /**
     * Fills this component with energy, if this 
     * component is capable of storing any.
     */
    public default void saturate() {  }

    /**
     * Returns this component to its arbitrary
     * default state.
     */
    public default void resetState() {  }

    public default void tick(Griddable<?> host) {}
}
