package com.quattage.mechano.content.block.power;

import net.minecraftforge.energy.EnergyStorage;

public class MechanoEnergyStorage extends EnergyStorage {
    private boolean allowEnergyExtraction = true;
    private boolean allowEnergyInsertion = true;
    public MechanoEnergyStorage(int capacity) {
        super(capacity);
    }

    public MechanoEnergyStorage(int capacity, int maxTransfer) {
        super(capacity, maxTransfer);
    }

    public MechanoEnergyStorage(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }

    public MechanoEnergyStorage(int capacity, int maxReceive, int maxExtract, int energy) {
        super(capacity, maxReceive, maxExtract, energy);
    }

    public void allowInsertion(){
        allowEnergyInsertion = true;
    }

    public void forbidInsertion(){
        allowEnergyInsertion = false;
    }

    public void allowExtraction(){
        allowEnergyExtraction = true;
    }

    public void forbidExtraction(){
        allowEnergyExtraction = false;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return allowEnergyExtraction ? super.extractEnergy(maxExtract, simulate) : 0;
    }

    @Override
    public boolean canExtract() {
        return super.canExtract();
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return allowEnergyInsertion ? super.receiveEnergy(maxReceive, simulate) : 0;
    }


}
