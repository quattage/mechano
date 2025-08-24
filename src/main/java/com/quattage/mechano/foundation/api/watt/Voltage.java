package com.quattage.mechano.foundation.api.watt;

import net.minecraft.nbt.CompoundTag;

/**
 * Voltage values are stored such that a legible integer value is packed into a short.
 * The get() method returns values between <code>0</code> and <code>262140</code>.
 * Any value is implicitly quantized to its nearest whole 4.
 */
public class Voltage implements Comparable<Voltage>, VoltageReturnable {


    private static final int MERIDIAN = 4; // voltage values are always multiples of 4
    private static final int DIV = MERIDIAN / 2;

    public static final Voltage ZERO = new Voltage(0);
    public static final Voltage MAX = new Voltage((short)32767);
    public static final int MAX_INT_VALUE = 65535 * MERIDIAN;

    private final short packed;

    public static short pack(int voltageIn) {
        if(voltageIn > Voltage.MAX_INT_VALUE) return Short.MAX_VALUE;
        else if(voltageIn < 0) return Short.MIN_VALUE;
        return (short)(((voltageIn + DIV) / MERIDIAN) - 32768);
    }

    public static int getNearest(int in) {
        return Math.max(0, Math.min(MAX_INT_VALUE, (in + (in < 0 ? -DIV : DIV)) / MERIDIAN * MERIDIAN));
    }

    public 
    Voltage(int volts) {
        this.packed = Voltage.pack(volts);
    }

    public Voltage(float volts) {
        this(Math.round(volts));
    }

    public Voltage(double volts) {
        this((int)Math.round(volts));
    }

    public Voltage(short packed) {
        this.packed = packed;
    }

    public Voltage(CompoundTag tag) {
        packed = tag.getShort("volts");
    }

    public CompoundTag toTag(CompoundTag tag) {
        tag.putShort("volts", packed);
        return tag;
    }

    public static Voltage add(Voltage a, Voltage b) {
        return new Voltage(pack((MERIDIAN * a.packed) + (MERIDIAN * b.packed) + MAX_INT_VALUE));
    }

    public static Voltage subtract(Voltage a, Voltage b) {
        return new Voltage(pack((MERIDIAN * a.packed) - (MERIDIAN * b.packed)));
    }

    @Override
    public int compareTo(Voltage that) {
        if(this.packed < that.packed) return -1;
        if(this.packed > that.packed) return 1;
        return 0;
    }

    @Override
    public boolean equals(Object other) {
        if(!(other instanceof Voltage that)) return false;
        return this.packed == that.packed;
    }

    @Override
    public int hashCode() {
        return (int)packed;
    }

    public int getValue() {
        return (((int)(packed)) + 32768) * MERIDIAN; 
    }

    public short getRaw() {
        return packed;
    }

    @Override
    public String toString() {
        return getValue() + "";
    }

    @Override
    public Voltage get(float percent) {
        return this;
    }

    @Override
    public Voltage get() {
        return this;
    }

    @Override
    public Voltage[] getPrecomputed(int res) {
        return new Voltage[]{this};
    }
}