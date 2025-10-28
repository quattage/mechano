package com.quattage.mechano.api.catenary.meshing;

import org.joml.Vector3f;

import com.quattage.mechano.api.catenary.CatenaryModel;

/**
 * A helper class for determining whether or not a verlet-based 
 * catenary simulation has reached a state of minimum potential 
 * energy. This class is designed to be instantiated by 
 * {@link CatenaryModel instances} for convenience.
 */
public class EntropyTracker {

    // TODO this class is LOD unfriendly and kind of expensive to store in every single catenary instance

    private final float epsilon;
    private float avgVelocity = 0f;
    private int tick = 0;
    private float previousAccumulatedError = 0;
    private float accumulatedError = 0;

    public EntropyTracker(float precision) {
        this.epsilon = precision;
    }

    public boolean isResting() {
        if(avgVelocity < CatenaryRenderFeatures.RESTITUTION_SPEED && (Math.abs(accumulatedError - previousAccumulatedError) < epsilon)) {
            if(tick > 42) return true;
            tick++;
            return false;
        }
        tick = 0;
        return false;
    }

    public void accumulate(float error) {
        accumulatedError += error;
    }

    public void walk(float newError) {
        previousAccumulatedError = accumulatedError;
        accumulatedError = newError;
    }

    public void walk(float newError, int steps) {
        previousAccumulatedError = accumulatedError;
        accumulatedError = (newError / (float)(steps * CatenaryRenderFeatures.SETTINGS.getSolverSteps()));
        this.avgVelocity /= (float)steps;
    }

    public void apply(Vector3f velocity) {
        this.avgVelocity += velocity.length();
    }

    /**
     * A catenary is considered to be cascading when it receives a change
     * in velocity that is too great, or if its accumulated constraint error
     * is too high. This can occur in cases where catenary simulations
     * become unstable or too long/complex, where they're unable to resolve 
     * to a stable output. In cases like this, steps should be taken to ensure 
     * that the catenary is removed from the world before it causes extreme 
     * visual artifacts that are generally unpleasant, but may also affect 
     * people with photosensitivity.
     * @return <code>true</code> if this EntropyTracker contains data that 
     * suggests its instantiating catenary is cascading.
     */
    public boolean isCascading() {
        return Math.abs(avgVelocity) > 1e10 || Float.isNaN(avgVelocity) || accumulatedError > 500;
    }

    public void softReset() {
        this.avgVelocity = 0;
    }

    public void reset() {
        this.avgVelocity = 0;
        this.tick = 0;
        this.previousAccumulatedError = 0;
        this.accumulatedError = 0;
    }

    @Override
    public String toString() {
        return "RestitutionTracker[" + avgVelocity + "m/t, " + accumulatedError + " :: " + tick + "t]";
    }
}
