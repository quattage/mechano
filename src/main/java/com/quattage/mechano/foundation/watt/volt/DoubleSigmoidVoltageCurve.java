package com.quattage.mechano.foundation.watt.volt;

public class DoubleSigmoidVoltageCurve implements VoltageReturnable {

    private final Voltage vNominal;
    private final byte qUpper;
    private final byte qLower;
    private final float k;
    private final float u;

    public static final DoubleSigmoidVoltageCurve DEFAULT = DoubleSigmoidVoltageCurve.of(
        new Voltage(120),
        new Voltage(30),
        0.1f, 0.82f, 0.1f
    );

    public static DoubleSigmoidVoltageCurve of(Voltage vNominal, Voltage chargeBonus, float lower, float upper, float k) {
        float frac = (float)chargeBonus.getValue() / (float)vNominal.getValue();
        byte qL = (byte)(Math.round(Math.max(0.0001f, Math.min(1, lower)) * 255f) - 128);
        byte qU = (byte)(Math.round(Math.max(lower, Math.min(1, upper)) * 255f) - 128);
        return new DoubleSigmoidVoltageCurve(vNominal, qU, qL, Math.max(0.0001f, Math.min(5f, k)), frac);
    }

    public DoubleSigmoidVoltageCurve(Voltage vNom, byte qUpper, byte qLower, float k, float u) {
        this.vNominal = vNom;
        this.qUpper = qUpper;
        this.qLower = qLower;
        this.k = k;
        this.u = u;
    }

    public Voltage get(float percent) {
        int qX = (int)(percent * 255f) - 128;
        int calculated = (int)Math.round(vNominal.getValue() * (VoltageReturnable.sigmoid(qX, qLower, k) + (VoltageReturnable.sigmoid(qX, qUpper, k) * u)));
        return new Voltage(calculated); 
    }

    @Override
    public Voltage get() {
        return vNominal;
    }

    @Override
    public Voltage[] getPrecomputed(int samples) {
        Voltage[] output = new Voltage[samples];
        int pos = 0;
        float add = 1f / (float)samples;
        for(float x = add; x <= 1f; x += add) {
            output[pos] = get(x);
            pos++;
        }
        return output;
    }
}
