package com.quattage.mechano.api.circuit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.math.Bifrucated64;

/**
 * The basic structue of a charge-based battery model storing
 * amp-hours at voltages that fluctuate with SoC and environmental factors.
 */
public abstract class EnergyStore {

    protected static final double DELTA = 0.05d / 3600d;

    protected final Bifrucated64 internalCharge;

    public EnergyStore() {
        this.internalCharge = Bifrucated64.ZERO.mutableCopy();
    }

    /**
     * Construct a new {@link BatteryDatasheet datasheet} defining
     * various characteristics of this battery. This datasheet
     * determines several characteristics of this battery such as
     * overall charge capacity, as well as various electrolytic phenomena 
     * relating to the battery technology used. <p> 
     * Datasheets' contents are immutable and the cost of constructing
     * them may vary. It's highly reccomended to store a static final
     *  reference to your datasheet somewhere in your implementing class 
     * and return it here, rather than constructing a new one every time.
     * @return
     */
    public abstract @Nullable BatteryDatasheet getDatasheet();
    protected @NotNull BatteryDatasheet getDatasheetSafe() {
        BatteryDatasheet newData = getDatasheet();
        if(newData != null) return newData;
        return BatteryDatasheet.DEFAULT;
    }

    public abstract Watt charge(Watt energy);
    public abstract Watt discharge(double current);

    /**
     * A battery's SoC describes its fullness as <code>storedAh / ratedAh</code>
     * @return A double, usually falling in the range of <code>[0, 1] (inclusive)</code>, but may be greater than 1 in some circumstances.
     */
    public double getStateOfCharge() {
        return (double)Math.max((internalCharge.doubleValue() / getDatasheet().getRatedCapacity()), 0d);
    }

    /**
     * In charge-based energy systems, the stored charge is the amount of power
     * currently residing in the energy store. Power != energy, so this number
     * is an amp-hours, not joules. In order to get the total joules 
     * currently in this battery, you'd need to integrate the voltage curve.
     * @return amp-hours currently contained within this energy store
     */
    public Bifrucated64 getStoredCharge() {
        return internalCharge;
    }

    /**
     * @return The voltage potential that this battery posesses
     * at its current fullness.
     */
    public double getDischargeVoltage() {
        return getDatasheetSafe().getExpectedVoltage(getStateOfCharge());
    }

    /**
     * @return The voltage potential that this battery posesses when 
     * it is completely full
     */
    public double getNominalVoltage() {
        return getDatasheetSafe().getExpectedVoltage(1);
    }

    /**
     * @return The voltage potential that this battery posesses
     * when it is considered to be empty
     */
    public double getMinimumVoltage() {
        return getDatasheetSafe().getExpectedVoltage(0);
    }

    public EnergyStore eraseEnergy() {
        this.internalCharge.zeroOut();
        return this;
    }

    public EnergyStore fillEnergy() {
        this.internalCharge.setValue(getDatasheetSafe().getRatedCapacity());
        return this;
    }

    public boolean isEmpty() {
        if(internalCharge.isZero()) return true;
        if(internalCharge.longValue() < 1 || getStateOfCharge() < 0.01f) {
            eraseEnergy();
            return true;
        }
        return false;
    }

    public boolean isFull() {
        if(internalCharge.isMax()) return true;
        return getDatasheetSafe().isWithinCapacity(internalCharge);
    }
}
