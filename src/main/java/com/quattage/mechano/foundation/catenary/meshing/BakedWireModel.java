package com.quattage.mechano.foundation.catenary.meshing;

public class BakedWireModel {

    // x, y, z, u, v
    private float[] mesh;

    public void prime(int points) {
        this.mesh = new float[points * 20];
    }

}
