package com.quattage.mechano.foundation.api.catenary;

import org.joml.Vector3f;

public class RestitutionTracker {

    private final float epsilon;
    private float avgVelocity = 0f;
    private int tick = 0;
    private float previousAccumulatedError = 0;
    private float accumulatedError = 0;

    public RestitutionTracker(float precision) {
        this.epsilon = precision;
    }

    public boolean isResting() {
        if(avgVelocity < CatenaryAttributes.RESTITUTION_VELOCITY && (Math.abs(accumulatedError - previousAccumulatedError) < epsilon)) {
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
        accumulatedError = (newError / (float)(steps * CatenaryAttributes.SOLVER_STEPS));
        average(steps);
    }

    public void apply(Vector3f velocity) {
        this.avgVelocity += velocity.length();
    }

    public void average(int total) {
        this.avgVelocity /= total;
    }

    public boolean isCascading() {
        return avgVelocity > 1e10 || Float.isNaN(avgVelocity) || accumulatedError > 500;
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
