package com.quattage.mechano.api.grid.solver;

import java.util.Set;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.component.StampingComponent;
import com.quattage.mechano.api.grid.component.StampingComponent.StampsDynamically;
import com.quattage.mechano.api.grid.topology.Circuit;
import com.quattage.mechano.api.grid.topology.vertex.Node;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

/**
 * Tracks all active stampers as well as any auxillary variables during multi-nodal 
 * analysis (MNA) as {@link StampingComponent} instances allocate their own space in 
 * the B vector.
 */
public class MNAIndexer {
    
    private Set<StampingComponent> allStampers;
    private Object2IntOpenHashMap<StampsDynamically> sourceIndices;
    private int cursor;

    public static void asStamperDo(@Nullable Grid grid, CircuitComponent component, Consumer<StampingComponent> cons) {
        switch(component) {
            case StampingComponent stamper -> cons.accept(stamper);
            case Node n -> n.forEachTerminal(terminal -> { 
                if(terminal.getParentConstruct() instanceof StampingComponent stamper)
                    cons.accept(stamper);
            });
            case Circuit circuit -> circuit.forEachComponent(comp -> {
                if(comp instanceof StampingComponent stamper)
                    cons.accept(stamper);
            });
            case null, default -> { if(grid != null) grid.warn("Skipped semantic execution for " + component 
                + " - This component couldn't be paired down to a stamper!"); }
        }
    }

    public void allocate(CircuitComponent component) {
        MNAIndexer.asStamperDo(null, component, this::allocate);
    }

    public void allocate(StampingComponent component) {
        if(!getStampers().add(component) || !(component instanceof StampsDynamically sd)) 
            return;
        int size = sd.getAllocations();
        if(size == 0) return;
        if(size < 0) throw new IllegalArgumentException("Attempted to allocate a negative number for " + component + "!");
        getSourceIndices().put(sd, cursor);
        cursor += size;
    }

    public void forget(CircuitComponent component) {
        MNAIndexer.asStamperDo(null, component, this::forget);
    }

    public void forget(StampingComponent component) {
        if(!getStampers().remove(component) || !(component instanceof StampsDynamically sd)) 
            return;
        getSourceIndices().removeInt(sd);
        cursor -= sd.getAllocations();
    }

    public int get(StampingComponent component) {
        if(sourceIndices == null || sourceIndices.isEmpty()) {
            throw new IllegalStateException("Couldn't get source index for stamper " 
                + component + " - This tracker doesn't contain any allocations!");
        }
        int output = sourceIndices.getOrDefault(component, -1);
        if(output < 0) {
            throw new IllegalArgumentException("Couldn't get stamping index for stamper" 
                + component + " - This component hasn't been allocated in this tracker!");
        }
        return output;
    }

    public Object2IntOpenHashMap<StampsDynamically> getSourceIndices() {
        if(sourceIndices == null) sourceIndices = new Object2IntOpenHashMap<>();
        return sourceIndices;
    }

    public Set<StampingComponent> getStampers() {
        if(allStampers == null) allStampers = new ObjectOpenHashSet<>();
        return allStampers;
    }

    public int size() {
        return allStampers == null ? 0 : allStampers.size();
    }

    public int total() {
        return cursor;
    }

    public boolean hasStampers() {
        return !(allStampers == null || allStampers.isEmpty());
    }

    public void clear() {
        sourceIndices = null;
        allStampers = null;
        cursor = 0;
    }
}
