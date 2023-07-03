package com.quattage.mechano.core.block.orientation;

import java.util.Locale;

import com.quattage.mechano.Mechano;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public enum VerticalOrientation implements StringRepresentable {
    // stores t
    NORTH_UP("north_up", Direction.NORTH, Direction.UP),            //0
    NORTH_DOWN("north_down", Direction.NORTH, Direction.DOWN),      //1

    EAST_UP("east_up", Direction.EAST, Direction.UP),               //2
    EAST_DOWN("east_down", Direction.EAST, Direction.DOWN),         //3

    SOUTH_UP("south_up", Direction.SOUTH, Direction.UP),            //4
    SOUTH_DOWN("south_down", Direction.SOUTH, Direction.DOWN),      //5

    WEST_UP("west_up", Direction.WEST, Direction.UP),               //6
    WEST_DOWN("west_down", Direction.WEST, Direction.DOWN);         //7

    private final String name;
    private final Direction localFacing;
    private final Direction localVertical;
    private static final Int2ObjectMap<VerticalOrientation> COMBINED_LOOKUP = Util.make(new Int2ObjectOpenHashMap<>(values().length), (boysmell) -> {
        for(VerticalOrientation direction : values()) {
            boysmell.put(lookupKey(direction.localFacing, direction.localVertical), direction);
        }
    });

    private VerticalOrientation(String name, Direction localFacing, Direction localVertical) {
        this.name = name;
        this.localFacing = localFacing;
        this.localVertical = localVertical;
    }

    private static int lookupKey(Direction localFacing, Direction localVertical) {
        return localFacing.ordinal() << 3 | localVertical.ordinal();
    }

    public static VerticalOrientation combine(Direction localFacing, Direction localVertical) {
        if(localFacing == null) 
            throw new IllegalArgumentException("VerticallyJustifiedComplexDirection localFacing can't be null!");
        if(localVertical == null) 
            throw new IllegalArgumentException("VerticallyJustifiedComplexDirection localVertical can't be null!");
        if(localFacing.getAxis() == Axis.Y)
            throw new IllegalArgumentException("VerticallyJustifiedComplexDirection localFacing can't be on the Y Axis!");
        if(localVertical.getAxis() != Axis.Y)
            throw new IllegalArgumentException("VerticallyJustifiedComplexDirection localVertical must be on the Y Axis!");

        int i = lookupKey(localFacing, localVertical);
        return COMBINED_LOOKUP.get(i);
    }

    public Direction getLocalFacing() {
        return this.localFacing;
    }

    public Direction getLocalVertical() {
        return this.localVertical;
    }

    public static VerticalOrientation flipVertical(VerticalOrientation in) {
        int pos = in.ordinal();
        if(pos % 2 == 0) pos += 1;
        else pos -= 1;
        return VerticalOrientation.values()[pos];
    }

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
