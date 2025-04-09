package com.quattage.mechano.foundation.block.orientation;

import javax.annotation.Nullable;

import net.createmod.catnip.data.Pair;
import org.joml.Vector3f;

import com.quattage.mechano.foundation.block.CombinedOrientedBlock;
import com.quattage.mechano.foundation.block.SimpleOrientedBlock;
import com.quattage.mechano.foundation.block.VerticallyOrientedBlock;
import com.quattage.mechano.foundation.helper.VectorHelper;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Vec3i;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/***
 * This class is designed to deal with all of the differing (and sometimes conflicting) 
 * ways that blocks express their orientations in Minecraft. I wrote CombinedOrientation 
 * for my needs, but it doesn't play very well with other directional formats. This class
 * provides helper methods for extracting directions from most other block types and
 * converting them to CombinedOrientations.
 */
public class DirectionTransformer {

    private static final Vec3i MIDDLE = new Vec3i(8, 8, 8);
    
    /***
     * Pulls the forwards facing direction from the given BlockState and returns it.
     * @param state
     * @return A direction representing the forward facing direction of the given BlockState,
     * or null if this BlockState's block doesn't hold a valid direction property.
     */
    @Nullable
    public static Direction getForward(BlockState state) {
        Block block = state.getBlock();

        if(block instanceof CombinedOrientedBlock)
            return state.getValue(CombinedOrientedBlock.ORIENTATION).getLocalForward();

        if(block instanceof SimpleOrientedBlock) 
            return toDirection(state.getValue(SimpleOrientedBlock.ORIENTATION).getOrient());

        if(block instanceof VerticallyOrientedBlock)
            return state.getValue(VerticallyOrientedBlock.ORIENTATION).getLocalFacing();

        if(block instanceof HorizontalDirectionalBlock)
            return state.getValue(HorizontalDirectionalBlock.FACING);

        if(block instanceof DirectionalBlock)
            return state.getValue(DirectionalBlock.FACING);

        if(block instanceof DirectionalKineticBlock)
            return state.getValue(DirectionalKineticBlock.FACING);

        if(block instanceof RotatedPillarBlock)
            return toDirection(state.getValue(RotatedPillarBlock.AXIS));
        
        if(block instanceof RotatedPillarKineticBlock)
            return toDirection(state.getValue(RotatedPillarKineticBlock.AXIS));

        if(block instanceof Block)
            return Direction.NORTH;

        return null;
    }

    /***
     * Pulls the upwards facing direction from the given BlockState and returns it.
     * If the given blockstate doesn't inherit from a Block that supports independent
     * up/forward facing positions, this will usually just return Direction.UP.
     * {@link #getForward(BlockState) getForward()}
     * @param state
     * @return A direction representing the upwards facing direction of the given BlockState,
     * or null if this BlockState's block doesn't hold a valid direction property.
     */
    @Nullable
    public static Direction getUp(BlockState state) {
        Block block = state.getBlock();
        
        if(block instanceof CombinedOrientedBlock)
            return state.getValue(CombinedOrientedBlock.ORIENTATION).getLocalUp();

        if(block instanceof SimpleOrientedBlock) 
            return state.getValue(SimpleOrientedBlock.ORIENTATION).getCardinal();

        if(block instanceof VerticallyOrientedBlock)
            return state.getValue(VerticallyOrientedBlock.ORIENTATION).getLocalVertical();

        if(block instanceof HorizontalDirectionalBlock)
            return Direction.UP;

        if(block instanceof DirectionalBlock)
            return state.getValue(DirectionalBlock.FACING);

        if(block instanceof DirectionalKineticBlock)
            return state.getValue(DirectionalKineticBlock.FACING);

        if(block instanceof RotatedPillarBlock)
            return toDirection(state.getValue(RotatedPillarBlock.AXIS));

        if(block instanceof RotatedPillarKineticBlock)
            return toDirection(state.getValue(RotatedPillarKineticBlock.AXIS));

        if(block instanceof Block)
            return Direction.UP;

        return null;
    }

    @Nullable
    public static <R extends Enum<R> & StringRepresentable> Direction getForward(Property<R> group, R prop) {
        if(prop instanceof CombinedOrientation o) return o.getLocalForward();
        if(prop instanceof VerticalOrientation o) return o.getLocalFacing();
        if(prop instanceof SimpleOrientation o) return toDirection(o.getOrient());
        if(prop instanceof Direction.Axis o) return toDirection(o);
        if(prop instanceof Direction o) return o;
        return null;
    }

    @Nullable
    public static <R extends Enum<R> & StringRepresentable> Direction getUp(Property<R> group, R prop) {
        if(prop instanceof CombinedOrientation o) return o.getLocalUp();
        if(prop instanceof VerticalOrientation o) return o.getLocalVertical();
        if(prop instanceof SimpleOrientation o) return o.getCardinal();
        if(prop instanceof Direction.Axis o) {
            if(group.getPossibleValues().size() > 2)
                return toDirection(o);
            else return Direction.UP;
        }
        if(prop instanceof Direction o) {
            if(group.getPossibleValues().size() > 4) 
                return o;
            else return Direction.UP;
        }
        return null;
    }

