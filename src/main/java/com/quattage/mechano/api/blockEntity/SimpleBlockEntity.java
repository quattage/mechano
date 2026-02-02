package com.quattage.mechano.api.blockEntity;

import org.jetbrains.annotations.Nullable;

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

    private boolean initialized = false;

    public SimpleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * Called by {@link BERefreshable} whenever this BE's cooresponding block is broken.
     * @param world World to operate within (A LevelReader - the world cannot be modified within the scope of this method)
     * @param pos The position of the modified block
     * @param oldState The state that existed before this call was made
     * @param newState The state that exists now
     */
    public abstract void onBlockBroken(Level world, BlockPos pos, @Nullable BlockState oldState, BlockState newState);

    /**
     * Called by {@link BERefreshable} whenever this BE's cooresponding block is placed or updated in any way
     * @param world World to operate within (A LevelReader - the world cannot be modified within the scope of this method)
     * @param pos The position of the modified block
     * @param oldState The state that existed before this call was made
     * @param newState The state that exists now
     */
    public abstract void onRefresh(LevelReader world, BlockPos pos, @Nullable BlockState oldState, BlockState newState);


    /**
     * Called right after this BE is added to the world and 
     * before it ticks for the first time
     */
    public abstract void initialize();

    /**
     * Called continuously every game tick on both sides
     */
    public abstract void tick();

    /**
     * Save up-to-date information and variables pertaining to 
     * this BlockEntity so that they may persist after chunks
     * are unloaded.
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

    private static class SimpleBlockEntityTicker<T extends BlockEntity> implements BlockEntityTicker<T> {
        @Override
        public void tick(Level level, BlockPos pos, BlockState state, T blockEntity) {
            if(!(blockEntity instanceof SimpleBlockEntity sbe)) 
                throw new IllegalArgumentException("shut up");
            if(!sbe.hasLevel()) sbe.setLevel(level);
            if(!sbe.initialized) {
                sbe.initialize();
                sbe.initialized = true;
            }
            sbe.tick();
        }
    }

    /**
     * An overridden implementation of Create's IBE designed to accomodate the slightly lighter weight
     * SimpleBlockEntities that don't need to make use of Create's behaviour system
     */
    public interface BERefreshable<B extends SimpleBlockEntity> extends IBE<B> {

        @Override
        default <S extends BlockEntity> BlockEntityTicker<S> getTicker(Level world, BlockState state, BlockEntityType<S> type) {
            return new SimpleBlockEntityTicker<>();
        }

        /**
         * Call this method whenever this block has changed state in any way that should be carried over to the block entity. 
         * This method is called interanally when the block is wrenched, but your block must call it manually if you want state changes to
         * update any data in your BE. <p> Node - You only get a LevelReader within the scope of this method because the world shouldn't be
         * modified here. This is to prevent neighbour updates from recursively calling this method and stack overflowing.
         * @param oldState The state that existed before this call was made
         * @param world The world to operate within 
         * @param pos The position of the block that was modified
         * @param newState The state that exists at the time of calling this method
         */
        default void refreshBE(@Nullable BlockState oldState, LevelReader world, BlockPos pos, BlockState newState) {
            if(oldState != null && (!newState.hasBlockEntity() || oldState.is(newState.getBlock()))) return;
            BlockEntity be = world.getBlockEntity(pos);
            if(be instanceof SimpleBlockEntity sbe)
                sbe.onRefresh(world, pos, oldState, newState);
        }

        /**
         * Call this method whenever this block should signal that it is about to be broken. 
         * @param oldState The state that exists at the time of calling this method
         * @param world The world to operate within
         * @param pos The position of the block that was modified
         * @param newState The state that will exist after this call (usually air)
         */
        default void breakBE(@Nullable BlockState oldState, Level world, BlockPos pos, BlockState newState) {
            if(oldState == null || !oldState.hasBlockEntity()) return;
            BlockEntity be = world.getBlockEntity(pos);
            if(be instanceof SimpleBlockEntity sbe)
                sbe.onBlockBroken(world, pos, oldState, newState);
        }
    }
}
