package com.quattage.mechano.foundation.electricity.core.anchor;

import org.joml.Vector3f;

import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class AnchorTransform {

    private final Vector3f baseOffset;
    private Vector3f realOffset;

    public AnchorTransform(int xOffset, int yOffset, int zOffset) {
        this.baseOffset = new Vector3f(
            toFloatMeasurement(xOffset), 
            toFloatMeasurement(yOffset), 
            toFloatMeasurement(zOffset)
        );
        this.realOffset = this.baseOffset;
    }

    private float toFloatMeasurement(int x) {
        return 0.0625f * ((float) x);
    }

    public Vec3 toRealPos(BlockPos pos) {
        return new Vec3(
            ((double) pos.getX()) + realOffset.x,
            ((double) pos.getX()) + realOffset.y,
            ((double) pos.getX()) + realOffset.z
        );
    }

    // you cannot stop me
    public void rotateToFace(CombinedOrientation dir) {
        switch(dir) {
            case DOWN_EAST:
                realOffset = new Vector3f(
                    iv(baseOffset.z),
                    iv(baseOffset.y),
                    iv(baseOffset.x)
                ); return;

            case DOWN_NORTH:
                realOffset = new Vector3f(
                    iv(baseOffset.z),
                    iv(baseOffset.y),
                    baseOffset.x
                ); return;

            case DOWN_SOUTH:
                realOffset = new Vector3f(
                    baseOffset.x,
                    iv(baseOffset.y),
                    iv(baseOffset.z)
                ); return;

            case DOWN_WEST:
                realOffset = new Vector3f(
                    baseOffset.z,
                    iv(baseOffset.y),
                    baseOffset.x
                ); return;

            case EAST_DOWN:
                realOffset = new Vector3f(
                    baseOffset.y,
                    baseOffset.z,
                    baseOffset.x
                ); return;

            case EAST_NORTH:
                realOffset = new Vector3f(
                    baseOffset.y,
                    iv(baseOffset.x),
                    baseOffset.z
                ); return;
                
            case EAST_SOUTH:
                realOffset = new Vector3f(
                    baseOffset.y,
                    baseOffset.x,
                    iv(baseOffset.z)
                ); return;
                
            case EAST_UP:
                realOffset = new Vector3f(
                    baseOffset.y,
                    iv(baseOffset.z),
                    iv(baseOffset.x)
                ); return;

            case NORTH_DOWN:
                realOffset = new Vector3f(
                    baseOffset.x,
                    baseOffset.z,
                    iv(baseOffset.y)
                ); return;

            case NORTH_EAST:
                realOffset = new Vector3f(
                    iv(baseOffset.z),
                    baseOffset.x,
                    iv(baseOffset.y)
                ); return;

            case NORTH_UP:
                realOffset = new Vector3f(
                    iv(baseOffset.x),
                    iv(baseOffset.z),
                    iv(baseOffset.y)
                ); return;

            case NORTH_WEST:
                realOffset = new Vector3f(
                    baseOffset.z,
                    iv(baseOffset.x),
                    iv(baseOffset.y)
                ); return;

            case SOUTH_DOWN:
                realOffset = new Vector3f(
                    iv(baseOffset.x),
                    baseOffset.z,
                    baseOffset.y
                ); return;

            case SOUTH_EAST:
                realOffset = new Vector3f(
                    iv(baseOffset.z),
                    iv(baseOffset.x),
                    baseOffset.y
                ); return;

            case SOUTH_UP:
                realOffset = new Vector3f(
                    baseOffset.x,
                    iv(baseOffset.z),
                    baseOffset.y
                ); return;

            case SOUTH_WEST:
                realOffset = new Vector3f(
                    baseOffset.z,
                    baseOffset.x,
                    baseOffset.y
                ); return;

            case UP_EAST:
                realOffset = new Vector3f(
                    iv(baseOffset.z),
                    baseOffset.y,
                    baseOffset.x
                ); return;

            case UP_NORTH:
                realOffset = baseOffset;
                return;

            case UP_SOUTH:
                realOffset = new Vector3f(
                    iv(baseOffset.x),
                    baseOffset.y,
                    iv(baseOffset.z)
                ); return;

            case UP_WEST:
                realOffset = new Vector3f(
                    baseOffset.z,
                    baseOffset.y,
                    iv(baseOffset.x)
                ); return;

            case WEST_DOWN:
                realOffset = new Vector3f(
                    iv(baseOffset.y),
                    baseOffset.z,
                    iv(baseOffset.x)
                ); return;

            case WEST_NORTH:
                realOffset = new Vector3f(
                    iv(baseOffset.y),
                    baseOffset.x,
                    baseOffset.z
                ); return;

            case WEST_SOUTH:
                realOffset = new Vector3f(
                    iv(baseOffset.y),
                    iv(baseOffset.x),
                    iv(baseOffset.z)
                ); return;

            case WEST_UP:
                realOffset = new Vector3f(
                    iv(baseOffset.y),
                    iv(baseOffset.z),
                    baseOffset.x
                ); return;
        }
    }

    /***
     * Inverts a float ranging from 0 to 1
     * @param in
     * @return
     */
    private float iv(float in) {
        float out = in;
        if(in < 0.5f) out = in + ((0.5f - in) * 2.0f);
        if(in > 0.5f) out = in + ((0.5f - in) * 2.0f);
        return out;
    }

    private String describeVector(Vector3f vec) {
        return "[" + vec.x + "," + vec.y + ", " + vec.z + "]";
    }

    public String toString() {
        return "AnchorTransform : {Base: " + describeVector(baseOffset) + " Real: " + describeVector(realOffset);
    }
}
