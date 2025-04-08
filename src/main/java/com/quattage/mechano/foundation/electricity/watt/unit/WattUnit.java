package com.quattage.mechano.foundation.electricity.watt.unit;

import static com.quattage.mechano.Mechano.lang;

import com.quattage.mechano.MechanoSettings;

import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public class WattUnit implements Comparable<WattUnit> {

    public static final WattUnit EMPTY = new WattUnit(0, 0);
    public static final WattUnit MAX = new WattUnit(Short.MAX_VALUE, Float.MAX_VALUE);

    public static final float MIN_WATTS = 1f / (float)MechanoSettings.FE2W_RATE;

    private final Voltage volts;
    private float amps;

    public WattUnit(int volts, float amps) {
        this.volts = new Voltage(volts);
        this.amps = amps;
        setZeroIfNoPotential();
    }

    public WattUnit(Voltage volts, float amps) {
        this.volts = volts;
        this.amps = amps;
        setZeroIfNoPotential();
    }

    private WattUnit(short volts, float amps) {
        this.volts = new Voltage(volts);
        this.amps = amps;
        setZeroIfNoPotential();
    }

    private WattUnit(WattUnit other) {
        this.volts = new Voltage(other.volts.getRaw());
        this.amps = other.amps;
        setZeroIfNoPotential();
    }
    

    /**
     * Make a new WattUnit from total power in watts.
     * @param volts Voltage of this WattUnit
     * @param watts Total power. Used to derive the current value in the resulting WattUnit
     * @return a new WattUnit object
     */
    public static WattUnit of(Voltage volts, float watts) {
        if(WattUnit.hasNoPotential(watts)) return WattUnit.EMPTY;
        return new WattUnit(volts, watts / (float)volts.get());
    }

    /**
     * Make a new WattUnit from total power in watts.
     * @param volts Voltage of this WattUnit
     * @param watts Total power. Used to derive the current value in the resulting WattUnit
     * @return a new WattUnit object
     */
    public static WattUnit of(int volts, float watts) {
        WattUnit out = new WattUnit(volts, watts / (float)volts);
        return out;
    }

    /**
     * Make a new WattUnit from a CompoundTag <p>
     * Expects a short ("volt") and a float ("curr"
     * @param in CompoundTag to pull values from
     * @return a new WattUnit object
     */
    public static WattUnit ofTag(CompoundTag in) {
        return new WattUnit(in.getShort("volt"), in.getFloat("curr"));
    }

    /**
     * Make a new WattUnit from aa FriendlyByteBuf
     * @param buf Buf to pull values from
     * @return a new WattUnit object
     */
    public static WattUnit ofBytes(FriendlyByteBuf buf) {
        return new WattUnit(buf.readShort(), buf.readFloat());
    }

    public static boolean hasNoPotential(float watts) {
        return watts < MIN_WATTS;
    }

    public WattUnit copy() {
        return new WattUnit(this);
    }

    /**
     * Voltage can range from 0 to 262,140 rounded to the nearest 4 whole numbers
     * @return The voltage stored in this Watt unit
     */
    public Voltage getVoltage() {
        return volts;
    }

    /**
     * @return The current (amps) stored in this Watt unit
     */
    public float getCurrent() {
        return amps;
    }

    /**
     * Adjust the voltage stored within this Watt unit such that
     * <code>watts = voltage * current</code> <p>
     * Rounds the input voltage to the nearest multiple of 4 if necessary for precision accomodations.
     * Also adjusts the current to make the above equation true without altering the total power output (watts)
     * @param newVoltage the new voltage to set this Watt unit to
     * @return This WattUnit after modification
     */
    public WattUnit adjustVoltage(int newVoltage) {

        if(this.equals(WattUnit.MAX)) 
            return setVoltageLossy(newVoltage);

        if(getCurrent() == 0)  return this;
        if(newVoltage < 1) {
            setZero();
            return this;
        }

        float oldWatts = getWatts();
        volts.setTo(newVoltage < 4 ? 4 : newVoltage);
        this.amps = oldWatts / (float)volts.get();

        return this;
    }

    /**
     * Set the voltage stored within this WattUnit and alters the total power output.
     * Does not adjust current to ensure the total power output remains the same.
     * For that, use {@link WattUnit#adjustVoltage(int) <code>adjustVoltage</code>}
     * @param newVoltage
     * @return This WattUnit after modification
     */
    public WattUnit setVoltageLossy(int newVoltage) {
        volts.setTo(newVoltage);
        setZeroIfNoPotential();
        return this;
    }

    /**
     * Adjust the current (amps) stored within this Watt unit such that
     * <code>watts = voltage * current</code> <p>
     * Adjusts the voltage to make the above equation true without altering the total power output (watts)
     * Permissable voltage values only come in multiples of 4, so the resulting current may not be exactly
     * what was passed.
     * @param newAmps the new current to set this Watt unit to
     * @return This WattUnit after modification
     */
    public WattUnit adjustCurrent(float newAmps) {

        if(getCurrent() == 0) return this;
        if(newAmps < 0.00001) {
            setZero();
            return this;
        }

        int vRounded = Math.max(4, (int)(4f * Math.round((getWatts() / newAmps) / 4f)));
        this.amps = getWatts() / (float)vRounded;
        volts.setTo(vRounded);

        return this;
    }

    /**
     * Set the current stored within this WattUnit and alters the total power output.
     * Does not adjust voltage to ensure the total power output remains the same.
     * For that, use {@link WattUnit#adjustCurrent(float) <code>adjustCurrent</code>}
     * @param newVoltage
     * @return
     */
    public WattUnit setCurrentLossy(float amps) {
        this.amps = amps;
        setZeroIfNoPotential();
        return this;
    }

    /**
     * If the effective watt value of this WattUnit is zero, 
     * 
     * set both the voltage and current to zero.
     * <p>
     * This makes it impossible for a WattUnit to carry a non-zero voltage potential at zero (or effectively zero) amps, 
     * which would make no sense - the math that happens later on shouldn't have to deal with this edge case.
     * @return This WattUnit after modification - Equiavalent to {@link WattUnit#EMPTY <code>WattUnit.EMPTY</code>}
     */
    public WattUnit setZeroIfNoPotential() {
        if(hasNoPotential()) setZero();
        return this;
    }

    /**
     * @return <code>TRUE</code> if this WattUnit's total power output is zero or effectively zero.
     * (that is, if {@link WattUnit#getCurrent() <code>getCurrent()</code>} returns less than 1 equivalent unit of FE, whatever the conversion rate may be return <code>TRUE</code>)
     */
    public boolean hasNoPotential() {
        return getWatts() < MIN_WATTS;
    }

    /**
     * Sets the voltage and current of this WattUnit to zero.
     * @return This WattUnit after modification
     */
    public WattUnit setZero() {
        this.amps = 0;
        volts.setTo(0);
        return this;
    }

    /**
     * Returns the total power stored in this WattUnit.
     * @return <code>watts = voltage * current</code> <p>
     */
    public float getWatts() {
        return (float)getVoltage().get() * getCurrent();
    }

    /**
     * Returns the total power stored in this WattUnit rounded
     * to the nearest hundredth.
     * @return <code>watts = voltage * current</code> <p>
     */
    public float getRoundedWatts() {
        return (float)Math.round(getWatts() * 100) / 100f;
    }

    /**
     * Writes this Watt object to a CompoundTag <p>
     * Voltage is stored with the key "volt" and current is stored with the key "curr"
     * @param in CompoundTag to modify
     * @return The modified CompoundTag
     */
    public CompoundTag writeTo(CompoundTag in) {
        in.putShort("volt", volts.getRaw());
        in.putDouble("curr", amps);
        return in;
    }

    public String toString() {
        return getRoundedWatts() + " Watts: [" + getVoltage() + " V,  " + Math.round(getCurrent() * 100) / 100f + " A]";
    }

    /**
     * Writes this Watt object to a FriendlyByteBuf for sending in packets
     * @param in FriendlyByteBuf to write
     */
    public void toBytes(FriendlyByteBuf in) {
        in.writeShort(volts.getRaw());
        in.writeFloat(amps);
    }

    /**
     * Directly compares voltage and current for equivalence. Does not compare total power. Two WattUnits that 
     * have the same effective Watt value could still return false as a result of this call.
     * @param other Other object to compare
     * @return TRUE if this WattUnit's voltage and current values are identical to the provided object.
     */
    public boolean equals(Object other) {
        if(!(other instanceof WattUnit w)) return false;
        return this.amps == w.amps && this.volts.equals(w.volts);
    }

    /**
     * Compares based on <strong>total power</strong> - Both WattUnits can have differing voltage and current values
     * and still return zero!
     * @param o Other WattUnit to comapre
     * @return 1 if this > other,   -1 if this < other,   0 if this == other.
     */
    @Override
    public int compareTo(WattUnit o) {
        if(this.getWatts() > o.getWatts()) return 1;
        if(this.getWatts() < o.getWatts()) return -1;
        return 0;
    }

    /**
     * Adds this WattUnit with <code>other</code>. Adding WattUnits
     * is as simple as combining current values and taking the larger 
     * voltage value between the two
     * @param other WattUnit to add
     * @return This WattUnit, modified as a result of this call.
     */
    public WattUnit add(WattUnit other) {
        this.amps += other.amps;
        volts.setTo(volts.getGreater(other.getVoltage()));
        return this;
    }

    /**
     * Adds this WattUnit with <code>other</code>. Adding WattUnits
     * is as simple as combining current values and taking the larger 
     * voltage value between the two
     * @param other WattUnit to add
     * @return This WattUnit, modified as a result of this call.
     */
    public WattUnit clampToMax(float watts) {
        this.amps = Math.min(getCurrent(), watts / (float)volts.get());
        return this;
    }

    public LangBuilder format(ChatFormatting numberColor, ChatFormatting unitColor) {

        double watts = (double)getWatts() * 72000d;

        if(watts >= 10000000) {
            double converted = watts * 0.000000001d;
            if(converted > 0.5)
                return CreateLang.number(watts * 0.000000001d).style(numberColor).add(lang().translate("generic.unit.gigawatthours").style(unitColor));
        }
        if(watts >= 1000)
            return CreateLang.number(watts * 0.001d).style(numberColor).add(lang().translate("generic.unit.kilowatthours").style(unitColor));
        return CreateLang.number(watts).style(numberColor).add(lang().translate("generic.unit.watthours").style(unitColor));
    }

    /**
     * Compares this WattUnit with the provided WattUnit. Returns a WattUnit that has the lowest stats of both
     * @param o Other WattUnit to compare
     * @return A new WattUnit, containing the lowest current and voltage between this WattUnit and the provided WattUnit.
     */
    public WattUnit getLowerStats(WattUnit o) {
        float cOut = this.getCurrent();
        short vOut = this.getVoltage().getRaw();
        if(cOut > o.getCurrent()) cOut = o.getCurrent();
        if(vOut > o.getVoltage().getRaw()) vOut = o.getVoltage().getRaw();
        return new WattUnit(vOut, cOut);
    }
}