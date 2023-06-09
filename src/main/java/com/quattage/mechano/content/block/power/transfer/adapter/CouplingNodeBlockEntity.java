package com.quattage.mechano.content.block.power.transfer.adapter;

import com.mrh0.createaddition.energy.BaseElectricTileEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CouplingNodeBlockEntity extends BaseElectricTileEntity {
    public static final int MAX_IN = 2048, MAX_OUT = 2048, CAPACITY = 4096;

    public CouplingNodeBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state, CAPACITY, MAX_IN, MAX_OUT);
    }

    @Override
    public boolean isEnergyInput(Direction arg0) {
        return false;
    }

    @Override
    public boolean isEnergyOutput(Direction arg0) {
        return false;
    }
}
