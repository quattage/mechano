package com.quattage.mechano.content.test;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.PowerGridBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TestAxisBlockEntity extends PowerGridBlockEntity {

    public TestAxisBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void onFirstTick() {

    }

    @Override
    public void onBlockPlaced(Level world, BlockPos pos, BlockState oldState, BlockState newState) {
        Mechano.LOGGER.info("PLACED");
    }

    @Override
    public void onBlockBroken(Level world, BlockPos pos, BlockState oldState, BlockState newState) {
        Mechano.LOGGER.info("BROKEN");
    }

    @Override
    public void onBlockStateChanged(LevelReader world, BlockPos pos, BlockState oldState, BlockState newState) {
        Mechano.LOGGER.info("CHANGED");
    }

    @Override
    protected void saveTo(CompoundTag tag, Provider registries) {

    }

    @Override
    protected void loadFrom(CompoundTag tag, Provider registries) {

    }
}
