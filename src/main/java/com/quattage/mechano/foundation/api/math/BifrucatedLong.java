package com.quattage.mechano.foundation.api.math;

import java.math.BigDecimal;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.RecordBuilder;
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
 * batteries that can store exceedingly large values without worrying about
 * unpredictable rounding.
 */
public class BifrucatedLong extends Number implements Comparable<BifrucatedLong> {

    private static final int SPLITSHIFT = 28;
    private static final long MAX_HIGH = (1L << 64L - (long)SPLITSHIFT) - 1L;
    private static final int MAX_LOW = (1 << SPLITSHIFT) - 1;
    public static final double EPSILON = (1d / (double)MAX_LOW) * 2d;

    public static final BifrucatedLong MAX = new BifrucatedLong (Long.MAX_VALUE, false);
    public static final BifrucatedLong ZERO = new BifrucatedLong(0, false);

    public static final StreamCodec<? super RegistryFriendlyByteBuf, BifrucatedLong> STREAM_CODEC = new StreamCodec<>() {
        @Override public void encode(RegistryFriendlyByteBuf buffer, BifrucatedLong value) {
            buffer.writeLong(value.packed);
        }
        @Override public BifrucatedLong decode(RegistryFriendlyByteBuf buffer) {
            return new BifrucatedLong(buffer.readLong(), false);
        }
    };

    public static final Codec<BifrucatedLong> CODEC = new Codec<>() {
        @Override public <T> DataResult<T> encode(BifrucatedLong input, DynamicOps<T> ops, T prefix) {
            RecordBuilder<T> builder = ops.mapBuilder();
            builder.add("v", input.packed, Codec.LONG);
            return builder.build(prefix);
        }
        @Override public <T> DataResult<Pair<BifrucatedLong, T>> decode(DynamicOps<T> ops, T input) {
            Dynamic<T> dyn = new Dynamic<>(ops, input);
            Long w = dyn.get("v").asLong(0);
            return DataResult.success(Pair.of(new BifrucatedLong(w, false), input));
        }
    };

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

    public BifrucatedLong(double value) { setValue(value); }
    public BifrucatedLong setValue(double value) {
        if(immutabilityCheck()) return mutableCopy().setValue(value);
        long whole = Math.min(Math.abs((long)value), MAX_HIGH);
        double frac = value - Math.abs((double)whole);
        this.packed = BifrucatedLong.pack(whole, (int)Math.round((frac * MAX_LOW)));
        return this;
    }

    public BifrucatedLong(float value) { setValue(value); }
    public BifrucatedLong setValue(float value) {
        if(immutabilityCheck()) return mutableCopy().setValue(value);
        long whole = Math.min(Math.abs((long)value), MAX_HIGH);
        double frac = whole - Math.abs((double)whole);
        this.packed = BifrucatedLong.pack(whole, (int)Math.round((frac * MAX_LOW)));
        return this;
    }

    public BifrucatedLong(int value) { setValue(value); }
    public BifrucatedLong setValue(int value) {
        if(immutabilityCheck()) return mutableCopy().setValue(value);
        this.packed = Math.abs(((long)value << SPLITSHIFT));
        return this;
    }

    public BifrucatedLong(long value) { setValue(value); }
    public BifrucatedLong setValue(long value) {
        if(immutabilityCheck()) return mutableCopy().setValue(value);
        this.packed = BifrucatedLong.pack((int)Math.min(Math.abs(value), MAX_HIGH), 0);
        return this;
    }

    public BifrucatedLong(BigDecimal bd) { this(bd.toPlainString()); }
    public BifrucatedLong(String value) { setValue(value); }
    public BifrucatedLong setValue(String value) {
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
            this.packed = BifrucatedLong.pack(whole, (int)Math.round((frac * MAX_LOW)));
        } catch (NumberFormatException e) {
            Mechano.LOGGER.error("Couldn't parse BifrucatedLong from input string '" + value + "'", e);
        }
        return this;
    }

    private BifrucatedLong(long packed, boolean immutable) {
        this.packed = packed;
        this.immutable = immutable;
    }

    public BifrucatedLong mutableCopy() {
        return new BifrucatedLong(this.packed, false);
    }

    public BifrucatedLong immutableCopy() {
        return new BifrucatedLong(this.packed, true);
    }

    public BifrucatedLong add(double value) { return add(new BifrucatedLong(value)); }
    public BifrucatedLong add(BifrucatedLong other) {
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

    public BifrucatedLong subtract(double value) { return subtract(new BifrucatedLong(value)); }
    public BifrucatedLong subtract(BifrucatedLong other) {
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
    public int compareTo(BifrucatedLong o) {
        return this.packed > o.packed ? 1 : (this.packed < o.packed ? -1 : 0);
    }

    @Override
    public int intValue() {
        return (int)Math.min(BifrucatedLong.unpackHigh(packed), Integer.MAX_VALUE);
    }

    @Override
    public long longValue() {
        return BifrucatedLong.unpackHigh(packed);
    }

    @Override
    public float floatValue() {
        return (float)doubleValue();
    }
    
    @Override
    public double doubleValue() {
        return (double)longValue() + ((double)BifrucatedLong.unpackLow(packed) / (double)MAX_LOW);
    }

    public BigDecimal bigValue() {
        return new BigDecimal(toString());
    }

    public boolean isZero() { return packed <= 0; }
    public BifrucatedLong zeroOut() {
        if(immutabilityCheck()) return mutableCopy().zeroOut();
        packed = 0;
        return this;
    }

    public boolean isMax() { return packed >= MAX_HIGH; }
    public BifrucatedLong maxOut() {
        if(immutabilityCheck()) return mutableCopy().zeroOut();
        packed = MAX_HIGH;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(!(obj instanceof BifrucatedLong that)) return false;
        return this.packed == that.packed;
    }

    @Override
    public int hashCode() {
        return (int)(packed ^ (packed >>> 32));
    }

    @Override
    public String toString() {
        String dec = "" + ((double)BifrucatedLong.unpackLow(packed) / (double)MAX_LOW);
        return longValue() + "." + dec.substring(2, dec.length() - 1);
    }

    private boolean immutabilityCheck() {
        if(!immutable) return false;
        Mechano.LOGGER.warn("Attempted to perform a modifying operation on a BifrucatedLong - A copy will be returned instead.");
        return true;
    }
}
