package com.quattage.mechano.content.connector;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.blockEntity.GriddableBlockEntity;
import com.quattage.mechano.foundation.api.blockEntity.SimpleBlockEntity;
import com.quattage.mechano.foundation.api.blockEntity.SimpleBlockEntity.BERefreshable;
import com.quattage.mechano.foundation.block.CombinedOrientedBlock;
import com.quattage.mechano.foundation.block.ConnectorHostOverridable;
import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BlockWithConnections<T extends GriddableBlockEntity> extends CombinedOrientedBlock implements ConnectorHostOverridable, BERefreshable<T> {

    protected static final VoxelShape ROOT_X = Block.box(0, 7, 7, 10, 9, 9);
    protected static final VoxelShape ROOT_Y = Block.box(7, 7, 0, 9, 9, 10);

    public BlockWithConnections(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {

        Level world = context.getLevel();
        CombinedOrientation orient = CombinedOrientation.cycleLocalForward(state.getValue(ORIENTATION));
        BlockPos pos = context.getClickedPos();
        BlockState rotated = state.setValue(ORIENTATION, orient);

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
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        Direction facing = state.getValue(ORIENTATION).getLocalUp().getOpposite();
        BlockPos underPos = pos.relative(facing);
        if(!underPos.equals(neighborPos)) return;
        BlockState underState = world.getBlockState(underPos);
        if(underState.getBlock() instanceof ConnectorHostOverridable cho && 
            !cho.isConnectorAllowed(world, pos, state, underPos, underState)) {
                world.destroyBlock(pos, true);
                return;
        }
        if(isSupported(world, underPos, underState, facing, ROOT_X) 
            || isSupported(world, underPos, underState, facing, ROOT_Y))
                return;
        world.destroyBlock(pos, true);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        Direction facing = state.getValue(ORIENTATION).getLocalUp().getOpposite();
        BlockPos underPos = pos.relative(facing);
        BlockState underState = world.getBlockState(underPos);
        if(underState.getBlock() instanceof ConnectorHostOverridable cho)
            return cho.isConnectorAllowed(world, pos, state, underPos, underState);
        return isSupported(world, underPos, underState, facing, ROOT_X) 
            || isSupported(world, underPos, underState, facing, ROOT_Y);
    }

    private boolean isSupported(LevelReader world, BlockPos relative, BlockState underState, Direction facing, VoxelShape root) {
        return !Shapes.joinIsNotEmpty(underState.getBlockSupportShape(world, relative).getFaceShape(facing), root, BooleanOp.ONLY_SECOND);
    }

    @Override
    public boolean isConnectorAllowed(LevelReader world, BlockPos connectorPos, BlockState connectorState,
            BlockPos thisPos, BlockState thisState) {
        return false;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        CombinedOrientation orient = state.getValue(ORIENTATION);
        return state.setValue(ORIENTATION, orient.applyRotation(rotation));
    }

    @Override
    public @Nullable PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.NORMAL;
    }

    @Override
    public void onBlockStateChange(LevelReader world, BlockPos pos, BlockState oldState, BlockState newState) {
        super.onBlockStateChange(world, pos, oldState, newState);
        refreshBE(oldState, world, pos, newState);
    }

    @Override
    protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean movedByPiston) {
        breakBE(state, world, pos, newState);
        super.onRemove(state, world, pos, newState, movedByPiston);
    }
}
