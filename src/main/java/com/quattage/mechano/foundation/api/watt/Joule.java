package com.quattage.mechano.foundation.api.watt;

/**
 * Joules are stored as a decomposed long and byte decimal.
 * This allows for precision in <code>1/128 (0.0078)</code> increments,
 * and a range of <code>+- 2^63-1</code>
 */
public class Joule {

    private final long whole;
    private final byte decimal;

    public static final Joule MAX = new Joule(Long.MAX_VALUE);
    public static final Joule MIN = new Joule(Long.MIN_VALUE);
    public static final Joule ZERO = new Joule(0);

    private static final int WRAP = 80;

    public Joule(int joules) {
        this.whole = joules;
        this.decimal = 0;
    }

    public Joule(long joules) {
        this.whole = joules;
        this.decimal = 0;
    }

    public Joule(long whole, byte decimal) {
        this.whole = whole;
        this.decimal = decimal;
    }

    public Joule(float joules) {
        whole = (long)joules;
        decimal = (byte)Math.round((joules - whole) * WRAP);
    }

    public Joule(double joules) {
        whole = (long)joules;
        decimal = (byte)Math.round((joules - whole) * WRAP);
    }

    public static Joule negate(Joule in) {
        return new Joule(-in.whole, (byte)-in.decimal);
    }

    public static Joule add(Joule a, Joule b) {
        long newWhole = a.whole + b.whole;
        if(((a.whole ^ newWhole) & (b.whole ^ newWhole)) < 0)
            return Joule.MAX;
        int newDecimal = a.decimal + b.decimal;
        return compose(newWhole, newDecimal);
    }

    public static Joule subtract(Joule a, Joule b) {
        long newWhole = a.whole - b.whole;
        if(((a.whole ^ b.whole) & (a.whole ^ newWhole)) < 0)
            return Joule.MIN;
        int newDecimal = a.decimal - b.decimal;
        return compose(newWhole, newDecimal);
    }

    public static Joule compose(long whole, int decimal) {
        // TODO streamline? bitwise? 1 - ((a ^ b) >>> 31) * 2;
        if((whole ^ decimal) < 0) {
            if(decimal < 0)
                return new Joule(whole - 1, positiveWrap(decimal));
            return new Joule(whole + 1, negativeWrap(decimal));
        }
        if(decimal < -WRAP) return new Joule(whole - 1, positiveWrap(decimal));
        if(decimal > WRAP) return new Joule(whole + 1, negativeWrap(decimal));
        if(decimal == WRAP) return new Joule(whole + 1);
        if(decimal == -WRAP) return new Joule(whole - 1);
        return new Joule(whole, (byte)decimal);
    }

    private static byte positiveWrap(int dec) {
        return (byte)Math.max(-WRAP, Math.min(dec + WRAP, WRAP));
    }

    private static byte negativeWrap(int dec) {
        return (byte)Math.max(-WRAP, Math.min(dec - WRAP, WRAP));
    }

    public float getDecimal() {
        return ((float)decimal / WRAP);
    }

    public long getWhole() {
        return whole;
    }

    /**
     * Gets the actual double value of this Joule.
     * If you can help it, don't use this, as the returned
     * double is likely to lose precision.
     * @return The actual double value in this Joule.
     */
    public double getApproximateValue() {
        return whole + getDecimal();
    }

    public boolean equals(Object other) {
        if(!(other instanceof Joule that)) return false;
        if(this.whole > that.whole) return false;
        if(this.whole < that.whole) return false;
        if(this.decimal < that.decimal) return false;
        if(this.decimal > that.decimal) return false;
        return true;
    }

    public int hashCode() {
        return (int)whole * decimal * 31;
    }

    public String toString() {
        String dec = (Math.abs(getDecimal()) + "").replace("0.", "");
        return whole + "." + dec;
    }
}