    /***
     * Converts a Direction, SimpleOrientation, or VerticalOrientation into a
     * CombinedDirection.
     * @param dir Direction to use as a basis for conversion
     * @return A new CombinedOrientation cooresponding to the given direction.
     */
    public static CombinedOrientation convert(Direction dir) {
        return dir.getAxis() == Axis.Y ? CombinedOrientation.combine(dir, Direction.NORTH) : CombinedOrientation.combine(dir, Direction.UP);
    }

    /***
     * Converts a Direction, SimpleOrientation, or VerticalOrientation into a
     * CombinedDirection.
     * @param dir Direction to use as a basis for conversion
     * @return A new CombinedOrientation cooresponding to the given direction.
     */
    public static CombinedOrientation convert(SimpleOrientation dir) {
        Direction cDir = DirectionTransformer.toDirection(dir.getOrient()); 
        return CombinedOrientation.combine(dir.getCardinal(), cDir);
    }

    /***
     * Converts a Direction, SimpleOrientation, or VerticalOrientation into a
     * CombinedDirection.
     * @param dir Direction to use as a basis for conversion
     * @return A new CombinedOrientation cooresponding to the given direction.
     */
    public static CombinedOrientation convert(VerticalOrientation dir) {
        return CombinedOrientation.combine(dir.getLocalVertical(), dir.getLocalFacing());
    }

    /***
     * Extracts a CombinedOrientation from a given BlockState, 
     * no matter how that state expresses its Orientation.
     * @param state BlockState to read from
     * @return A composed CombinedOrientation
     */
    public static CombinedOrientation extract(BlockState state) {

        Block block = state.getBlock();
        if(block == null) return CombinedOrientation.NORTH_UP;

        if(block instanceof CombinedOrientedBlock)
            return state.getValue(CombinedOrientedBlock.ORIENTATION);

        if(block instanceof SimpleOrientedBlock) 
            return convert(state.getValue(SimpleOrientedBlock.ORIENTATION));

        if(block instanceof VerticallyOrientedBlock)
            return convert(state.getValue(VerticallyOrientedBlock.ORIENTATION));

        if(block instanceof HorizontalDirectionalBlock)
            return convert(state.getValue(HorizontalDirectionalBlock.FACING));

        if(block instanceof DirectionalBlock)
            return convert(state.getValue(DirectionalBlock.FACING));

        if(block instanceof DirectionalKineticBlock)
            return convert(state.getValue(DirectionalKineticBlock.FACING));

        if(block instanceof RotatedPillarBlock)
            return convert(toDirection(state.getValue(RotatedPillarBlock.AXIS)));
        
        if(block instanceof RotatedPillarKineticBlock)
            return convert(toDirection(state.getValue(RotatedPillarKineticBlock.AXIS)));

        return CombinedOrientation.NORTH_UP;
    }

    public static Vec3i getRotation(BlockState state) {
        if(state.getBlock() instanceof CombinedOrientedBlock)
            return state.getValue(CombinedOrientedBlock.ORIENTATION).getStateRotation();

        Direction up = getUp(state);
        Direction forward = getForward(state);
        if(forward == up) return dir2Vec(up);

        return CombinedOrientation.combine(up, forward).getStateRotation();
    }

    public static  <R extends Enum<R> & StringRepresentable> Vec3i getRotation(Property<R> group, R prop) {
        
        if(prop instanceof CombinedOrientation orient)
            return orient.getAbsoluteRotation();

        Direction up = getUp(group, prop);
        Direction forward = getForward(group, prop);

        if(forward == up) return dir2Vec(up);
        
        return CombinedOrientation.combine(up, forward).getAbsoluteRotation();
    }

    private static Vec3i dir2Vec(Direction dir) {
        if(dir == Direction.DOWN) return new Vec3i(180, 0, 0);
        if(dir == Direction.EAST) return new Vec3i(90, 90, 0);
        if(dir == Direction.NORTH) return new Vec3i(90, 0, 0);
        if(dir == Direction.SOUTH) return new Vec3i(270, 0, 0);
        if(dir == Direction.UP) return new Vec3i(0, 0, 0);
        return new Vec3i(270, 90, 0);
    }

    public static boolean sharesLocalUp(BlockState first, BlockState second) {
        return getUp(first) == getUp(second);
    }

    /***
     * Returns true if this BlockState's directional format is ambiguous.
     * A BlockState's directional format is ambiguous when its local
     * upward and local forward directions are the same. <p>
     * For example, both <code>RotatedPillarBlocks</code> and 
     * <code>DirectionalBlocks</code> are ambiguous, since we cannot 
     * accurately determine a local up direction for them. However, 
     * a <code>HorizontalDirectionalBlock</code> is not ambiguous. 
     * Since <code>HorizontalDirectionalBlocks</code> are horizontal, 
     * we can surmise that the local up direction of this block is 
     * simply <code>Direction.UP
     * </code>
     * @param state
     * @return
     */
    public static boolean isAmbiguous(BlockState state) {
        return getUp(state).equals(getForward(state));
    } 

