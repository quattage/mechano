    package com.quattage.mechano.content.spool;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.landmark.GridLink;
import com.quattage.mechano.foundation.api.transmitter.MechanoTransmissionTypes;
import com.quattage.mechano.foundation.api.transmitter.Transmitter;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class HookupTransmitter extends Transmitter<HookupTransmitter> {

    public HookupTransmitter() {
    
    }

    @Override
    public TransmitterType<HookupTransmitter> getType() {
        return MechanoTransmissionTypes.HOOKUP;
    }

    @Override
    public int getCost() {
        return 1;
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
