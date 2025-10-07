package com.quattage.mechano.foundation.block.orientation;

import java.util.Locale;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Vec3i;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Rotation;
/***
 * A CombinedOrientation is an implementation of Minecraft's BlockState enums that
 * combines two Direction objects, called localUp and localForward. LocalUp represents the 
 * direction direction that the block's top is facing, where localForward represents the
 * direction the block's front is facing. This accounts for all 24 possible directions of a
 * block in Minecraft.
 */
public enum CombinedOrientation implements StringRepresentable {

    DOWN_NORTH(new Vec3i(180, 180, 0), new Vec3i(0, 0, 180), Direction.DOWN, Direction.NORTH),      // 0
    DOWN_WEST(new Vec3i(180, 90, 0), new Vec3i(0, 270, 180), Direction.DOWN, Direction.WEST),
    DOWN_SOUTH(new Vec3i(180, 0, 0), new Vec3i(0, 180, 180), Direction.DOWN, Direction.SOUTH),
    DOWN_EAST(new Vec3i(180, 270, 0), new Vec3i(0, 90, 180), Direction.DOWN, Direction.EAST),

    UP_NORTH(new Vec3i(0, 0, 0), new Vec3i(0, 0, 0), Direction.UP, Direction.NORTH),             // 4
    UP_WEST(new Vec3i(0, 270, 0), new Vec3i(0, 90, 0), Direction.UP, Direction.WEST),
    UP_SOUTH(new Vec3i(0, 180, 0), new Vec3i(0, 180, 0), Direction.UP, Direction.SOUTH),
    UP_EAST(new Vec3i(0, 90, 0), new Vec3i(0, 270, 0), Direction.UP, Direction.EAST),

    NORTH_UP(new Vec3i(270, 270, 0), new Vec3i(270, 0, 180), Direction.NORTH, Direction.UP),             // 8
    NORTH_EAST(new Vec3i(180, 270, 0), new Vec3i(270, 0, 90), Direction.NORTH, Direction.EAST),
    NORTH_DOWN(new Vec3i(90, 270, 0), new Vec3i(270, 0, 0), Direction.NORTH, Direction.DOWN),
    NORTH_WEST(new Vec3i(0, 270, 90), new Vec3i(270, 0, 270), Direction.NORTH, Direction.WEST),

    EAST_UP(new Vec3i(270, 0, 0), new Vec3i(0, 90, 270), Direction.EAST, Direction.UP),                // 12
    EAST_SOUTH(new Vec3i(180, 0, 0), new Vec3i(0, 180, 270), Direction.EAST, Direction.SOUTH),
    EAST_DOWN(new Vec3i(90, 0, 90), new Vec3i(0, 270, 270), Direction.EAST, Direction.DOWN),
    EAST_NORTH(new Vec3i(0, 0, 0), new Vec3i(0, 0, 270), Direction.EAST, Direction.NORTH),

    SOUTH_UP(new Vec3i(270, 90, 0), new Vec3i(90, 0, 0), Direction.SOUTH, Direction.UP),             // 16
    SOUTH_WEST(new Vec3i(180, 90, 0), new Vec3i(90, 0, 90), Direction.SOUTH, Direction.WEST),
    SOUTH_DOWN(new Vec3i(90, 90, 0), new Vec3i(90, 0, 180), Direction.SOUTH, Direction.DOWN),
    SOUTH_EAST(new Vec3i(0, 90, 0), new Vec3i(90, 0, 270), Direction.SOUTH, Direction.EAST),

    WEST_UP(new Vec3i(270, 180, 0), new Vec3i(0, 270, 90), Direction.WEST, Direction.UP),               // 20
    WEST_NORTH(new Vec3i(180, 180, 0), new Vec3i(0, 0, 90), Direction.WEST, Direction.NORTH),
    WEST_DOWN(new Vec3i(90, 180, 0), new Vec3i(0, 90, 90), Direction.WEST, Direction.DOWN),
    WEST_SOUTH(new Vec3i(0, 180, 0), new Vec3i(0, 180, 90), Direction.WEST, Direction.SOUTH);

    private final Direction localUp;
    private final Direction localForward;

    private final Vec3i stateRotation;
    private final Vec3i absRotation;

    private static final Int2ObjectMap<CombinedOrientation> COMBINED_LOOKUP = Util.make(new Int2ObjectOpenHashMap<>(values().length), b -> {
        for(CombinedOrientation direction : values()) {
            b.put(lookupKey(direction.localUp, direction.localForward), direction);
        }
    });

