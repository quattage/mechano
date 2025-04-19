package com.quattage.mechano.foundation.api;

import com.quattage.mechano.foundation.api.grid.TransferProtocolRepresentable;
import com.quattage.mechano.foundation.api.grid.landmarks.GridLink;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class WireSpoolItem extends Item implements TransferProtocolRepresentable {

    public WireSpoolItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onConnectionCreated(Level world, Player creator, GridLink connection) {
        return true;
    }

    @Override
    public boolean onConnectionCreatedAnonymous(Level world, GridLink connection) {
        return true;
    }

    @Override
    public boolean onConnectionDestroyed(Level world, Player destroyer, GridLink connection) {
        return true;
    }

    @Override
    public boolean onConnectionDestroyedAnonymous(Level world, GridLink connection) {
        return true;
    }

    @Override
    public Item get() {
        return this;
    }
}
