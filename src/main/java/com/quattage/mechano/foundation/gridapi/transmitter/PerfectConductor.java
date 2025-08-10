package com.quattage.mechano.foundation.gridapi.transmitter;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.gridapi.landmark.GridLink;
import com.quattage.mechano.foundation.gridapi.transmitter.TransmitterRegistry.TransmitterType;

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
