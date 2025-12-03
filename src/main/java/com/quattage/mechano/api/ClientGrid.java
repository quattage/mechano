package com.quattage.mechano.api;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.foundation.tracking.GridIdentifiable;

import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;

public final class ClientGrid extends Grid {


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

    @Override
    public void addComponent(Griddable<?>source) {
        
    }

    @Override
    public CircuitComponent popComponent(GridIdentifiable<?> obj) {
        return null;
    }

    @Override
    public int getComponentCount() {
        return 0;
    }
}
