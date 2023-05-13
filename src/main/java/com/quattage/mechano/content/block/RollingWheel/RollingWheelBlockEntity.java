package com.quattage.mechano.content.block.RollingWheel;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.foundation.utility.Iterate;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

public class RollingWheelBlockEntity extends KineticTileEntity {

    public RollingWheelBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        setLazyTickRate(20);
    }

    @Override
	protected Box createRenderBoundingBox() {
		return new Box(pos).expand(1);
	}
}
