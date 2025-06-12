package com.quattage.mechano.foundation.catenary.model;

public class BakedCatenary {

    // px, py, pz, nx, ny, nz, u, v
    private float[] mesh;

    public void prime(int points) {
        this.mesh = new float[points * 20];
    }

}
