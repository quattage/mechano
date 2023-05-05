package com.quattage.mechano.content.block.RollingWheel;

import com.mrh0.createaddition.shapes.CAShapes;
import com.quattage.mechano.registry.MechanoBlockEntities;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.contraptions.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.ITE;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class RollingWheelBlock extends RotatedPillarKineticBlock implements ITE<RollingWheelTileEntity> {

    public RollingWheelBlock(Settings properties) {
        super(properties);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.get(AXIS);
    }

    @Override
	public VoxelShape getCollisionShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
		return CAShapes.shape(0, 0, 0, 16, 16, 16).build();
	}

    @Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.ENTITYBLOCK_ANIMATED;
	}

    @Override
    public Class<RollingWheelTileEntity> getTileEntityClass() {
        return RollingWheelTileEntity.class;
    }

    @Override
    public BlockEntityType<? extends RollingWheelTileEntity> getTileEntityType() {
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

    // holy moly
    public void updateControllers(BlockState state, World world, BlockPos pos, Direction dir) {
        if (isShafted(dir, state))
			return;
		if (world == null)
			return;

        // i do not brain hard enough to continue
        
    }
}
