package com.quattage.mechano.api;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.solver.NodalSolver;
import com.quattage.mechano.api.grid.solver.NodeUnionSet;
import com.quattage.mechano.api.grid.solver.StabilizedBiconjucateSolver;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.grid.topology.ComponentLink;
import com.quattage.mechano.foundation.tracking.GridIdentifiable;
import com.quattage.mechano.foundation.tracking.GridUUID;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;

public final class ServerGrid extends Grid {

    private final NodalSolver solver = new StabilizedBiconjucateSolver();
    private final Object2ObjectOpenHashMap<GridUUID, CircuitComponent> graph = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<GridUUID, List<ComponentLink<?>>> externalLinks = new Object2ObjectOpenHashMap<>();
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

    @Override
    public void addComponent(Griddable<?> source) {
        Objects.requireNonNull(source);
        GridUUID addr = source.getUUIDSafe();
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

    @Override
    public CircuitComponent popComponent(GridIdentifiable<?> obj) {
        Objects.requireNonNull(obj);
        GridUUID addr = obj.getUUIDSafe();
        CircuitComponent component = graph.remove(addr);
        if(component == null) return null;
        component.forEachNode(node -> unionizer.remove(node));
        return component;
    }

    @Override
    public int getLinkCount() {
        return externalLinks.size();
    }

    @Override
    public int getComponentCount() {
        return graph.size();
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
