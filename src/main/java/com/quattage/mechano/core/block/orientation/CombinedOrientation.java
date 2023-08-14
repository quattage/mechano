package com.quattage.mechano.core.block.orientation;

import java.util.Locale;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec2;
import com.quattage.mechano.core.block.DirectionTransformer;

/***
 * A CombinedOrientation is an implementation of Minecraft's BlockState enums that
 * combines two Direction objects, called localUp and localForward. LocalUp represents the 
 * direction direction that the block's top is facing, where localForward represents the
 * direction the block's front is facing. This accounts for all 24 possible directions of a
 * block in Minecraft.
 */
public enum CombinedOrientation implements StringRepresentable {
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
    private static final Int2ObjectMap<CombinedOrientation> COMBINED_LOOKUP = Util.make(new Int2ObjectOpenHashMap<>(values().length), (boysmell) -> {
        for(CombinedOrientation direction : values()) {
            boysmell.put(lookupKey(direction.localUp, direction.localForward), direction);
        }
    });

    private CombinedOrientation(String name, Direction localUp, Direction localForward) {
        this.name = name;
        this.localUp = localUp;
        this.localForward = localForward;
    }

    private static int lookupKey(Direction localUp, Direction localForward) {
        return localUp.ordinal() << 3 | localForward.ordinal();
    }

    /***
     * Creates a CombinedOrientation derived from two Directions.
     * @param localUp The local up direction
     * @param localForward The local forward direction
     * @throws NullPointerException if any given parameter is null
     * @throws IllegalStateException if the given directions are incompatable - 
     * For example, passing NORTH and NORTH would throw an error, as a NORTH NORTH 
     * CombinedOrientation cannot exist.
     * @return A CombinedOrientation with the given directions
     */
    public static CombinedOrientation combine(Direction localUp, Direction localForward) {
        if(localUp == null) 
            throw new NullPointerException("CombinedOrientation localUp was passed an illegal value of '" + localUp + "'");

        if(localForward == null) 
            throw new NullPointerException("CombinedOrientation orient was passed an invalid value of '" + localForward + "'");

        if(localUp.getAxis() == localForward.getAxis())
            throw new IllegalStateException("A CombinedOrientation facing '" + localUp.toString().toUpperCase() 
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

    /***
     * Cycles only the local forward direction of the given CombinedOrientation.
     * @param in
     * @return A modified CombinedOrientation.
     */
    public static CombinedOrientation cycleLocalForward(CombinedOrientation in) {
        int pos = in.ordinal();
        int newPos = pos + 1;
        if(newPos >= getGroupMaxRange(pos))
                newPos -= 4;
        return CombinedOrientation.values()[newPos];
    }

    /***
     * Cycles through all possible orientations starting at the given 
     * CombinedOrientation.
     * @param in
     * @return A modified CombinedOrientation.
     */
    public static CombinedOrientation cycle(CombinedOrientation in) {
        int pos = in.ordinal();
        if(getGroupIndex(in) < 3) { 
            pos += 4;
            if(pos > 23) pos -= 23;
            if(pos < 8) pos = 8 + (pos % 4);
            return CombinedOrientation.values()[pos];
        }
        pos += 4;
        if(pos > 23) pos -= 23;
        if(pos < 8) pos += 8;
        return CombinedOrientation.values()[pos];
    }

    /***
     * Converts a Direction, SimpleOrientation, or VerticalOrientation into a
     * CombinedDirection.
     * @param dir Direction to use as a basis for conversion
     * @return A new CombinedOrientation cooresponding to the given direction.
     */
    public static CombinedOrientation convert(Direction dir) {
        return dir.getAxis() == Axis.Y ? combine(dir, Direction.NORTH) : combine(dir, Direction.UP);
    }

    /***
     * Converts a Direction, SimpleOrientation, or VerticalOrientation into a
     * CombinedDirection.
     * @param dir Direction to use as a basis for conversion
     * @return A new CombinedOrientation cooresponding to the given direction.
     */
    public static CombinedOrientation convert(SimpleOrientation dir) {
        Direction cDir = DirectionTransformer.toDirection(dir.getOrient()); 
        return combine(dir.getCardinal(), cDir);
    }

    /***
     * Converts a Direction, SimpleOrientation, or VerticalOrientation into a
     * CombinedDirection.
     * @param dir Direction to use as a basis for conversion
     * @return A new CombinedOrientation cooresponding to the given direction.
     */
    public static CombinedOrientation convert(VerticalOrientation dir) {
        return combine(dir.getLocalVertical(), dir.getLocalFacing());
    }

    private static int getGroupIndex(CombinedOrientation in) {
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

    public XY getRotation() {
        XY out = new XY();

        switch(this) {
            case DOWN_EAST:
                out.setX(180);
                out.setY(270);
                return out;

            case DOWN_NORTH:
                out.setX(180);
                out.setY(180);
                return out;

            case DOWN_SOUTH:
                out.setX(180);
                out.setY();
                return out;

            case DOWN_WEST:
                out.setX(180);
                out.setY(90);
                return out;
            ////
            case EAST_DOWN:
                out.setX(90);
                out.setY();
                return out;

            case EAST_NORTH:
                out.setX();
                out.setY();
                return out;

            case EAST_SOUTH:
                out.setX(180);
                out.setY();
                return out;

            case EAST_UP:
                out.setX(270);
                out.setY();
                return out;
            ////
            case NORTH_DOWN:
                out.setX(90);
                out.setY(270);
                return out;

            case NORTH_EAST:
                out.setX(180);
                out.setY(270);
                return out;

            case NORTH_UP:
                out.setX(270);
                out.setY(270);
                return out;

            case NORTH_WEST:
                out.setX();
                out.setY(270);
                return out;
            ////
            case SOUTH_DOWN:
                out.setX(90);
                out.setY(90);
                return out;

            case SOUTH_EAST:
                out.setX();
                out.setY(90);
                return out;

            case SOUTH_UP:
                out.setX(270);
                out.setY(90);
                return out;

            case SOUTH_WEST:
                out.setX(180);
                out.setY(90);
                return out;
            ////
            case UP_EAST:
                out.setX();
                out.setY(90);
                return out;

            case UP_NORTH:
                out.setX();
                out.setY();
                return out;

            case UP_SOUTH:
                out.setX();
                out.setY(180);
                return out;

            case UP_WEST:
                out.setX();
                out.setY(270);
                return out;
            ////
            case WEST_DOWN:
                out.setX(90);
                out.setY(180);
                return out;

            case WEST_NORTH:
                out.setX(180);
                out.setY(180);
                return out;

            case WEST_SOUTH:
                out.setX();
                out.setY(180);
                return out;

            case WEST_UP:
                out.setX(270);
                out.setY(180);
                return out;

            default:
                throw new IllegalArgumentException("CombinedOrientation named '" + name() + "' is invalid!");
        }
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
