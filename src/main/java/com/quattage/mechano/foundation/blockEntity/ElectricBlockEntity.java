package com.quattage.mechano.foundation.blockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ElectricBlockEntity extends SimpleBlockEntity {

    public ElectricBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onRefresh(LevelReader world, BlockPos pos, BlockState oldState, BlockState newState) {

    }   

    @Override
    public void onBlockBroken(Level world, BlockPos pos, BlockState oldState, BlockState newState) {
        
    }

    @Override
    public void tick() {
        
    }

    @Override
    protected void saveTo(CompoundTag tag, Provider registries) {
        
    }

    @Override
    protected void loadFrom(CompoundTag tag, Provider registries) {
        
    }
}
