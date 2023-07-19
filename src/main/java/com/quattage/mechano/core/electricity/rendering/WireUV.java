package com.quattage.mechano.core.electricity.rendering;

public record WireUV(float x0, float x1) {
    public static final WireUV SKEW_A = new WireUV(0, 3);
    public static final WireUV SKEW_B = new WireUV(3, 6);
}
