package com.quattage.mechano.core.block.orientation.relative;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.core.block.DirectionTransformer;
import com.quattage.mechano.core.block.orientation.CombinedOrientation;
import com.simibubi.create.foundation.utility.Color;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;

public class RelativeDirection {

    private final Relative rel;
    private Direction facingDir;

    public RelativeDirection(Relative rel) {
        this.rel = rel;
        this.facingDir = rel.getDefaultDir();
    }

    /***
     * Rotates this RelativeDirection to follow the given direction.
     * @param dir Direction the block is facing
     */
    public RelativeDirection rotate(CombinedOrientation dir) {
        if(rel == Relative.TOP) {
            facingDir = dir.getLocalUp();
            return this;
        }
        if(rel == Relative.BOTTOM) {
            facingDir = dir.getLocalUp().getOpposite();
            return this;
        }
        if(rel == Relative.FRONT) {
            facingDir = dir.getLocalForward();
            return this;
        }
        if(rel == Relative.BACK) {
            facingDir = dir.getLocalForward().getOpposite();
            return this;
        }

        Matrix4f fac = toMatrix(rel.getRelative());
        if(dir.getLocalForward().getAxis() == Axis.Y) {
            fac.rotate(dir.getLocalUp().getOpposite().getRotation());
            facingDir = Direction.rotate(fac, dir.getLocalForward());
            return this;
        }
        // TODO this works but fucking christ it sucks
        if(dir.getLocalUp().getAxis() != Axis.Y) {
            if(rel == Relative.RIGHT) {
                if(DirectionTransformer.isPositive(dir.getLocalForward())) {
                    facingDir = Direction.UP;
                    return this;
                } 
                facingDir = Direction.DOWN;
                return this;
            }
            if(DirectionTransformer.isPositive(dir.getLocalForward())) {
                facingDir = Direction.DOWN;
                return this;
            } 
            facingDir = Direction.UP;
            return this;
        } 
        
        fac.rotate(dir.getLocalUp().getRotation());
        facingDir = Direction.rotate(fac, dir.getLocalForward());
        return this;
    }

    public Matrix4f toMatrix(Quaternionf in) {
        return new Matrix4f().set(in);
    }

    public Direction get() {
        return facingDir;
    }

    public Relative getRaw() {
        return rel;
    }

    public String toString() {
        return rel.name() + " -> '" + facingDir.name() + "'";
    }

    public boolean equals(RelativeDirection other) {
        if(rel.ordinal() != other.rel.ordinal()) return false;
        if(facingDir.ordinal() != other.facingDir.ordinal()) return false;
        return true;
    }

    public int hashCode() {
        return rel.ordinal();
    }

    public Color getColor() {
        return rel.getColor();
    }

    public static RelativeDirection[] populateAll() {
        Relative[] allRels = Relative.values();
        RelativeDirection[] out = new RelativeDirection[allRels.length];
        for(int x = 0; x < out.length; x++) 
            out[x] = new RelativeDirection(allRels[x]);
        return out;
    }
}
