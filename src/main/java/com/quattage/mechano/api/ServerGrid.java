package com.quattage.mechano.api;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.grid.solver.NodalSolver;
import com.quattage.mechano.api.grid.solver.StabilizedBiconjucateSolver;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.foundation.tracking.GridUUID;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;

public final class ServerGrid extends SidedGridDispatcher {

    private final NodalSolver solver = new StabilizedBiconjucateSolver();
    private final Object2ObjectOpenHashMap<GridUUID, CircuitComponent> graph = new Object2ObjectOpenHashMap<>();

    public static ServerGrid loadFrom() {
        return null;
    }

    protected ServerGrid(Level world) {
        super(world);
    }

    @Override
    protected @Nullable ListTag writeAll() {
        return null;
    }

    @Override protected String getDistPrefix() { return "SERVER"; }

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
