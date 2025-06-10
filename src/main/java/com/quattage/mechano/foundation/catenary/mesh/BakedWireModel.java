package com.quattage.mechano.foundation.catenary.mesh;

public class BakedWireModel {

    // px, py, pz, nx, ny, nz, u, v
    private float[] mesh;

    public void prime(int points) {
        this.mesh = new float[points * 20];
    }

}
