package com.quattage.mechano.api.circuit.topology;

import java.util.Collection;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.griddable.Griddable;
import com.quattage.mechano.foundation.numeric.Bifrucated64;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

public abstract class CircuitComponent implements StringRepresentable {

    protected final String componentID;

    public CircuitComponent(String componentID) {
        this.componentID = componentID;
    }

    /**
     * Gets all pins associated with this component.
     * (ex. a Diode would return a list of two members: [anode, cathode])
     * If this component is compositional (like a Circuit object), calls to
     * this method will need to construct a collection of terminals, which
     * may be rather expensive.
     * @return all pins attached to this component
     */
    public abstract Collection<Terminal> getTerminals();

    /**
     * Steps forward in time by one tick
     * @param host The source of the ticking. May be a BlockEntity, Entity, or the level itself.
     */
    public abstract void tick(Griddable<?> host);

    /**
     * Gets the charge that this component is currently storing, if applicable.
     * Otherwise, this method returns <code>BifrucatedLong.ZERO</code>
     * @return amp-hours currently contained within this energy store
     */
    public Bifrucated64 getStoredCharge() { return Bifrucated64.ZERO.mutableCopy(); }

    /**
     * Gets the voltage potential that exists at this device
     * @return Volts
     */
    public double getVoltage() { return 0; }
    /**
     * Sets the voltage potential at this device
     * @param voltage
     */
    public void setVoltage(double voltage) {}

    /**
     * Gets the resistance that exists between
     * the two primary loading pins of this component,
     * if applicable. Otherwise, this method returns 
     * <code>0</code>
     * @return Ohms
     */
    public float getResistance() { return 0; }

    /**
     * Gets the current
     * @return Amps
     */
    public double getCurrent() { return getVoltage() / (double)getResistance(); }

    /**
     * Fills this component with energy, if this 
     * component is capable of storing any.
     */
    public void saturate() {}

    /**
     * Returns this component to its arbitrary
     * default state.
     */
    public void resetState() {  }

    @Override
    public final String getSerializedName() {
        return componentID.toLowerCase(Locale.ROOT);
    }

    @Override
    public final String toString() {
        return componentID + "[" + describeState() + "]";
    }

    public String describeState() {
        return "No state descriptor";
    }

    public final ResourceLocation asResource() {
        return Mechano.asResource("grid.component." + getSerializedName());
    }

    /**
     * Used to enforce a parent/child relationship for components and the 
     * circuits they belong to. Some components, especially implementing
     * ones with multiple terminals (diodes, resistors, etc.) may belong
     * to multiple components simultaneously. Implementations should return
     * the parent component at the terminal that makes the most sense. For
     * arrays, just return the parent at <code>terminals[0]</code>
     * @return The CircuitComponent instance that currently owns this one.
     */
    public abstract @Nullable CircuitComponent getParentComponent();


    protected boolean assertCanBeOwnedBy(CircuitComponent other) {
        if(this == other) throw new IllegalArgumentException("Cannot add component " + this + " - This component cannot be parented to itself!");
        if(this instanceof Circuit) throw new IllegalArgumentException("Cannot add component '" + this + "' to '" + other + " - These components are incompatible!");
        return true;
    }
}
