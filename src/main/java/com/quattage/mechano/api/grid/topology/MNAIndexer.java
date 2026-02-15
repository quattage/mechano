package com.quattage.mechano.api.grid.topology;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.HierarchicalConstruct;
import com.quattage.mechano.api.grid.component.StampingComponent;
import com.quattage.mechano.api.grid.component.StampingComponent.StampsDynamically;
import com.quattage.mechano.api.grid.topology.landmark.Node;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

/**
 * Tracks all active stampers as well as any auxillary variables during multi-nodal 
 * analysis (MNA) as {@link StampingComponent} instances allocate their own space in 
 * the B vector.
 */
public class MNAIndexer implements StampsDynamically {
    
    private @Nullable Set<StampingComponent> allStampers;
    private @Nullable Object2IntOpenHashMap<StampsDynamically> sourceIndices;
    private @Nullable Object2IntOpenHashMap<Node> nodeIndices;
    private int cursor;

    public static MNAIndexer concatenate(MNAIndexer a, MNAIndexer b) {
        if(a.allStampers.size() > b.allStampers.size()) {
            a.allStampers.addAll(b.allStampers);
            if(b.sourceIndices != null) {
                a.getSourceIndices().ensureCapacity(a.sourceIndices.size() + b.sourceIndices.size());
                for(StampsDynamically sd : b.sourceIndices.keySet())
                    a.add(sd);

            }
            a.nodeIndices = null;
            b.clear();
            return a;
        }
        b.allStampers.addAll(a.allStampers);
        if(a.sourceIndices != null) {
        b.getSourceIndices().ensureCapacity(a.sourceIndices.size() + b.sourceIndices.size());
            for(StampsDynamically sd : a.sourceIndices.keySet())
                b.add(sd);
        }
        b.nodeIndices = null;
        a.clear();
        return b;
    }

    public MNAIndexer() {}

    public void add(StampingComponent component) {
        if(!getStampers().add(component) || !(component instanceof StampsDynamically sd)) 
            return;
        int size = sd.getAllocations();
        if(size == 0) return;
        if(size < 0) throw new IllegalArgumentException("Attempted to allocate a negative number for " + component + "!");
        getSourceIndices().put(sd, cursor);
        cursor += (size - 1);
    }

    private void add(StampsDynamically sd) {
        int size = sd.getAllocations();
        sourceIndices.put(sd, cursor);
        cursor += (size - 1);
    }

    public void remove(StampingComponent component) {
        if(!getStampers().remove(component) || !(component instanceof StampsDynamically sd)) 
            return;
        getSourceIndices().removeInt(sd);
        cursor -= sd.getAllocations();
    }

    public int get(StampingComponent component) {
        if(sourceIndices == null || sourceIndices.isEmpty()) {
            throw new IllegalStateException("Couldn't get source index for stamper " 
                + component + " - This indexer doesn't contain any allocations!");
        }
        return sourceIndices.getOrDefault(component, -1);
    }

    public boolean add(Node node, int index) {
        if(node.isGrounded()) return false;
        if(nodeIndices == null) {
            nodeIndices = new Object2IntOpenHashMap<>();
            nodeIndices.defaultReturnValue(-2);
        }
        nodeIndices.put(node, index);
        return true;
    }

    public int remove(Node node) {
        if(sourceIndices == null) return -2;
        if(node.isGrounded()) return -1;
        return nodeIndices.removeInt(node);
    }

    public int indexOf(Node node) {
        if(nodeIndices == null)  return -2;
        if(node.isGrounded()) return -1;
        return nodeIndices.getInt(node);
    }

    public Object2IntOpenHashMap<StampsDynamically> getSourceIndices() {
        if(sourceIndices == null) sourceIndices = new Object2IntOpenHashMap<>();
        return sourceIndices;
    }

    public Set<StampingComponent> getStampers() {
        if(allStampers == null) allStampers = new ObjectOpenHashSet<>();
        return allStampers;
    }

    public void stamp(ServerGrid grid, GridDomain domain) {
        if(!hasStampers()) return;
        for(StampingComponent component : allStampers)
            component.stamp(grid, domain);
    }

    @Override
    public void stampDynamic(ServerGrid grid, GridDomain domain) {
        if(sourceIndices == null || sourceIndices.isEmpty()) return;
        for(StampsDynamically dyn : sourceIndices.keySet())
            dyn.stampDynamic(grid, domain);
    }

    @Override
    public int getAllocations() {
        return cursor;
    }

    public int sourceCount() {
        return sourceIndices == null ? 0 : sourceIndices.size();
    }

    public boolean hasStampers() {
        return !(allStampers == null || allStampers.isEmpty());
    }

    @Override
    public String toString() {
        String out = "\n";
        if(!hasStampers())
            return out + "  Empty";
        for(StampingComponent component : allStampers) {
            out += "  '" + component.getComponentID() + "'\n";
            HierarchicalConstruct parent = component.getParentConstruct();
            out += "    type: " + component.getHierarchyType() + ", owned by " + (parent == null ? " n/a" : parent.getClass().getSimpleName()) + "\n";
            if(!(component instanceof StampsDynamically sd)) {
                out += "    [no dynamic allocations]\n";
                continue;
            }
            int allocs = sd.getAllocations();
            out += "    [" + allocs + (allocs == 1 ? " allocation" : " allocations") + " starting at index " + sourceIndices.getInt(sd) + "]\n";
        }
        return out.substring(0, out.length() - 1);
    }

    public void clear() {
        sourceIndices = null;
        nodeIndices = null;
        allStampers = null;
        cursor = 0;
    }
}
