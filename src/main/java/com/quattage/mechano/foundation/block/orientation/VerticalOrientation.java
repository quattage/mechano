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
    NORTH_UP("north_up", Direction.NORTH, true),            //0
    NORTH_DOWN("north_down", Direction.NORTH, false),      //1

    EAST_UP("east_up", Direction.EAST, true),               //2
    EAST_DOWN("east_down", Direction.EAST, false),         //3

    SOUTH_UP("south_up", Direction.SOUTH, true),            //4
    SOUTH_DOWN("south_down", Direction.SOUTH, false),      //5

    WEST_UP("west_up", Direction.WEST, true),               //6
    WEST_DOWN("west_down", Direction.WEST, false);         //7

    private final String name;
    private final Direction localFacing;
    private final boolean localVertical;
    private static final Int2ObjectMap<VerticalOrientation> COMBINED_LOOKUP = Util.make(new Int2ObjectOpenHashMap<>(values().length), (boysmell) -> {
        for(VerticalOrientation direction : values()) {
            boysmell.put(lookupKey(direction.localFacing, direction.localVertical), direction);
        }
    });

    private VerticalOrientation(String name, Direction localFacing, boolean localVertical) {
        this.name = name;
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

        int i = lookupKey(localFacing, localVertical);
        return COMBINED_LOOKUP.get(i);
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

        int i = lookupKey(localFacing, yToBool(localVertical));
        return COMBINED_LOOKUP.get(i);
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