    private CombinedOrientation(Vec3i stateRotation, Vec3i absRotation, Direction localUp, Direction localForward) {
        this.localUp = localUp;
        this.localForward = localForward;
        this.stateRotation = stateRotation;
        this.absRotation = absRotation;
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

    public static CombinedOrientation next(CombinedOrientation in) {
        return CombinedOrientation.values()[(in.ordinal() + 1) % CombinedOrientation.values().length];
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

    public CombinedOrientation applyRotation(Rotation rotation) {
        if(this.ordinal() < 8) {
            return switch (rotation) {
                case CLOCKWISE_180 -> cycleLocalForward(cycleLocalForward(this));
                case CLOCKWISE_90 -> cycleLocalForward(cycleLocalForward(cycleLocalForward(this)));
                case COUNTERCLOCKWISE_90 -> cycleLocalForward(this);
                case NONE -> this;
            };
        }
        switch (rotation) {
            case CLOCKWISE_180 -> {
                int ord = ordinal() + 8;
                if(ord > 23) ord = ordinal() - 8;
                return CombinedOrientation.values()[ord];
            }
            case CLOCKWISE_90 -> {
                int ord = ordinal() + 4;
                if(ord > 23) ord = 8 + (ord % 4);
                return CombinedOrientation.values()[ord];
            }
            case COUNTERCLOCKWISE_90 -> {
                int ord = ordinal() - 4;
                if(ord < 8) ord = 20 - (ord % 4);
                return CombinedOrientation.values()[ord];
            }
            case NONE -> { return this; }
        };
        throw new IllegalStateException("bruh");
    }

    private static int getGroupMaxRange(int in) {
        return getGroupIndex(in) * 4;
    }

    public Vec3i getStateRotation() {
        if(stateRotation == null) return absRotation;
        return stateRotation;
    }

    public Vec3i getAbsoluteRotation() {
        return absRotation;
    }

    /**
     * When a block is clicked, this method will retieve the quadrant (triangular, offset by 45 degrees)
     * that the player clicked on and return that quadrant as a Direction relative to the orientation of
     * the face.
     * @param context BlockPlaceContext for the placement of the block
     * @param orient Direction if the clicked face
     * @param negateCenter Set to true if clicking near the middle of the block should be differentiated
     * (e.g. placing a block facing away when the middle is clicked)
     * @return
     */
    public static Direction getClickedQuadrant(BlockPlaceContext context, Direction orient, boolean negateCenter) {
        double x = 0;
        double y = 0;

        if(orient.getAxis() == Axis.Y) {
            y = context.getClickLocation().z - (double)context.getClickedPos().getZ();
            x = context.getClickLocation().x - (double)context.getClickedPos().getX();
        } else if(orient.getAxis() == Axis.Z) {
            y = context.getClickLocation().y - (double)context.getClickedPos().getY();
            x = context.getClickLocation().x - (double)context.getClickedPos().getX();
        } else if(orient.getAxis() == Axis.X) {
            y = context.getClickLocation().y - (double)context.getClickedPos().getY();
            x = context.getClickLocation().z - (double)context.getClickedPos().getZ();
        }

        double lineA = 1 * x + 0;
        double lineB = -1 * x + 1;

        if(negateCenter) {
            double cen = 0.3;
            if(x > cen && x < (1 - cen) && y > cen && y < (1 - cen))
                return orient;
        }

        // sorry
        if(orient == Direction.UP) {
            if(y <= lineA && y <= lineB) return Direction.NORTH;    // down
            if(y <= lineA && y >= lineB) return Direction.EAST;     // right
            if(y >= lineA && y >= lineB) return Direction.SOUTH;    // up
            if(y >= lineA && y <= lineB) return Direction.WEST;     // left
        } else if(orient == Direction.DOWN) {
            if(y <= lineA && y <= lineB) return Direction.NORTH;   
            if(y <= lineA && y >= lineB) return Direction.EAST; 
            if(y >= lineA && y >= lineB) return Direction.SOUTH;    
            if(y >= lineA && y <= lineB) return Direction.WEST;  
        } else if(orient == Direction.NORTH) {
            if(y <= lineA && y <= lineB) return Direction.DOWN;
            if(y <= lineA && y >= lineB) return Direction.EAST;
            if(y >= lineA && y >= lineB) return Direction.UP;
            if(y >= lineA && y <= lineB) return Direction.WEST;
        } else if(orient == Direction.SOUTH) {
            if(y <= lineA && y <= lineB) return Direction.DOWN;
            if(y <= lineA && y >= lineB) return Direction.EAST;
            if(y >= lineA && y >= lineB) return Direction.UP;
            if(y >= lineA && y <= lineB) return Direction.WEST;
        } else if(orient == Direction.EAST) {
            if(y <= lineA && y <= lineB) return Direction.DOWN;
            if(y <= lineA && y >= lineB) return Direction.SOUTH;
            if(y >= lineA && y >= lineB) return Direction.UP;
            if(y >= lineA && y <= lineB) return Direction.NORTH;
        } else if(orient == Direction.WEST) {
            if(y <= lineA && y <= lineB) return Direction.DOWN;
            if(y <= lineA && y >= lineB) return Direction.SOUTH;
            if(y >= lineA && y >= lineB) return Direction.UP;
            if(y >= lineA && y <= lineB) return Direction.NORTH;
        }
        return Direction.UP;
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
