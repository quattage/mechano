package com.quattage.mechano.foundation.block.orientation;

import java.util.Locale;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Vec3i;
import net.minecraft.util.StringRepresentable;

/***
 * Represents a direction in the local space. Modifiable by global directions
 * to achieve rotations.
 */
public enum Relative implements StringRepresentable {
    FRONT(0, 0, 0, Direction.NORTH),
    BACK(0, 180, 0, Direction.SOUTH),
    LEFT(0, 90, 0, Direction.WEST),
    RIGHT(0, 270, 0, Direction.EAST),
    TOP(270, 0, 0, Direction.UP),
    BOTTOM(90, 0, 0, Direction.DOWN);

    private final Direction defaultDir;

    Relative(int x, int y, int z, Direction defaultDir) {
        this.defaultDir = defaultDir;
    }

    public static Relative of(Direction dir) {
        return switch (dir) {
            case NORTH -> FRONT;
            case SOUTH -> BACK;
            case WEST -> LEFT;
            case EAST -> RIGHT;
            case UP -> TOP;
            case DOWN -> BOTTOM;
        };
    }

    /**
     * Applies this Relative given the provided {@link CombinedOrientation orientation}
     * @param orientation Orientation to apply
     * @return The global {@link Direction} relative to the provided orientation
     */
    public Direction apply(CombinedOrientation orientation) {
        Vec3i up = orientation.getLocalUp().getNormal();
        Vec3i fn = orientation.getLocalForward().getNormal();
        Vec3i tan = Relative.cross(up, fn);
        Vec3i ln = this.defaultDir.getNormal();
        return Direction.getNearest(
            ln.getX() * tan.getX() + ln.getY() * up.getX() + ln.getZ() * fn.getX(), 
            ln.getX() * tan.getY() + ln.getY() * up.getY() + ln.getZ() * fn.getY(), 
            ln.getX() * tan.getZ() + ln.getY() * up.getZ() + ln.getZ() * fn.getZ()
        );
    }

    private static Vec3i cross(Vec3i a, Vec3i b) {
        return new Vec3i(
            a.getY() * b.getZ() - a.getZ() * b.getY(),
            a.getZ() * b.getX() - a.getX() * b.getZ(),
            a.getX() * b.getY() - a.getY() * b.getX()
        );
    }

    public Direction getDefaultDir() {
        return defaultDir;
    }

    public Axis getAxis() {
        return defaultDir.getAxis();
    }

    public Relative copy() {
        return Relative.values()[this.ordinal()];
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return getSerializedName();
    }
}
