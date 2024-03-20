package com.quattage.mechano.foundation.electricity.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

/***
 * A generic implementation of <code>EnergyStorage</code> provided by Forge.
 */
public class LocalEnergyStorage<T extends DirectionalEnergyStorable> extends EnergyStorage {

    private T parent;

    /***
     * Creates a new LocalEnergyStorage. Provides extensible EnergyStorage features from
     * the ForgeEnergy API.
     * @param parent Parent object (handles reads/writes and updates).
     * @param capacity Capacity in FE.
     * @param maxReceive Maximum input in FE/t.
     * @param maxExtract Maximum output in FE/t.
     * @param energy Stored energy to initialize with.
     */
    public LocalEnergyStorage(T parent, int capacity, int maxReceive, 
        int maxExtract, int energy) {
            super(capacity, maxReceive, maxExtract, energy);
            this.parent = parent;
    }
    
    @Override
    public boolean canExtract() {
        return maxExtract > 0;
    }
    
    @Override
    public boolean canReceive() {
        return maxReceive > 0;
    }

    public CompoundTag writeTo(CompoundTag nbt) {
        nbt.putInt("stored", energy);
        return nbt;
    }
    
    public void readFrom(CompoundTag nbt) {
        setEnergyStored(nbt.getInt("stored"));
    }

    public int getRemainingStorage() {
        return Math.max(getMaxEnergyStored() - getEnergyStored(), 0);
    }
    
    /***
     * Manually sets the internally stored energy of this EnergyStorage
     * @param energy Energy to add
     * onEnergyUpdated() if stored energy is changed.
     */
    public int setEnergyStored(int energy) {
        return setEnergyStored(energy, false);
    }

    /***
     * Manually sets the internally stored energy of this EnergyStorage
    * @param energy Energy to add
    * @param update Optional (defaults to false) - Whether or not to call
    onEnergyUpdated() if stored energy is changed.
     */
    public int setEnergyStored(int energy, boolean update) {
        if(this.energy != energy) {
            this.energy = energy;
            if(update) onEnergyUpdated();
        }
        return this.energy;
    }
    
    /***
     * Manually overrides the maximum capacity of this EnergyStorage.
     * @param capacity
     */
    public void setMaxCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int energyExtracted = super.extractEnergy(maxExtract, simulate);
        if(energyExtracted != 0) onEnergyUpdated();
        return energyExtracted;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int energyReceived = super.receiveEnergy(maxReceive, simulate);
        if(energyReceived != 0) onEnergyUpdated();
        return energyReceived;
    }

    /***
     * Called automatically whenever stored energy is changed.
     */
    public void onEnergyUpdated() {
        parent.onEnergyUpdated();
    }
    
    /***
     * Sends energy to the neighboring block in the given direction.
     * @param world World to operate within.
     * @param pos Position of the parent block.
     * @param side Side of the block to output energy towards.
     * @param maxExtract Maximum fe/t to extract.
     */
    public void outputTo(Level world, BlockPos pos, Direction side, int maxExtract) {
        BlockEntity sendParent = world.getBlockEntity(pos.relative(side));
		if(sendParent == null) return;
		LazyOptional<IEnergyStorage> optStorage = 
            sendParent.getCapability(ForgeCapabilities.ENERGY, side.getOpposite());

		IEnergyStorage ies = optStorage.orElse(null);
		if(ies == null)
			return;

		int extracted = this.extractEnergy(maxExtract, false);
		this.receiveEnergy(extracted - ies.receiveEnergy(extracted, false), false);
    }
    
    @Override
    public String toString() {
        return "Energy: [" 
            + getEnergyStored() + " / " 
            + getMaxEnergyStored() + ", " 
            + "Max input: " + maxReceive 
            + " Max output: " + maxExtract
            + "]";
    }
}
