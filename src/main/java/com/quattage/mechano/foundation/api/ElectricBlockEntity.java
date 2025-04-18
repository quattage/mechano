package com.quattage.mechano.foundation.api;

import com.quattage.mechano.foundation.SimpleBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ElectricBlockEntity extends SimpleBlockEntity{

    public ElectricBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void saveTo(CompoundTag tag, Provider registries) {
    
    }

    @Override
    protected void loadFrom(CompoundTag tag, Provider registries) {
        
    }

    @Override
    protected void onFirstTick() {
        
    }
    
}
