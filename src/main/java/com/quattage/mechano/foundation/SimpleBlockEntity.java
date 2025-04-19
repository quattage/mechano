package com.quattage.mechano.foundation;

import com.simibubi.create.api.schematic.nbt.PartialSafeNBT;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.CachedRenderBBBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class SimpleBlockEntity extends CachedRenderBBBlockEntity implements PartialSafeNBT {

    private boolean hasInit = false;

    public SimpleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract void onBlockPlaced(Level world, BlockPos pos, BlockState oldState, BlockState newState);
    public abstract void onBlockBroken(Level world, BlockPos pos, BlockState oldState, BlockState newState);
    public abstract void onBlockStateChanged(LevelReader world, BlockPos pos, BlockState oldState, BlockState newState);

    /**
     * Save up-to-date information and variables pertaining to 
     * this BlockEntity so that they may persist.
     * @param tag CompoundTag to save data to
     * @param registries
     */
    protected abstract void saveTo(CompoundTag tag, HolderLookup.Provider registries);

    @Override
    protected void saveAdditional(CompoundTag tag, Provider registries) {
        super.saveAdditional(tag, registries);
        saveTo(tag, registries);
    }

    @Override
    public void writeSafe(CompoundTag compound, Provider registries) {
        super.saveAdditional(compound, registries);
    }

    @Override
    public CompoundTag writeClient(CompoundTag tag, Provider registries) {
        saveTo(tag, registries);
        return tag;
    }

    /**
     * Update values in this BlockEntity from data contained 
     * within the provided CompoundTag.
     * @param tag CompoundTag with data to load
     * @param registries
     */
    protected abstract void loadFrom(CompoundTag tag, HolderLookup.Provider registries);

    @Override
    protected void loadAdditional(CompoundTag tag, Provider registries) {
        super.loadAdditional(tag, registries);
        loadFrom(tag, registries);
    }

    @Override
    public void readClient(CompoundTag tag, Provider registries) {
        loadFrom(tag, registries);
    }

    /**
     * Initializer method called once after onLoad()
     */
    protected abstract void onFirstTick();

    public void tick() {
		if (!hasInit && hasLevel()) {
			onFirstTick();
			hasInit = true;
        }
    }

    private static class SimpleBlockEntityTicker<T extends BlockEntity> implements BlockEntityTicker<T> {
        @Override
        public void tick(Level level, BlockPos pos, BlockState state, T blockEntity) {
            if(!blockEntity.hasLevel())
                blockEntity.setLevel(level);
            ((SimpleBlockEntity)blockEntity).tick();
        }
    }

    /**
     * An overridden implementation of Create's IBE designed to accomodate the slightly lighter weight
     * SimpleBlockEntity implementation.
     */
    public static interface SBE<B extends SimpleBlockEntity> extends IBE<B> {

        @Override
        default <S extends BlockEntity> BlockEntityTicker<S> getTicker(Level p_153212_, BlockState p_153213_,
                BlockEntityType<S> p_153214_) {
            return new SimpleBlockEntityTicker<>();
        }

        default void onBlockPlaced(BlockState oldState, Level world, BlockPos pos, BlockState newState) {
            if(!newState.hasBlockEntity() || oldState.is(newState.getBlock())) return;
            BlockEntity be = world.getBlockEntity(pos);
            if(be instanceof SimpleBlockEntity sbe)
                sbe.onBlockPlaced(world, pos, oldState, newState);
        }

        default void onBlockBroken(BlockState oldState, Level world, BlockPos pos, BlockState newState) {
            if(!oldState.hasBlockEntity() || oldState.is(newState.getBlock())) return;
            BlockEntity be = world.getBlockEntity(pos);
            if(be instanceof SimpleBlockEntity sbe)
                sbe.onBlockBroken(world, pos, oldState, newState);
            world.removeBlockEntity(pos);
        }

        default void onBlockStateChanged(BlockState oldState, LevelReader world, BlockPos pos, BlockState newState) {
            if(!newState.hasBlockEntity() || oldState.is(newState.getBlock())) return;
            BlockEntity be = world.getBlockEntity(pos);
            if(be instanceof SimpleBlockEntity sbe)
                sbe.onBlockStateChanged(world, pos, oldState, newState);
        }
    }
}
