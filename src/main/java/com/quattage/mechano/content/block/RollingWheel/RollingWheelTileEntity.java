package com.quattage.mechano.content.block.RollingWheel;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.foundation.utility.Iterate;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

public class RollingWheelTileEntity extends KineticTileEntity {

    public RollingWheelTileEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        setLazyTickRate(20);
    }
    
    private void syncControllers() {
        for (Direction dir : Iterate.directions) {
            ((RollingWheelBlock) getCachedState().getBlock()).updateControllers(getCachedState(), getWorld(), getPos(), dir);
        }
    }

    @Override
	public void onSpeedChanged(float prevSpeed) {
		super.onSpeedChanged(prevSpeed);
		syncControllers();
	}

    @Override
	public void lazyTick() {
		super.lazyTick();
		syncControllers();
	}

    @Override
	protected Box createRenderBoundingBox() {
		return new Box(pos).expand(1);
	}
}
