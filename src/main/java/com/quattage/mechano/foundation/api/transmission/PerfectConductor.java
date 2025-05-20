package com.quattage.mechano.foundation.api.transmission;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.landmarks.GridLink;
import com.quattage.mechano.foundation.api.transmission.TransmitterRegistry.TransmitterType;

import net.minecraft.world.entity.player.Player;
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
    public void onConnectionDestroyed(Level world, @Nullable Player destroyer, GridLink connection) {

    }

    
}