    public static BlockState rotate(BlockState state) {
        BlockState rotated = state.setValue(CombinedOrientedBlock.ORIENTATION,
            CombinedOrientation.cycleLocalForward(state.getValue(CombinedOrientedBlock.ORIENTATION))
        );
        return rotated;
    }

    /***
     * In this case, "Distinction" refers to the semantics that arise when dealing with
     * BlockStates. A Block's directional format requires distinction when it is capable
     * individually distinguishing local facing directions on more than one axis. <p>
     * 
     * HorizontalDirectionalBLocks are in a bit of an odd spot,
     * where they do have a known independent local up, but
     * this distinction is not necessary to make. <p> See 
     * {@link #isAmbiguous(BlockState) isAmbiguous()} for more
     * context.
     * @param state
     * @return
     */
    public static boolean isDistinctionRequired(BlockState state) {
        if(state.getBlock() instanceof HorizontalDirectionalBlock) return false;
        return !isAmbiguous(state);
    }

    public static boolean isHorizontal(BlockState state) {
        Direction up = getUp(state);
        return up.getAxis().isHorizontal();
    }

    public static Direction[] getPlaneFromAxis(Axis axis) {
        Direction[] out = new Direction[4];
        if(axis == Axis.Z) {
            out[0] = Direction.UP;
            out[1] = Direction.WEST;
            out[2] = Direction.DOWN;
            out[3] = Direction.EAST;
        } else if(axis == Axis.Y) {
            out[0] = Direction.NORTH;
            out[1] = Direction.EAST;
            out[2] = Direction.SOUTH;
            out[3] = Direction.WEST;
        } else {
            out[0] = Direction.UP;
            out[1] = Direction.NORTH;
            out[2] = Direction.DOWN;
            out[3] = Direction.SOUTH;
        }
        return out;
    }

    public static int getJoinedCornerStatus(Direction dirA, Direction dirB, Axis axis) {

        int dirAIndex = -1;
        int dirBIndex = -1;

        Direction[] plane = getPlaneFromAxis(axis);
        for(int x = 0; x < plane.length; x++) {
            if(plane[x] == dirA)
                dirAIndex = x;
            if(plane[x] == dirB)
                dirBIndex = x;
            x++;
        }

        boolean invert = isPositive(dirA) == isPositive(dirB);

        if(Math.abs(dirAIndex - dirBIndex) == 1 || Math.abs(dirAIndex - dirBIndex) == plane.length - 1)
            return dirAIndex < dirBIndex ? (invert ? 1 : -1) : (invert ? -1 : 1);
        return 0;
    }

    public static Direction getComplementingDirection(Direction dir, Axis axis) {
        Direction[] plane = getPlaneFromAxis(axis);

        for(int x = 0; x < plane.length; x++) {
            if(plane[x] == dir) {
                if(x < plane.length - 1)
                    return plane[x + 1];
                return plane[0];
            }
        }

        return plane[0];
    }

    public static BlockPos[] getAllCorners(BlockPos center, Axis axis) {

        Direction[] plane = getPlaneFromAxis(axis);

        BlockPos[] out = new BlockPos[4];
        out[0] = center.relative(plane[0]).relative(plane[1]);
        out[1] = center.relative(plane[0]).relative(plane[3]);
        out[2] = center.relative(plane[2]).relative(plane[3]);
        out[3] = center.relative(plane[2]).relative(plane[1]);
        return out;
    }

    public static BlockPos[] getAllAdjacent(BlockPos center, Axis axis) {

        Direction[] plane = getPlaneFromAxis(axis);

        BlockPos[] out = new BlockPos[4];
        out[0] = center.relative(plane[0]);
        out[1] = center.relative(plane[1]);
        out[2] = center.relative(plane[2]);
        out[3] = center.relative(plane[3]);
        return out;
    }

    public static Pair<BlockPos, BlockPos> getPositiveCorners(BlockPos center, Axis axis) {
        Direction[] plane = getPlaneFromAxis(axis);
        BlockPos c1 = center.relative(plane[0]).relative(plane[1]);
        BlockPos c2 = center.relative(plane[2]).relative(plane[3]);
        return Pair.of(c1, c2);
    }

    public static Direction toDirection(Axis ax) {
        if(ax == Axis.Y) return Direction.DOWN;
        if(ax == Axis.X) return Direction.WEST;
        return Direction.SOUTH;
    }

    public static boolean isPositive(Direction dir) {
        if(dir == Direction.UP) return true;
        if(dir == Direction.SOUTH) return true;
        if(dir == Direction.EAST) return true;
        return false;
    }

    public static Axis fromDisplacement(Vector3f offset) {


        Vector3f disp = new Vector3f(Math.abs(0.5f - offset.x), Math.abs(0.5f - offset.y), Math.abs(0.5f - offset.z));
    

        float greatest = VectorHelper.getGreatest(disp);

        if(greatest == disp.x) return Axis.X;
        if(greatest == disp.y) return Axis.Y;
        return Axis.Z;
    }
}