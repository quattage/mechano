package com.quattage.mechano.foundation.watt.barebones;

import com.quattage.mechano.foundation.SimpleBlockEntity;
import com.quattage.mechano.foundation.watt.Joule;
import com.quattage.mechano.foundation.watt.volt.DoubleSigmoidVoltageCurve;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class WattStorageBlockEntity extends SimpleBlockEntity {

    private long maxEnergy;
    private DoubleSigmoidVoltageCurve voltage = DoubleSigmoidVoltageCurve.DEFAULT;
    private Joule storedEnergy;

    public WattStorageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
