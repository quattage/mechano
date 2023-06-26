package com.quattage.mechano.core.placement;

import java.util.Locale;

import com.quattage.mechano.Mechano;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public enum StrictComplexDirection implements StringRepresentable {
    // stores both the diretion the model is facing in its local Y, and the direction the model is "looking" in its local Z.
    DOWN_NORTH("down_north", Direction.DOWN, Direction.NORTH),      // 0
    DOWN_EAST("down_east", Direction.DOWN, Direction.EAST),
    DOWN_SOUTH("down_south", Direction.DOWN, Direction.SOUTH),
    DOWN_WEST("down_west", Direction.DOWN, Direction.WEST),

    UP_NORTH("up_north", Direction.UP, Direction.NORTH),             // 4
    UP_WEST("up_west", Direction.UP, Direction.WEST),
    UP_SOUTH("up_south", Direction.UP, Direction.SOUTH),
    UP_EAST("up_east", Direction.UP, Direction.EAST),

    NORTH_UP("north_up", Direction.NORTH, Direction.UP),             // 8
    NORTH_EAST("north_east", Direction.NORTH, Direction.EAST),
    NORTH_DOWN("north_down", Direction.NORTH, Direction.DOWN),
    NORTH_WEST("north_west", Direction.NORTH, Direction.WEST),

    EAST_UP("east_up", Direction.EAST, Direction.UP),                // 16
    EAST_SOUTH("east_south", Direction.EAST, Direction.SOUTH),
    EAST_DOWN("east_down", Direction.EAST, Direction.DOWN),
    EAST_NORTH("east_north", Direction.EAST, Direction.NORTH),

    SOUTH_UP("south_up", Direction.SOUTH, Direction.UP),             // 12
    SOUTH_WEST("south_west", Direction.SOUTH, Direction.WEST),
    SOUTH_DOWN("south_down", Direction.SOUTH, Direction.DOWN),
    SOUTH_EAST("south_east", Direction.SOUTH, Direction.EAST),

    WEST_UP("west_up", Direction.WEST, Direction.UP),               // 20
    WEST_NORTH("west_north", Direction.WEST, Direction.NORTH),
    WEST_DOWN("west_down", Direction.WEST, Direction.DOWN),
    WEST_SOUTH("west_south", Direction.WEST, Direction.SOUTH);

    private final String name;
    private final Direction localUp;
    private final Direction localForward;
    private static final Int2ObjectMap<StrictComplexDirection> COMBINED_LOOKUP = Util.make(new Int2ObjectOpenHashMap<>(values().length), (boysmell) -> {
        for(StrictComplexDirection direction : values()) {
            boysmell.put(lookupKey(direction.localUp, direction.localForward), direction);
        }
    });

    private StrictComplexDirection(String name, Direction localUp, Direction localForward) {
        this.name = name;
        this.localUp = localUp;
        this.localForward = localForward;
    }

    private static int lookupKey(Direction localUp, Direction localForward) {
        return localUp.ordinal() << 3 | localForward.ordinal();
    }

    public static StrictComplexDirection combine(Direction localUp, Direction localForward) {
        if(localUp == null) 
            throw new IllegalArgumentException("StrictComplexDirection localUp was passed an illegal value of '" + localUp + "'");

        if(localForward == null) 
            throw new IllegalArgumentException("StrictComplexDirection orient was passed an invalid value of '" + localForward + "'");

        if(localUp.getAxis() == localForward.getAxis())
            throw new IllegalArgumentException("A StrictComplexDirection facing '" + localUp.toString().toUpperCase() 
                + "' cannot possess a local '" + localForward.toString().toUpperCase() + "' direction!");
        int i = lookupKey(localUp, localForward);
        return COMBINED_LOOKUP.get(i);
    }

    public Direction getLocalUp() {
        return this.localUp;
    }

    public Direction getLocalForward() {
        return this.localForward;
    }

    public static StrictComplexDirection cycleLocalForward(StrictComplexDirection in) {
        int pos = in.ordinal();
        int newPos = pos + 1;
        if(newPos >= getGroupMaxRange(pos))
                newPos -= 4;
        return StrictComplexDirection.values()[newPos];
    }

    public static StrictComplexDirection cycle(StrictComplexDirection in) {
        int pos = in.ordinal();
        if(getGroupIndex(in) < 3) { 
            pos += 4;
            if(pos > 23) pos -= 23;
            if(pos < 8) pos = 8 + (pos % 4);
            return StrictComplexDirection.values()[pos];
        }
        Mechano.log("cycling horizontal");
        pos += 4;
        if(pos > 23) pos -= 23;
        if(pos < 8) pos += 8;
        return StrictComplexDirection.values()[pos];
    }

    private static int getGroupIndex(StrictComplexDirection in) {
        int pos = in.ordinal();
        return getGroupIndex(pos);
    }

    private static int getGroupIndex(int in) {
        if(in > 19) return 6;
        if(in > 15) return 5;
        if(in > 11) return 4;
        if(in > 7) return 3;
        if(in > 3) return 2;
        return 1;
    }

    private static int getGroupMaxRange(int in) {
        return getGroupIndex(in) * 4;
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
