package com.quattage.mechano.foundation.api.landmark;

import java.util.Iterator;
import java.util.function.Consumer;

import com.quattage.mechano.foundation.api.ServerMatrix;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/**
 * A collection of {@link GridNode GridNodes}. This class implements much of the data management features
 * that used to be built into {@link ServerMatrix}. The NodeMap is intended for use as
 * the x axis of an adjacency list of {@link GridNode GridNodes}, but it may find use as a more
 * generic wrapper for a hash map. As such, all matrix-related implementation, such as
 * searching and pathfinding, are located in the {@link ServerMatrix} class. <br></br>
 * This class is backed by an {@link it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap Object2ObjectOpenHashMap},
 * where nodes are treated simultaneously as the key and the value. 
 * The {@link NodeMap#get get} method can be used with any subclass of {@link GridUUID} 
 * to look up nodes in the map.
 */
public class NodeMap implements Iterable<GridNode> {

    private final Object2ObjectOpenHashMap<GridUUID, GridNode> map;

    public NodeMap() {
        this.map = new Object2ObjectOpenHashMap<>(2);
    }

    public NodeMap(Object2ObjectOpenHashMap<GridUUID, GridNode> nodes) {
        this.map = nodes;
    }

    public NodeMap(int preload) {
        this.map = new Object2ObjectOpenHashMap<>(preload);
    }

    @Override
    public boolean equals(Object o) {
        if(o == this) return true;
        if(!(o instanceof NodeMap that)) return false;
        if(this.size() != that.size()) return false;
        for(GridUUID addr : map.keySet()) {
            if(!that.contains(addr)) 
                return false;
        }
        return true;
    }

    public ListTag write() {
        ListTag output = new ListTag();
        for(GridNode node : map.values()) {
            if(node == null) continue;
            output.add(node.writeTo(new CompoundTag()));
        }
        return output;
    }

    @Override
    public void forEach(Consumer<? super GridNode> action) {
        for(GridNode node : map.values()) {
            if(node == null) continue;
            action.accept(node);
        }
    }

    /**
     * Add the given GridNode to this NodeMap.
     * @param node Node to add
     * @return <code>true</code> if this NodeMap didn't already contain the given node
     */
    public boolean add(GridNode node) {
        return map.put(node.getAddress(), node) != null;
    }

    
    /**
     * Remove a Node from this NodeMap. Nodes can be removed by supplying
     * any instance of {@link GridUUID},
     * @param o {@link GridNode} or {@link GridUUID}
     * @return the GridNode that was removed, or <code>null</code> if this NodeMap did not contain a {@link GridNode} mapped to <code>address</code>
     */
    public GridNode remove(GridNode o) {
        if(o == null) return null;
        return map.remove(o.getAddress());
    }

    /**
     * Remove a Node from this NodeMap. Nodes can be removed by supplying
     * any instance of {@link GridUUID} or {@link GridNode}.
     * @param o {@link GridNode} or {@link GridUUID}
     * @return the GridNode that was removed, or <code>null</code> if this NodeMap did not contain a {@link GridNode} mapped to <code>address</code>
     */
    public GridNode remove(GridUUID o) {
        return map.remove(o);
    }

    /**
     * Remove a Node from this NodeMap. Nodes can be removed by supplying
     * any instance of {@link GridUUID} or {@link GridNode}.
     * @param o {@link GridNode} or {@link GridUUID}
     * @return the GridNode that was removed, or <code>null</code> if this NodeMap did not contain a {@link GridNode} mapped to <code>address</code>
     */
    public boolean contains(GridNode o) {
        if(o == null) return false;
        return map.containsKey(o.getAddress());
    }

    /**
     * Remove a Node from this NodeMap. Nodes can be removed by supplying
     * any instance of {@link GridUUID} or {@link GridNode}.
     * @param o {@link GridNode} or {@link GridUUID}
     * @return the GridNode that was removed, or <code>null</code> if this NodeMap did not contain a {@link GridNode} mapped to <code>address</code>
     */
    public boolean contains(GridUUID o) {
        return map.containsKey(o);
    }

    /**
     * Retrieve a Node from this NodeMap. Nodes can be acquired by supplying
     * any instance of {@link GridUUID} or {@link GridNode}.
     * @param o Any subclass of {@link GridUUID} with compatable hashing
     * @return The GridNode object at the given address, or <code>null</code> if none could be found.
     */
    public GridNode get(GridNode o) {
        if(o == null) return null;
        return map.get(o.getAddress());
    }

    /**
     * Retrieve a Node from this NodeMap. Nodes can be acquired by supplying
     * any instance of {@link GridUUID} or {@link GridNode}.
     * @param o {@link GridNode} or {@link GridUUID}
     * @return The GridNode object at the given address, or <code>null</code> if none could be found.
     */
    public GridNode get(GridUUID o) {
        return map.get(o);
    }

    public NodeMap ensureCapacity(int capacity) {
        map.ensureCapacity(capacity);
        return this;
    }

    public NodeMap trim() {
        map.trim(4);
        return this;
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public Iterator<GridNode> iterator() {
        return map.values().iterator();
    }
}
