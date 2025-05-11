package com.quattage.mechano.foundation.api;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.ListTag;

public class GlobalClientGrid extends SidedGridDispatcher {

    public static GlobalClientGrid loadFrom(ListTag serializedGlobals, ClientLevel world) {
        return new GlobalClientGrid(world);
    }

    protected GlobalClientGrid(ClientLevel world) {
        super(world);
    }

    @Override
    public ClientLevel getWorld() {
        return (ClientLevel)super.getWorld();
    }

    @Override
    protected ListTag writeAll() {
        return new ListTag();
    }

    @Override
    protected String getDistPrefix() {
        return "CLIENT";
    }

    @Override
    public String toString() {
        return "GlobalClientGrid(" + getDimensionName() + ", 0 members)";
    }
}
