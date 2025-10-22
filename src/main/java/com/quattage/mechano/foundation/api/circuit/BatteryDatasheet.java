package com.quattage.mechano.foundation.api.circuit;

import java.util.Objects;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import com.quattage.mechano.foundation.api.circuit.VoltageDecay.DataPoint;
import com.quattage.mechano.foundation.api.math.BifrucatedLong;

public class BatteryDatasheet implements Cloneable {

    private final BifrucatedLong ratedCapacity;      // amp hours
    private final float internalResistance;    // ohms
    private final float dischargeEfficiency;   // [0, 1] scalar
    private final float chargeRate;         // C rate
    private final float dischargeRate;      // C rate
    private final @NotNull VoltageDecay voltage;

    /**
     * A barebones datasheet that approximates the characteristics of a midrange lead-acid car battery.
     * This datasheet is used for testing and to be referenced later when creating new datasheets for other types of batteries.
     */
    public static final BatteryDatasheet DEFAULT = new BatteryDatasheet(
        new BifrucatedLong(40), 0.015f, 0.98f, 0.3f, 0.9f,
        new VoltageDecay.LinearLUT(new float[][] {
            { 0.0f, 11.59f },
            { 0.1f, 11.63f },
            { 0.2f, 11.76f },
            { 0.3f, 11.87f },
            { 0.4f, 12.2f },
            { 0.5f, 12.23f },
            { 0.6f, 12.26f },
            { 0.7f, 12.29f },
            { 0.8f, 12.41f },
            { 0.9f, 12.53f },
            { 1.0f, 12.64f },
            { 1.5f, 13.20f },
            { 2.0f, 18.45f },
        })
    );

    public BatteryDatasheet(BifrucatedLong ratedCapacity, float internalResistance, float dischargeEfficiency, float chargeRate, float dischargeRate, VoltageDecay voltage) {
        Objects.requireNonNull(ratedCapacity);
        Objects.requireNonNull(voltage);
        if(ratedCapacity.isZero()) 
            throw new IllegalArgumentException("Error instantiating battery datasheet - maximum capacity cannot be zero!");
        this.ratedCapacity = ratedCapacity;
        this.internalResistance = internalResistance;
        this.dischargeEfficiency = dischargeEfficiency;
        this.chargeRate = chargeRate;
        this.dischargeRate = dischargeRate;
        this.voltage = voltage;
    }

    /**
     * @return The maximum capacity of this battery in amp hours.
     * @see {@link #getRatedJoules}
     */
    public double getRatedAmpHours() { 
        return ratedCapacity.doubleValue();
    }

    public boolean isWithinCapacity(BifrucatedLong charge) {
        return ratedCapacity.compareTo(charge) >= 0;
    }

    /**
     * The total energy stored by this battery can be acquired
     * by integrating its {@link #getVoltageCurve voltage curve} 
     * given its {@link #getRatedAmpHours Ah rating}. For right now, 
     * This calculation is extraneous, as in, it does not provide a 
     * meaningful metric for charge-based systems other than for
     * testing and debugging. <p>
     * 
     * This method will become much more relevent later when 
     * implicit ForgeEnergy conversion is written into this API,
     * since ForgeEnergy is far closer to joules (energy),
     * rather than watt-hours (charge/power).
     * 
     * @return The maximum capacity of this battery in joules
     * @see {@link #getRatedAmpHours}
     */
    public double getRatedJoules() { 
        double joules = 0d;
        DataPoint[] table = voltage.getTable();
        for(int i = 0; i < table.length - 1; i++)
            joules += 0.5 * (table[i].volts() + table[i + 1].volts()) * (table[i + 1].soc() - table[i].soc());
        return joules * ratedCapacity.doubleValue() * 3600.0;
    }

    public VoltageDecay getVoltageCurve() { return voltage; }
    public float getInternalResistance() { return internalResistance; }
    public float getDischargeEfficiency() { return dischargeEfficiency; }
    public double getMaxChargeCurrent() { return chargeRate * ratedCapacity.doubleValue(); }
    public double getMaxDischargeCurrent() { return dischargeRate * ratedCapacity.doubleValue(); }

    /**
     * Get the open-circuit voltage (V_oc), or the expected voltage potential between the (implied) 
     * positive/negative terminals of a battery.
     * @param soc State of charge of the battery
     * @return V_oc open circuit voltage. This voltage may be greater than or less than this battery's rating / cutoff.
     */
    public double getExpectedVoltage(double soc) { return voltage.apply(soc); }

    /**
     * @return <code>true</code> if this datasheet indicates a statistically significant charge/discharge efficiency loss.
     * If <code>false</code>, the battery described by this datasheet is 100% efficient.
     */
    public boolean isLossy() { return dischargeEfficiency < 0.9999; }

    /**
     * Return a new datasheet with all the attributes of this one, but replace the voltage decay function
     * with the one provided.
     * @param voltage A new voltage decay function
     * @return A new datasheet instance
     */
    public BatteryDatasheet copyWith(VoltageDecay voltage) {
        return new BatteryDatasheet(ratedCapacity, internalResistance, dischargeEfficiency, chargeRate, dischargeRate, voltage);
    }

    /**
     * Return a new datasheet with all the attributes of this one, but replace the voltage decay function
     * with the one provided.
     * @param voltage A new voltage decay function
     * @return A new datasheet instance
     */
    public BatteryDatasheet copyWith(Function<DataPoint[], VoltageDecay> func) {
        return new BatteryDatasheet(ratedCapacity, internalResistance, dischargeEfficiency, chargeRate, dischargeRate, func.apply(this.voltage.getTable()));
    }
}
