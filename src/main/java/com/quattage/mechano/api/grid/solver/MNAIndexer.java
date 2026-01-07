package com.quattage.mechano.api.grid.solver;

import java.util.Set;

import com.quattage.mechano.api.grid.topology.CircuitComponent.StampingComponent;
import com.quattage.mechano.api.grid.topology.CircuitComponent.StampsDynamically;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

/**
 * Tracks all active stampers as well as any auxillary variables during multi-nodal 
 * analysis (MNA) as {@link StampingComponent} instances allocate their own space in 
 * the B vector.
 */
public class MNAIndexer {
    
    private Set<StampingComponent> allStampers = new ObjectOpenHashSet<>();
    private Object2IntOpenHashMap<StampsDynamically> sourceIndices = new Object2IntOpenHashMap<>();
    private int cursor;

    public void allocate(StampingComponent component) {
        if(!allStampers.add(component) || !(component instanceof StampsDynamically sd)) 
            return;
        int size = sd.getAllocations();
        if(size == 0) return;
        if(size < 0) throw new IllegalArgumentException("Attempted to allocate a negative number for " + component + "!");
        sourceIndices.put(sd, cursor);
        cursor += size;
    }

    public void forget(StampingComponent component) {
        if(!allStampers.remove(component) || !(component instanceof StampsDynamically sd)) 
            return;
        sourceIndices.removeInt(sd);
        cursor -= sd.getAllocations();
    }

    public int get(StampingComponent component) {
        if(sourceIndices.isEmpty()) {
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

    public int size() {
        return allStampers.size();
    }

    public int total() {
        return cursor;
    }

    public Set<StampingComponent> getAllStampers() {
        return allStampers;
    }

    public boolean hasStampers() {
        return !(allStampers == null || allStampers.isEmpty());
    }

    public void clear() {
        sourceIndices = new Object2IntOpenHashMap<>();
        allStampers = new ObjectOpenHashSet<>();
        cursor = 0;
    }
}
