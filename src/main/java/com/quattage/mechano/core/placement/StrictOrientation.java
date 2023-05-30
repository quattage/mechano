package com.quattage.mechano.core.placement;

import java.util.Locale;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public enum StrictOrientation implements StringRepresentable {
    // stores both the model orientation and the polarity of the stator's magnet
    DOWN_X("down_x", Direction.DOWN, Axis.X),
    DOWN_Z("down_z", Direction.DOWN, Axis.Z),

    UP_X("up_x", Direction.UP, Axis.X),
    UP_Z("up_z", Direction.UP, Axis.Z),

    NORTH_Y("north_y", Direction.NORTH, Axis.Y),
    NORTH_X("north_x", Direction.NORTH, Axis.X),

    SOUTH_Y("south_y", Direction.SOUTH, Axis.Y),
    SOUTH_X("south_x", Direction.SOUTH, Axis.X),

    EAST_Y("east_y", Direction.EAST, Axis.Y),
    EAST_Z("east_z", Direction.EAST, Axis.Z),

    WEST_Y("west_y", Direction.WEST, Axis.Y),
    WEST_Z("west_z", Direction.WEST, Axis.Z);

    public static final EnumProperty<StrictOrientation> INSTANCE = EnumProperty.create("orientation", StrictOrientation.class);

    private final String name;
    private final Direction cardinal;
    private final Axis orient;
    private static final Int2ObjectMap<StrictOrientation> COMBINED_LOOKUP = Util.make(new Int2ObjectOpenHashMap<>(values().length), (boysmell) -> {
        for(StrictOrientation StrictOrientation : values()) {
            boysmell.put(lookupKey(StrictOrientation.cardinal, StrictOrientation.orient), StrictOrientation);
        }
    });

    private StrictOrientation(String name, Direction cardinal, Axis orient) {
        this.name = name;
        this.cardinal = cardinal;
        this.orient = orient;
    }

    private static int lookupKey(Direction cardinal, Axis orient) {
        return cardinal.ordinal() << 3 | orient.ordinal();
    }

    public static StrictOrientation combine(Direction cardinal, Axis orient) {
        if(cardinal == null) 
            throw new IllegalArgumentException("StrictOrientation cardinal was passed an illegal value of '" + cardinal + "'");

        if(orient == null) 
            throw new IllegalArgumentException("StrictOrientation orient was passed an invalid value of '" + orient + "'");

        if(cardinal.getAxis() == orient)
            throw new IllegalArgumentException("An StrictOrientation facing '" + cardinal.toString().toUpperCase() 
                + "' is invalid for the axis '" + orient.toString().toUpperCase() + "'");
        int i = lookupKey(cardinal, orient);
        return COMBINED_LOOKUP.get(i);
    }

    public Direction getCardinal() {
        return this.cardinal;
    }

    public Axis getOrient() {
        return this.orient;
    }

    public static StrictOrientation cycleOrient(StrictOrientation in) {
        int pos = in.ordinal();
        if(pos % 2 == 0) pos += 1;
        else pos -= 1;
        return StrictOrientation.values()[pos];
    }

    public static StrictOrientation cycle(StrictOrientation in) {
        Direction cardinal = in.getCardinal();
        // Axis orient = in.getOrient();
        int pos = in.ordinal();
        if(cardinal.getAxis() == Axis.Y) {
            if(pos % 2 == 0) pos += 1;
            else pos -= 1;
            return StrictOrientation.values()[pos];
        }
        pos = pos += 2;
        if(pos > 11) pos -= 11;
        if(pos < 4) { 
            if(pos % 2 == 0) pos = 5;
            else pos = 4;
        }
        return StrictOrientation.values()[pos];
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
