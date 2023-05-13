package com.quattage.mechano.content.block.RollingWheel;

import com.mrh0.createaddition.shapes.CAShapes;
import com.quattage.mechano.registry.MechanoBlockEntities;
import com.quattage.mechano.registry.MechanoBlocks;
import com.simibubi.create.content.contraptions.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.ITE;

import io.github.fabricators_of_create.porting_lib.event.common.LivingEntityEvents.Tick;
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

public class RollingWheelBlock extends RotatedPillarKineticBlock implements ITE<RollingWheelBlockEntity> {

    private boolean isControlled = false;

    public RollingWheelBlock(Settings properties) {
        super(properties);
    }

    @SuppressWarnings("deprecation") // TODO investigate
    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos,
            boolean notify) {
        checkForController(pos, world, state);
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
    }

    @Override
    public void onPlaced(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        checkForController(pos, worldIn, state);
        super.onPlaced(worldIn, pos, state, placer, stack);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.get(AXIS);
    }

    @Override
	public VoxelShape getCollisionShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        if(isControlled)    // this doesn't actually work but it might not be necesary 
            return CAShapes.shape(0, 5, 0, 16, 16, 16).build();
        return CAShapes.shape(0, 0, 0, 16, 16, 16).build();
        
	}

    @Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.ENTITYBLOCK_ANIMATED;
	}

    @Override
    public Class<RollingWheelBlockEntity> getTileEntityClass() {
        return RollingWheelBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RollingWheelBlockEntity> getTileEntityType() {
        return MechanoBlockEntities.ROLLING_WHEEL.get();
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

    public void checkForController(BlockPos pos, World world, BlockState state) {
        BlockPos below = pos.offset(Direction.DOWN);
        if(world.getBlockState(below).getBlock() == MechanoBlocks.ROLLING_WHEEL.get()) {
            world.setBlockState(below, MechanoBlocks.ROLLING_WHEEL_CONTROLLER.get().getDefaultState().with(RollingWheelControllerBlock.AXIS, state.get(AXIS)));
            isControlled = true;
        } else {
            isControlled = false;
        }
    }

    public void updateControllers(BlockState state, World world, BlockPos pos, Direction dir) {
        this.checkForController(pos, world, state);        
        if (isShafted(dir, state))
			return;
		if (world == null)
			return;
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return state.get(AXIS) != Axis.Y;           // makes sure the wheel can't face up
    }                                               // idk how to remove this capability properly so deal with it
}
