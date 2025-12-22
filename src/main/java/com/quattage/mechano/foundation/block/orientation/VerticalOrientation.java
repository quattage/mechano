package com.quattage.mechano.foundation.block.orientation;

import java.util.Locale;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.StringRepresentable;

/***
 * A VerticalOrientation is a directional enum which stores a local up direction and
 * a local facing direction. Blocks that implement VerticalOrientation can face up or down
 * in any cardinal direction. This is essentially Minecraft's HorizontalDirection, but it
 * can also face down.
 */
public enum VerticalOrientation implements StringRepresentable {
    // stores t
    NORTH_UP(Direction.NORTH, true),            //0
    NORTH_DOWN(Direction.NORTH, false),      //1

    EAST_UP(Direction.EAST, true),               //2
    EAST_DOWN(Direction.EAST, false),         //3

    SOUTH_UP(Direction.SOUTH, true),            //4
    SOUTH_DOWN(Direction.SOUTH, false),      //5

    WEST_UP(Direction.WEST, true),               //6
    WEST_DOWN(Direction.WEST, false);         //7

    private final Direction localFacing;
    private final boolean localVertical;
    private static final Int2ObjectMap<VerticalOrientation> COMBINED_LOOKUP = Util.make(new Int2ObjectOpenHashMap<>(VerticalOrientation.values().length), boysmell -> {
        for(VerticalOrientation direction : VerticalOrientation.values()) {
            boysmell.put(VerticalOrientation.lookupKey(direction.localFacing, direction.localVertical), direction);
        }
    });

    VerticalOrientation(Direction localFacing, boolean localVertical) {
        this.localFacing = localFacing;
        this.localVertical = localVertical;
    }

    private static int lookupKey(Direction localFacing, boolean localVertical) {
        return localFacing.ordinal() << 3 | (localVertical ? 1 : 0);
    }

    public static VerticalOrientation combine(Direction localFacing, boolean localVertical) {
        if(localFacing == null) 
            throw new NullPointerException("VerticalOrientation localFacing can't be null!");
        if(localFacing.getAxis() == Axis.Y)
            throw new IllegalStateException("VerticalOrientation localFacing can't be on the Y Axis!");

        int i = VerticalOrientation.lookupKey(localFacing, localVertical);
        return VerticalOrientation.COMBINED_LOOKUP.get(i);
    }

    public static VerticalOrientation combine(Direction localFacing, Direction localVertical) {
        if(localFacing == null) 
            throw new NullPointerException("VerticalOrientation localFacing can't be null!");
        if(localFacing.getAxis() == Axis.Y)
            throw new IllegalStateException("VerticalOrientation localFacing can't be on the Y Axis!");
        if(localVertical == null) 
            throw new NullPointerException("VerticalOrientation localVertical can't be null!");
        if(localVertical.getAxis() == Axis.Y)
            throw new IllegalStateException("VerticalOrientation localVertical must be on the Y Axis!");

        int i = VerticalOrientation.lookupKey(localFacing, VerticalOrientation.yToBool(localVertical));
        return VerticalOrientation.COMBINED_LOOKUP.get(i);
    }

    public static boolean yToBool(Direction dir) {
        if(dir.getAxis() == Axis.Y) return true;
        return false;
    }

    public Direction getLocalFacing() {
        return this.localFacing;
    }

    public Direction getLocalVertical() {
        return localVertical ? Direction.UP : Direction.DOWN;
    }

    /***
     * Flips the vertical axis of the given VerticalOrientation.
     * @param in
     * @return A modified VerticalOrientation.
     */
    public static VerticalOrientation flipVertical(VerticalOrientation in) {
        int pos = in.ordinal();
        if(pos % 2 == 0) pos += 1;
        else pos -= 1;
        return VerticalOrientation.values()[pos];
    }

    /***
     * Cycles through all possible VerticalOrientations starting at
     * the given VerticalOrientation.
     * @param in
     * @return A modified VerticalOrientation.
     */
    public static VerticalOrientation cycle(VerticalOrientation in) {
        int pos = in.ordinal();
        pos = pos += 2;
        if(pos > 7) pos -= 7;
        return VerticalOrientation.values()[pos];
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
