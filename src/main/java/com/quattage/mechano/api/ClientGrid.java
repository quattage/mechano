package com.quattage.mechano.api;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;

public final class ClientGrid extends SidedGridDispatcher {

    protected ClientGrid(Level world) {
        super(world);
    }

    @Override protected @Nullable ListTag writeAll() { return null; }
    @Override protected String getDistPrefix() { return "CLIENT"; }

    @Override
    protected void onLoad() {

    }

    @Override
    protected void onUnload() {

    }

    @Override
    protected void tick() {
    }
    
}
