package com.quattage.mechano.foundation.api.watt;

public interface VoltageReturnable {

    public static final float EULER = 2.7182818284590452353602874713527f;

    /**
     * Gets the Voltage at the given percent.
     * @param percent (Optional) - scalar value (0 to 1)
     * @return Voltage mapped to the given value, 
     * or a default if no scalar is supplied.
     */
    abstract Voltage get(float percent);

    /**
     * Gets the Voltage at the given percent.
     * @param percent (Optional) - scalar value (0 to 1)
     * @return Voltage mapped to the given value, 
     * or a default if no scalar is supplied.
     */
    abstract Voltage get();

    /**
     * Helper method for sigmoid functions
     * https://en.wikipedia.org/wiki/Sigmoid_function
     * @param x position along the x axis to find
     * @param q location of the sigmoud across the x axis
     * @param k steepness of the sigmouid
     * @return response value along the decay function defined by q, k at x
     */
    public static double sigmoid(int x, int q, float k) {
        return 1d / (1d + Math.exp(-k * (float)(x - q)));
    }

    /**
     * Collects all calculated voltage values
     * in an array, useful for unit tests and 
     * for building an acceleration structure
     * around pre-computed voltage values.
     * @return An array, usually 255 members long, storing pre-computed voltage values.
     */
    abstract Voltage[] getPrecomputed(int res);
}