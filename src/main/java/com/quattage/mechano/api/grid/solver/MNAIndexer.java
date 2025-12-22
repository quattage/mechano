package com.quattage.mechano.api.grid.solver;

import com.quattage.mechano.api.grid.topology.CircuitComponent.StampingComponent;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Tracks any auxillary variables during multi-nodal analysis (MNA)
 * as {@link StampingComponent} instances allocate their own space in the mna vector.
 */
public class MNAIndexer {
    
    private Object2IntOpenHashMap<StampingComponent> sourceIndices = new Object2IntOpenHashMap<>();
    private int cursor, totalStampers;

    public void allocate(StampingComponent component) {
        totalStampers++;
        if(sourceIndices.containsKey(component)) return;
        int size = component.getAllocations();
        if(size == 0) return;
        if(size < 0) throw new IllegalArgumentException("Attempted to allocate a negative number for " + component + "!");
        sourceIndices.put(component, cursor);
        cursor += size;
    }

    public void forget(StampingComponent component) {
        totalStampers--;
        if(!sourceIndices.containsKey(component)) return;
        sourceIndices.removeInt(component);
        cursor -= component.getAllocations();
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
        return cursor;
    }

    public int totalStampers() {
        return totalStampers;
    }

    public boolean hasStampers() {
        return totalStampers > 0;
    }

    public void clear() {
        sourceIndices = new Object2IntOpenHashMap<>();
        cursor = 0; totalStampers = 0;
    }
}
