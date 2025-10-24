package com.quattage.mechano.foundation.math;

import java.math.BigDecimal;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.quattage.mechano.Mechano;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * This class wraps a single long which stores a 36 bit whole and 28 bit decimal
 * value. Useful for fixed-point arithmetic with unsigned numbers up to 68 billion.
 * Numbers stored here can be added & subtracted faster than heavily saturated doubles,
 * and fixed-precision allows exact comparisons with a static epsilon.
 * <p>
 * This class was written for the Mechano project to allow API users to implement 
 * batteries that can store exceedingly large amounts of charge without worrying about
 * unpredictable rounding.
 */
public class Bifrucated64 extends Number implements Comparable<Bifrucated64> {

    private static final int SPLITSHIFT = 28;
    private static final long MAX_HIGH = (1L << 64L - (long)SPLITSHIFT) - 1L;
    private static final int MAX_LOW = (1 << SPLITSHIFT) - 1;
    public static final double EPSILON = (1d / (double)MAX_LOW) * 2d;

    public static final Bifrucated64 MAX = new Bifrucated64 (Long.MAX_VALUE, true);
    public static final Bifrucated64 ZERO = new Bifrucated64(0, true);

    public static final StreamCodec<? super RegistryFriendlyByteBuf, Bifrucated64> STREAM_CODEC = new StreamCodec<>() {
        @Override public void encode(RegistryFriendlyByteBuf buffer, Bifrucated64 value) {
            buffer.writeLong(value.packed);
        }
        @Override public Bifrucated64 decode(RegistryFriendlyByteBuf buffer) {
            return new Bifrucated64(buffer.readLong(), false);
        }
    };

    public static final Codec<Bifrucated64> CODEC = new PrimitiveCodec<Bifrucated64>() {
        @Override
        public <T> DataResult<Bifrucated64> read(DynamicOps<T> ops, T input) {
            return Bifrucated64.makeResult(ops.getNumberValue(input).map(Number::longValue));
        }
        @Override
        public <T> T write(DynamicOps<T> ops, Bifrucated64 value) {
            return ops.createLong(value.packed);
        }
    };

    private static DataResult<Bifrucated64> makeResult(DataResult<Long> value) {
        if(value == null || value.isError()) return DataResult.error(value != null && value.error().isPresent() ? value.error().get().messageSupplier() : () -> "ruh roh");
        return DataResult.success(new Bifrucated64(value.getOrThrow()));
    }

    private long packed;
    private boolean immutable;

    public static long pack(long high, int low) {
        return (high << SPLITSHIFT) | low;
    }

    public static long unpackHigh(long packed) {
        return (packed >>> SPLITSHIFT);
    }

    public static int unpackLow(long packed) {
        return (int)(packed & 0x0FFFFFFFL);
    }

    private Bifrucated64(long value) {
        this.packed = value;
        this.immutable = false;    
    }

    public Bifrucated64(double value) { setValue(value); }
    public Bifrucated64 setValue(double value) {
        if(immutabilityCheck()) return mutableCopy().setValue(value);
        long whole = Math.min(Math.abs((long)value), MAX_HIGH);
        double frac = value - Math.abs((double)whole);
        this.packed = Bifrucated64.pack(whole, (int)Math.round((frac * MAX_LOW)));
        return this;
    }

    public Bifrucated64(float value) { setValue(value); }
    public Bifrucated64 setValue(float value) {
        if(immutabilityCheck()) return mutableCopy().setValue(value);
        long whole = Math.min(Math.abs((long)value), MAX_HIGH);
        double frac = whole - Math.abs((double)whole);
        this.packed = Bifrucated64.pack(whole, (int)Math.round((frac * MAX_LOW)));
        return this;
    }

    public Bifrucated64(int value) { setValue(value); }
    public Bifrucated64 setValue(int value) {
        if(immutabilityCheck()) return mutableCopy().setValue(value);
        this.packed = Math.abs(((long)value << SPLITSHIFT));
        return this;
    }

