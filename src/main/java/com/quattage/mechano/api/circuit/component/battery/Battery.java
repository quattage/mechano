package com.quattage.mechano.api.circuit.component.battery;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.circuit.CircuitComponent;
import com.quattage.mechano.api.circuit.Terminal;
import com.quattage.mechano.api.circuit.Watt;
import com.quattage.mechano.foundation.math.Bifrucated64;

/**
 * The basic structue of a charge-based battery model storing
 * amp-hours at voltages that fluctuate with SoC and environmental factors.
 */
public abstract class Battery implements CircuitComponent {

    private final Terminal[] terminals;

    protected final Bifrucated64 internalCharge;

    public Battery(Terminal negative, Terminal positive) {
        this.terminals = Terminal.pair(negative, positive);
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

    @Override
    public Terminal getTerminal(int index) {
        return terminals[index];
    }

    /**
     * A battery's SoC describes its fullness as <code>storedAh / ratedAh</code>
     * @return A double, usually falling in the range of <code>[0, 1] (inclusive)</code>, but may be greater than 1 in some circumstances.
     */
    public double getStateOfCharge() {
        return (double)Math.max((internalCharge.doubleValue() / getDatasheet().getRatedCapacity()), 0d);
    }

    @Override
    public double getVoltage() {
        return getDatasheetSafe().getExpectedVoltage(getStateOfCharge());
    }

    @Override
    public double getCurrent() {
        return getVoltage() / getDatasheetSafe().getInternalResistance();
    }
    
    @Override
    public Bifrucated64 getStoredCharge() {
        return internalCharge;
    }

    

    @Override
    public double getInstantaneousPower() {
        return getVoltage() * internalCharge.doubleValue();
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

    public Battery eraseEnergy() {
        this.internalCharge.zeroOut();
        return this;
    }

    public Battery fillEnergy() {
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
