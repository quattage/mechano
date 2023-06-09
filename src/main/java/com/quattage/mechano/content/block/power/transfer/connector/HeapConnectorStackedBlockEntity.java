package com.quattage.mechano.content.block.power.transfer.connector;

import com.mrh0.createaddition.energy.BaseElectricTileEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class HeapConnectorStackedBlockEntity extends BaseElectricTileEntity {
    public static final int MAX_TRANSFER = 10240;
    public static final int CAPACITY = MAX_TRANSFER * 2;

    public HeapConnectorStackedBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state, CAPACITY, MAX_TRANSFER, MAX_TRANSFER);
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
