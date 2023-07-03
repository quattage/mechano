package com.quattage.mechano.core.block;

import com.quattage.mechano.core.block.orientation.SimpleOrientation;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class SimpleOrientedBlock extends Block implements IWrenchable {

    public static final EnumProperty<SimpleOrientation> ORIENTATION = EnumProperty.create("orientation", SimpleOrientation.class); //accomodates for up and down PER CARDINAL, ex. UP_NORTH, or DOWN_EAST
    
    public SimpleOrientedBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(ORIENTATION, SimpleOrientation.UP_X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ORIENTATION);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level world = context.getLevel();
		SimpleOrientation rotatedOrient = SimpleOrientation.cycle(state.getValue(ORIENTATION));
        BlockState rotated = state.setValue(ORIENTATION, rotatedOrient);

        if (!rotated.canSurvive(world, context.getClickedPos()))
			return InteractionResult.PASS;

        KineticBlockEntity.switchToBlockState(world, context.getClickedPos(), updateAfterWrenched(rotated, context));

        if (world.getBlockState(context.getClickedPos()) != state)
			playRotateSound(world, context.getClickedPos());

		return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction orientation = context.getClickedFace();
        Direction followingDir = getTriQuadrant(context, orientation, true);

        if(orientation == followingDir) followingDir = context.getHorizontalDirection();
        if(orientation.getAxis() == followingDir.getAxis()) followingDir = followingDir.getClockWise();
        if(context.getPlayer().isCrouching()) orientation = orientation.getOpposite();

        return this.defaultBlockState().setValue(ORIENTATION, SimpleOrientation.combine(orientation, followingDir.getAxis()));
    }

    public static Direction getTriQuadrant(BlockPlaceContext context, Direction orient, boolean negateCenter) {
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
}