    public Bifrucated64(BigDecimal bd) { this(bd.toPlainString()); }
    public Bifrucated64(String value) { setValue(value); }
    public Bifrucated64 setValue(String value) {
        int decimalPlace = value.indexOf('.');
        if(decimalPlace < 0) {
            try { return setValue(Long.parseLong(value)); } catch (NumberFormatException e) {
                Mechano.LOGGER.error("Couldn't parse BifrucatedLong from input string '" + value + "'", e);
            }
        }
        String wholeComponent = value.substring(0, decimalPlace);
        String decimalComponent = "0" + value.substring(decimalPlace);
        try {
            long whole = Math.min(Math.abs(Long.parseLong(wholeComponent)), MAX_HIGH);
            double frac = Double.parseDouble(decimalComponent);
            this.packed = Bifrucated64.pack(whole, (int)Math.round((frac * MAX_LOW)));
        } catch (NumberFormatException e) {
            Mechano.LOGGER.error("Couldn't parse BifrucatedLong from input string '" + value + "'", e);
        }
        return this;
    }

    private Bifrucated64(long packed, boolean immutable) {
        this.packed = packed;
        this.immutable = immutable;
    }

    public Bifrucated64 mutableCopy() {
        return new Bifrucated64(this.packed, false);
    }

    public Bifrucated64 immutableCopy() {
        return new Bifrucated64(this.packed, true);
    }

    public Bifrucated64 add(double value) { return add(new Bifrucated64(value)); }
    public Bifrucated64 add(Bifrucated64 other) {
        if(immutabilityCheck()) return mutableCopy().add(other);
        long low = unpackLow(this.packed) + unpackLow(other.packed);
        int carry = 0;
        if(low > MAX_LOW) {
            low = low & MAX_LOW; 
            carry++;
        }
        long highSum = Math.min(unpackHigh(this.packed) + unpackHigh(other.packed) + carry, MAX_HIGH);
        this.packed = pack(highSum, (int)low);
        return this;
    }

    public Bifrucated64 subtract(double value) { return subtract(new Bifrucated64(value)); }
    public Bifrucated64 subtract(Bifrucated64 other) {
        if(immutabilityCheck()) return mutableCopy().subtract(other);
        long highA = unpackHigh(this.packed);
        long highB = unpackHigh(other.packed);
        if(highA < highB) {
            this.packed = 0;
            return this;
        }
        long low = unpackLow(this.packed) - unpackLow(other.packed);
        int borrow = 0;
        if(low < 0) {
            low += 1L << SPLITSHIFT;
            borrow++;
        }
        this.packed = pack(Math.clamp(highA - highB - borrow, 0, MAX_HIGH), (int)low);
        return this;
    }

    @Override
    public int compareTo(Bifrucated64 o) {
        return this.packed > o.packed ? 1 : (this.packed < o.packed ? -1 : 0);
    }

    @Override
    public int intValue() {
        return (int)Math.min(Bifrucated64.unpackHigh(packed), Integer.MAX_VALUE);
    }

    @Override
    public long longValue() {
        return Bifrucated64.unpackHigh(packed);
    }

    @Override
    public float floatValue() {
        return (float)doubleValue();
    }
    
    @Override
    public double doubleValue() {
        return (double)longValue() + ((double)Bifrucated64.unpackLow(packed) / (double)MAX_LOW);
    }

    public BigDecimal bigValue() {
        return new BigDecimal(toString());
    }

    public boolean isZero() { return packed <= 0; }
    public Bifrucated64 zeroOut() {
        if(immutabilityCheck()) return mutableCopy().zeroOut();
        packed = 0;
        return this;
    }

    public boolean isMax() { return packed >= MAX_HIGH; }
    public Bifrucated64 maxOut() {
        if(immutabilityCheck()) return mutableCopy().zeroOut();
        packed = MAX_HIGH;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(!(obj instanceof Bifrucated64 that)) return false;
        return this.packed == that.packed;
    }

    @Override
    public int hashCode() {
        return (int)(packed ^ (packed >>> 32));
    }

    @Override
    public String toString() {
        String dec = "" + ((double)Bifrucated64.unpackLow(packed) / (double)MAX_LOW);
        return longValue() + "." + dec.substring(2, dec.length() - 1);
    }

    private boolean immutabilityCheck() {
        if(!immutable) return false;
        Mechano.LOGGER.warn("Attempted to perform a modifying operation on a BifrucatedLong - A copy will be returned instead.");
        return true;
    }
}
