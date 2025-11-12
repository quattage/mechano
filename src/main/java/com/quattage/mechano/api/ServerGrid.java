package com.quattage.mechano.api;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.solver.NodalSolver;
import com.quattage.mechano.api.grid.solver.NodeUnionSet;
import com.quattage.mechano.api.grid.solver.StabilizedBiconjucateSolver;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.foundation.tracking.GridUUID;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;

public final class ServerGrid extends SidedGridDispatcher {

    private final NodalSolver solver = new StabilizedBiconjucateSolver();
    private final Object2ObjectOpenHashMap<GridUUID, CircuitComponent> graph = new Object2ObjectOpenHashMap<>();
    private final NodeUnionSet unionizer = new NodeUnionSet();

    public static ServerGrid loadFrom(Level world) {
        return new ServerGrid(world);
        // TODO impl
    }

    protected ServerGrid(Level world) {
        super(world);
    }

    @Override
    protected @Nullable ListTag writeAll() {
        return null;
    }

    public void addParticipant(Griddable source) {
        Objects.requireNonNull(source);
        GridUUID addr = source.getAddress();
        if(addr == null) throw new NullPointerException("Error adding participant to " + this 
            + " - The provided griddable '" + source.getClass().getSimpleName() + "' couldn't provide a valid GridUUID!");
        CircuitComponent component = graph.get(addr);
        if(component != null) {
            component = graph.remove(addr);
            component.forEachNode(node -> unionizer.remove(node));
        }
        component = source.getCircuit();
        if(component == null || !component.isSignificant()) 
            return;
        component.forEachNode(node -> unionizer.add(node));
    }

    public void removeParticipant(Griddable source) {
        Objects.requireNonNull(source);
        GridUUID addr = source.getAddress();
        if(addr == null) throw new NullPointerException("Error removing participant from " + this 
            + " - The provided griddable '" + source.getClass().getSimpleName() + "' couldn't provide a valid GridUUID!");
        CircuitComponent component = graph.remove(addr);
        component.forEachNode(node -> unionizer.remove(node));
    }

    @Override
    protected void onLoad() {
        
    }

    @Override
    protected void onUnload() {
        unionizer.reset();
        solver.reset();
    }


    @Override
    protected void tick() {
        
    }

    @Override protected String getDistPrefix() { 
        return "SERVER"; 
    }

    @Override
    public String toString() {
        return "ServerGrid[" + getDimensionName() + "]";
    }
}
