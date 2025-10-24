package com.quattage.mechano.foundation.block.orientation;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

import net.createmod.catnip.theme.Color;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;

public class RelativeDirection implements OrientationUpdatable {

    private final Relative rel;
    private Direction facingDir;

    public static RelativeDirection[] all() {
        Relative[] allRels = Relative.values();
        RelativeDirection[] out = new RelativeDirection[allRels.length];
        for(int x = 0; x < out.length; x++) 
            out[x] = new RelativeDirection(allRels[x]);
        return out;
    }

    public RelativeDirection(Relative rel) {
        this.rel = rel;
        this.facingDir = rel.getDefaultDir();
    }

    @Override
    public void updateOrientation(CombinedOrientation dir) {
        switch(rel) {
            case TOP -> facingDir = dir.getLocalUp();
            case BOTTOM -> dir.getLocalUp().getOpposite();
            case FRONT -> dir.getLocalForward();
            case BACK -> dir.getLocalForward().getOpposite();
            default -> {}
        }
        Matrix4f fac = toMatrix(rel.getRelMatrix());
        if(dir.getLocalForward().getAxis() == Axis.Y) {
            fac.rotate(dir.getLocalUp().getOpposite().getRotation());
            facingDir = Direction.rotate(fac, dir.getLocalForward());
            return;
        }
        if(dir.getLocalUp().getAxis() != Axis.Y) {
            if(rel == Relative.RIGHT) {
                if(dir.getLocalForward().getAxisDirection() == Direction.AxisDirection.POSITIVE)
                    facingDir = Direction.UP;
                else facingDir = Direction.DOWN; return;
            }
            if(dir.getLocalForward().getAxisDirection() == Direction.AxisDirection.POSITIVE)
                facingDir = Direction.DOWN;
            else facingDir = Direction.UP; return;
        } 
        fac.rotate(dir.getLocalUp().getRotation());
        facingDir = Direction.rotate(fac, dir.getLocalForward());
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

    @Override
    public String toString() {
        return rel.name() + " -> '" + facingDir.name() + "'";
    }

    public boolean equals(RelativeDirection other) {
        if(rel.ordinal() != other.rel.ordinal()) return false;
        if(facingDir.ordinal() != other.facingDir.ordinal()) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return rel.ordinal();
    }

    public Color getColor() {
        return rel.getColor();
    }
}
