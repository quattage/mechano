package com.quattage.mechano.core.block;

import javax.annotation.Nullable;

import org.antlr.v4.parse.ANTLRParser.labeledAlt_return;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

public class DirectionTransformer {
    
    /***
     * Pulls the facing direction from the given BlockState and returns it.
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
            return state.getValue(SimpleOrientedBlock.ORIENTATION).getCardinal();

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
            return state.getValue(CombinedOrientedBlock.ORIENTATION).getLocalForward();

        if(block instanceof SimpleOrientedBlock) 
            return toDirection(state.getValue(SimpleOrientedBlock.ORIENTATION).getOrient());

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

        return null;
    }

    /***
     * Gets the rotation in degrees of the given Direction.
     * Used for BlockState generation.
     * @return
     */
    public static int getRotation(Direction dir) {
        if(dir == Direction.UP) return 0;
        if(dir == Direction.DOWN) return 180;
        return (int) dir.toYRot();
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
        return getUp(state) == getForward(state);
    } 

    /***
     * HorizontalDirectionalBLocks are in a bit of an odd spot,
     * where they do have a known independent local up, but
     * this distinction is not necessary to make. <p> See 
     * {@link #isAmbiguous(BlockState) isAmbiguous()} for more
     * context.
     * @param state
     * @return
     */
    public static boolean isDistinctionRequired(BlockState state) {
        return state.getBlock() instanceof HorizontalDirectionalBlock 
            ? false : isAmbiguous(state);
    }

    public static boolean isHorizontal(BlockState state) {
        Direction up = getUp(state);
        return up.getAxis().isHorizontal();
    }

    public static Direction toDirection(Axis ax) {
        if(ax == Axis.Y) return Direction.UP;
        if(ax == Axis.X) return Direction.EAST;
        return Direction.NORTH;
    }
}
