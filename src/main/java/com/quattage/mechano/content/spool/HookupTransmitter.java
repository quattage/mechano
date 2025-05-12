    package com.quattage.mechano.content.spool;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.landmarks.GridLink;
import com.quattage.mechano.foundation.api.transmission.Transmitter;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class HookupTransmitter extends Transmitter {

    public HookupTransmitter(byte registryIndex) {
        super(registryIndex);
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
	public boolean onConnectionCreated(Level world, @Nullable Player creator, GridLink connection) {
        return true;
    }

	@Override
	public void onConnectionDestroyed(Level world, @Nullable Player destroyer, GridLink connection) {
        return;
    }
}
