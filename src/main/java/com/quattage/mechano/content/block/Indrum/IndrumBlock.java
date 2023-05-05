package com.quattage.mechano.content.block.Indrum;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Direction;
import net.minecraft.state.StateManager.Builder;

public class IndrumBlock extends Block {
    public static final DirectionProperty FACING = Properties.FACING;

    public IndrumBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

    @Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		return this.getDefaultState().with(FACING, context.getSide().getOpposite());
	}

    @Override
	public BlockState rotate(BlockState state, BlockRotation direction) {
		return state.with(FACING, direction.rotate(state.get(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.with(FACING, mirror.apply(state.get(FACING)));
	}
}
