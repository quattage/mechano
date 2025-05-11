package com.quattage.mechano.foundation.api.transmission;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.landmarks.GridLink;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class PerfectInsulator extends Transmitter {

    public PerfectInsulator(byte registryIndex) {
        super(registryIndex);
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
    public boolean onConnectionCreated(Level world, @Nullable Player creator, GridLink connection) {
        return true;
    }

    @Override
    public void onConnectionDestroyed(Level world, @Nullable Player destroyer, GridLink connection) {
        return;
    }
}
