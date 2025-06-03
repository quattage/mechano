package com.quattage.mechano.foundation.api.catenary;

import org.joml.Math;

import com.quattage.mechano.foundation.api.landmark.NodeIdentifiable;

import net.minecraft.world.phys.Vec3;

public class DisplacementHash {

    private final int hash;

    public static DisplacementHash of(Vec3 start, Vec3 end) {
        float vertical = (float)start.y - (float)end.y;
        float d0 = (float)start.x - (float)end.x;
        float d2 = (float)start.z - (float)end.z;
        float horizontal = Math.sqrt(d0 * d0 + d2 * d2);
        return new DisplacementHash(Float.floatToIntBits(vertical) * 31 + Float.floatToIntBits(horizontal));
    }

    public static DisplacementHash of(NodeIdentifiable start, NodeIdentifiable end) {
        int vertical = start.getY() - end.getY();
        float d0 = start.getX() - end.getX();
        float d2 = start.getZ() - end.getZ();
        float horizontal = Math.sqrt(d0 * d0 + d2 * d2);
        return new DisplacementHash(vertical * 31 + Float.floatToIntBits(horizontal) * 31 + start.getIndex() * 31 + end.getIndex());
    }

    public static DisplacementHash ofDirectional(Vec3 start, Vec3 end) {
        float dX = (float)start.x - (float)end.x;
        float dY = (float)start.y - (float)end.y;
        float dZ = (float)start.z - (float)end.z;
        return new DisplacementHash(Float.floatToIntBits(dX) * 31 + Float.floatToIntBits(dY) * 31 + Float.floatToIntBits(dZ));
    }

    public static DisplacementHash ofDirectional(NodeIdentifiable start, NodeIdentifiable end) {
        int dX = start.getX() - end.getX();
        int dY = start.getY() - end.getY();
        int dZ = start.getZ() - end.getZ();
        return new DisplacementHash(dX * 31 + dY * 31 + dZ * 31 + start.getIndex() * 31 + end.getIndex());
    }

    private DisplacementHash(int hash) {
        this.hash = hash;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(!(obj instanceof DisplacementHash that)) return false;
        return this.hash == that.hash;
    }
}
