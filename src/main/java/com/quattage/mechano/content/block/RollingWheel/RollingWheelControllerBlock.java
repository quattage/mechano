package com.quattage.mechano.content.block.RollingWheel;

import com.mrh0.createaddition.shapes.CAShapes;
import com.quattage.mechano.registry.MechanoBlockEntities;
import com.quattage.mechano.registry.MechanoBlocks;
import com.simibubi.create.content.contraptions.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.ITE;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class RollingWheelControllerBlock extends RotatedPillarKineticBlock implements ITE<RollingWheelControllerBlockEntity> {

    public RollingWheelControllerBlock(Settings properties) {
        super(properties);
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

    // holy moly
    public void updateControllers(BlockState state, World world, BlockPos pos, Direction dir) {
        checkForDummy(pos, world, state);
        if (isShafted(dir, state))
			return;
		if (world == null)
			return;

        // i do not brain hard enough to continue
        
    }
}
