package com.quattage.mechano.content.block.RollingWheel;

import com.mrh0.createaddition.shapes.CAShapes;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.registry.MechanoBlockEntities;
import com.quattage.mechano.registry.MechanoBlocks;
import com.simibubi.create.content.contraptions.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.ITE;

import io.github.fabricators_of_create.porting_lib.block.CustomRunningEffectsBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class RollingWheelControllerBlock extends RotatedPillarKineticBlock implements ITE<RollingWheelControllerBlockEntity>, CustomRunningEffectsBlock {

    public static final DirectionProperty FACING = Properties.FACING;

    public RollingWheelControllerBlock(Settings properties) {
        super(properties);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.DOWN));
    }

    @SuppressWarnings("deprecation") // TODO investigate
    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos,
            boolean notify) {
        checkForDummy(pos, world, state);
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
    }

    @Override
    public void onPlaced(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        checkForDummy(pos, worldIn, state);
        super.onPlaced(worldIn, pos, state, placer, stack);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.get(AXIS);
    }
    
    @Override
	public VoxelShape getCollisionShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
		return CAShapes.shape(0, 0, 0, 16, 9, 16).build();
	}

    @Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        return new ItemStack(MechanoBlocks.ROLLING_WHEEL.get());
    }

    @Override
    public Class<RollingWheelControllerBlockEntity> getTileEntityClass() {
        return RollingWheelControllerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RollingWheelControllerBlockEntity> getTileEntityType() {
        return MechanoBlockEntities.ROLLING_WHEEL_CONTROLLER.get();
    }



    @Override
	public float getParticleTargetRadius() {
		return 1.5f;
	}

	@Override
	public float getParticleInitialRadius() {
		return 1f;
	}

    @Override
	public boolean hasShaftTowards(WorldView world, BlockPos pos, BlockState state, Direction dir) {
        return isShafted(dir, state);
	}

    public boolean isShafted(Direction dir, BlockState state) {
        return dir.getAxis() == state.get(AXIS);
    }

    public void checkForDummy(BlockPos pos, World world, BlockState state) {
        BlockPos above = pos.offset(Direction.UP);
        if(world.getBlockState(above).getBlock() != MechanoBlocks.ROLLING_WHEEL.get()) {
            world.setBlockState(pos, MechanoBlocks.ROLLING_WHEEL.get().getDefaultState().with(RollingWheelBlock.AXIS, state.get(AXIS)));
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING);
    }

    @Override
    public boolean addRunningEffects(BlockState arg0, World arg1, BlockPos arg2, Entity arg3) {
        return true;
    }
}
