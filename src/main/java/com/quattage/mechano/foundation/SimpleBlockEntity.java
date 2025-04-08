package com.quattage.mechano.foundation;

import com.simibubi.create.api.schematic.nbt.PartialSafeNBT;
import com.simibubi.create.foundation.blockEntity.CachedRenderBBBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class SimpleBlockEntity extends CachedRenderBBBlockEntity implements PartialSafeNBT {

    private int stateIndex = 0;
    private boolean hasInit = false;

    public SimpleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

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
        tag.putInt("fs", stateIndex);
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
        stateIndex = tag.getInt("fs");
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
}
