package com.quattage.mechano.foundation.block;

import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.blockEntity.SimpleBlockEntity;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class CombinedOrientedBlock extends Block implements IWrenchable {

    public static final EnumProperty<CombinedOrientation> ORIENTATION = EnumProperty.create("orientation", CombinedOrientation.class);
    
    public CombinedOrientedBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(ORIENTATION, CombinedOrientation.UP_WEST));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ORIENTATION);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {

        Level world = context.getLevel();
        Axis intendedRotation = context.getClickedFace().getAxis();
        CombinedOrientation strictCD;
        BlockPos pos = context.getClickedPos();

        if(intendedRotation == state.getValue(ORIENTATION).getLocalUp().getAxis())
            strictCD = CombinedOrientation.cycleLocalForward(state.getValue(ORIENTATION));
        else strictCD = CombinedOrientation.cycle(state.getValue(ORIENTATION));

        BlockState rotated = state.setValue(ORIENTATION, strictCD);
        if(!rotated.canSurvive(world, pos))
			return InteractionResult.PASS;
        world.setBlock(pos, updateAfterWrenched(rotated, context), 3);

        BlockState postState = world.getBlockState(pos);
        if(postState != state) {
            BlockEntity be = world.getBlockEntity(pos);
            if(be instanceof SimpleBlockEntity sbe)
                sbe.onRefresh(world, pos, state, postState);
            IWrenchable.playRotateSound(world, pos);           
        }

		return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction orientation = context.getClickedFace();
        Direction followingDir = CombinedOrientation.getClickedQuadrant(context, orientation, true);

        if(orientation == followingDir) followingDir = context.getHorizontalDirection();
        if(orientation.getAxis() == followingDir.getAxis()) followingDir = followingDir.getClockWise();
        if(context.getPlayer().isCrouching()) orientation = orientation.getOpposite();

        return this.defaultBlockState().setValue(ORIENTATION, CombinedOrientation.combine(orientation, followingDir));
    }
}
