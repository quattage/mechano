package com.quattage.mechano.foundation.api.transmitter;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.landmark.GridLink;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class PerfectInsulator extends Transmitter<PerfectInsulator> {

    public PerfectInsulator() {

    }

    @Override
    public TransmitterType<PerfectInsulator> getType() {
        return MechanoTransmissionTypes.PERFECT_INSULATOR;
    }

    @Override
    public int getCost() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean needsSerialization() {
        return false;
    }

    @Override
    public void onConnectionCreated(Level world, GridLink connection) {

    }

    @Override
    public void onConnectionDestroyed(Level world, @Nullable Entity destroyer, GridLink connection) {
        // TODO Auto-generated method stub
        
    }
}
