package com.quattage.mechano.foundation.block.orientation;

import java.util.Locale;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.StringRepresentable;

/***
 * A SimpleOrientation is an implementation of Minecraft's BlockState enum direction stuff which stores both 
 * the axis the model is aligned with (its local up, or "orient"), and the direction that the model's front lies 
 * on (its local forward, or "cardinal"). This is useful for blocks that need to distinguish between directions 
 * when facing straight up or down, but don't need as much control as a CombinedOrientation.
 */
public enum SimpleOrientation implements StringRepresentable {
    DOWN_X(Direction.DOWN, Axis.X),
    DOWN_Z(Direction.DOWN, Axis.Z),

    UP_X(Direction.UP, Axis.X),
    UP_Z(Direction.UP, Axis.Z),

    NORTH_Y(Direction.NORTH, Axis.Y),
    NORTH_X(Direction.NORTH, Axis.X),

    SOUTH_Y(Direction.SOUTH, Axis.Y),
    SOUTH_X(Direction.SOUTH, Axis.X),

    EAST_Y(Direction.EAST, Axis.Y),
    EAST_Z(Direction.EAST, Axis.Z),

    WEST_Y(Direction.WEST, Axis.Y),
    WEST_Z(Direction.WEST, Axis.Z);

    private final Direction cardinal;
    private final Axis orient;
    private static final Int2ObjectMap<SimpleOrientation> COMBINED_LOOKUP = Util.make(new Int2ObjectOpenHashMap<>(SimpleOrientation.values().length), boysmell -> {
        for(SimpleOrientation direction : SimpleOrientation.values()) {
            boysmell.put(SimpleOrientation.lookupKey(direction.cardinal, direction.orient), direction);
        }
    });

    SimpleOrientation(Direction cardinal, Axis orient) {
        this.cardinal = cardinal;
        this.orient = orient;
    }

    private static int lookupKey(Direction cardinal, Axis orient) {
        return cardinal.ordinal() << 3 | orient.ordinal();
    }

    /***
     * Creates a SimpleOrientation derived from a Direction and an axis.
     * @param cardinal The direction the model is facing
     * @param orient The axis the model is following
     * @throws NullPointerException if any given parameter is null
     * @throws IllegalStateException if the given directions are incompatable - 
     * For example, passing UP and Y would throw an error, as a UP Y 
     * SimpleOrientation cannot exist. (UP and Y belong to the same axis.)
     * @return A CombinedOrientation with the given directions
     */
    public static SimpleOrientation combine(Direction cardinal, Axis orient) {
        if(cardinal == null) 
            throw new NullPointerException("SimpleOrientation cardinal was passed an illegal value of '" + cardinal + "'");

        if(orient == null) 
            throw new NullPointerException("SimpleOrientation orient was passed an invalid value of '" + orient + "'");

        if(cardinal.getAxis() == orient)
            throw new IllegalStateException("A SimpleOrientation facing '" + cardinal.toString().toUpperCase() 
                + "' is invalid for the axis '" + orient.toString().toUpperCase() + "'");
        int i = SimpleOrientation.lookupKey(cardinal, orient);
        return SimpleOrientation.COMBINED_LOOKUP.get(i);
    }

    public Direction getCardinal() {
        return this.cardinal;
    }

    public Axis getOrient() {
        return this.orient;
    }

    /***
     * Cycles through the local forward directions of the given SimpleOrientation.
     * @param in
     * @return A modified SimpleOrientation.
     */
    public static SimpleOrientation cycleOrient(SimpleOrientation in) {
        int pos = in.ordinal();
        if(pos % 2 == 0) pos += 1;
        else pos -= 1;
        return SimpleOrientation.values()[pos];
    }

    /***
     * Cycles through all possible directions starting at the given
     * SimpleOrientation.
     * @param in
     * @return A modified SimpleOrientation.
     */
    public static SimpleOrientation cycle(SimpleOrientation in) {
        Direction cardinal = in.getCardinal();
        // Axis orient = in.getOrient();
        int pos = in.ordinal();
        if(cardinal.getAxis() == Axis.Y) {
            if(pos % 2 == 0) pos += 1;
            else pos -= 1;
            return SimpleOrientation.values()[pos];
        }
        pos = pos += 2;
        if(pos > 11) pos -= 11;
        if(pos < 4) { 
            if(pos % 2 == 0) pos = 5;
            else pos = 4;
        }
        return SimpleOrientation.values()[pos];
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
