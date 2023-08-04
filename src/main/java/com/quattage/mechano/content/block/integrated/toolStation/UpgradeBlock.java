package com.quattage.mechano.content.block.integrated.toolStation;

import java.util.Locale;

import com.quattage.mechano.MechanoBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;



public class UpgradeBlock extends Block {
    public static final EnumProperty<UpgradeBlockModelType> MODEL_TYPE = EnumProperty.create("model", UpgradeBlockModelType.class);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public enum UpgradeBlockModelType implements StringRepresentable {
        STANDALONE, CONNECTED;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);

        }

        @Override
        public String toString() {
            return getSerializedName();
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(level, pos, state, placer, itemStack);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MODEL_TYPE);
    }

    public UpgradeBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(MODEL_TYPE, UpgradeBlockModelType.STANDALONE));
    }

    @SuppressWarnings("deprecation") // TODO investigate
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborChanged(state, level, pos, sourceBlock, sourcePos, notify);
        if(!level.isClientSide) {
            BlockState adjacentBlockState = level.getBlockState(pos.relative(state.getValue(FACING).getClockWise()));
            if(!adjacentBlockState.getBlock().equals(MechanoBlocks.TOOL_STATION.get())) {
                level.destroyBlock(pos, true);
            }
        }
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos possy = pos.relative(state.getValue(FACING).getClockWise());
        BlockState targetBlockState = level.getBlockState(possy);
        Block targetBlock = targetBlockState.getBlock();
        if(targetBlock == MechanoBlocks.TOOL_STATION.get()) {
            if (targetBlockState.getValue(FACING) == state.getValue(FACING)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }
}