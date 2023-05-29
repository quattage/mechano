package com.quattage.mechano.content.block.Alternator.Collector;

import java.util.Locale;

import com.quattage.mechano.registry.MechanoBlockEntities;
import com.quattage.mechano.registry.MechanoBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class CollectorBlock extends DirectionalKineticBlock implements IBE<CollectorBlockEntity> {

    public static final EnumProperty<CollectorBlockModelType> MODEL_TYPE = EnumProperty.create("model", CollectorBlockModelType.class);

    public CollectorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(defaultBlockState().setValue(MODEL_TYPE, CollectorBlockModelType.BASE));
    }

    public enum CollectorBlockModelType implements StringRepresentable {
        BASE, ROTORED;

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
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction dir) {
		return dir.getAxis() == state.getValue(FACING).getAxis();
	}

    @Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        Direction ax = state.getValue(FACING).getOpposite();
        if(world.getBlockState(pos.relative(ax)).getBlock() == MechanoBlocks.ROTOR.get())
            world.setBlock(pos, state.setValue(MODEL_TYPE, CollectorBlockModelType.ROTORED), Block.UPDATE_ALL);
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        super.neighborChanged(state, world, pos, pBlock, pFromPos, pIsMoving);
        if(state.getValue(MODEL_TYPE) == CollectorBlockModelType.ROTORED) {
            Direction ax = state.getValue(FACING).getOpposite();
            if(world.getBlockState(pos.relative(ax)).getBlock() != MechanoBlocks.ROTOR.get())
                world.destroyBlock(pos, true);
        }
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        Direction ax = state.getValue(FACING);
        if(ax == Direction.UP)
            return false;
        if(ax == Direction.DOWN)
            return false;
        return super.canSurvive(state, world, pos);
    }

    @Override
    public Class<CollectorBlockEntity> getBlockEntityClass() {
        return CollectorBlockEntity.class;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MODEL_TYPE);
    }

    @Override
    public BlockEntityType<? extends CollectorBlockEntity> getBlockEntityType() {
        return MechanoBlockEntities.COLLECTOR.get();
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }    
}
