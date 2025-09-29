package com.quattage.mechano.foundation.api.transmitter;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.landmark.GridLink;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class PerfectConductor extends Transmitter<PerfectConductor> {

    public PerfectConductor() {
    
    }

    @Override
    public TransmitterType<PerfectConductor> getType() {
        return MechanoTransmissionTypes.PERFECT_CONDUCTOR;
    }

    @Override
    public int getCost() {
        return 0;
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
        
    }
}
