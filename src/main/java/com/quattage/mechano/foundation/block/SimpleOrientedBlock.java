package com.quattage.mechano.foundation.block;

import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.SimpleOrientation;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.Direction;
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

        if(!rotated.canSurvive(world, context.getClickedPos()))
			return InteractionResult.PASS;

        context.getLevel().setBlockAndUpdate(context.getClickedPos(),  
            Block.updateFromNeighbourShapes(rotated, context.getLevel(), 
                context.getClickedPos()));

        if(world.getBlockState(context.getClickedPos()) != state)
			IWrenchable.playRotateSound(world, context.getClickedPos());

		return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction orientation = context.getClickedFace();
        Direction followingDir = CombinedOrientation.getClickedQuadrant(context, orientation, true);

        if(orientation == followingDir) followingDir = context.getHorizontalDirection();
        if(orientation.getAxis() == followingDir.getAxis()) followingDir = followingDir.getClockWise();
        if(context.getPlayer().isCrouching()) orientation = orientation.getOpposite();

        return this.defaultBlockState().setValue(ORIENTATION, 
            SimpleOrientation.combine(orientation, followingDir.getAxis()));
    }
}