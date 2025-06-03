package com.quattage.mechano.foundation.api.landmark;

import com.quattage.mechano.foundation.api.PowerGrid;

import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/**
 * A collection of {@link GridNode GridNodes}. This class implements much of the data management features
 * that used to be built into {@link PowerGrid}. The NodeSet is intended for use as
 * the x axis of an adjacency list of {@link GridNode GridNodes}, but it may find use as a more
 * generic wrapper for a hash set. As such, all matrix-related implementation, such as
 * searching and pathfinding, are located in the {@link PowerGrid} class. <br></br>
 * This class is backed by an {@link it.unimi.dsi.fastutil.objects.ObjectOpenHashSet ObjectOpenHashSet},
 * where nodes are treated simultaneously as the key and the value. 
 * The {@link NodeSet#get get} method can be used with any subclass of {@link NodeIdentifiable} 
 * to look up nodes in the set.
 */
public class NodeSet extends AbstractObjectSet<GridNode> {

    public final ObjectOpenHashSet<GridNode> set;

    public NodeSet() {
        this.set = new ObjectOpenHashSet<GridNode>(2);
    }

    public NodeSet(ObjectOpenHashSet<GridNode> set) {
        this.set = set;
    }

    @Override
    public boolean contains(Object o) {
        return set.contains(o);
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    /**
     * Remove a Node from this NodeSet. Nodes can be removed by supplying
     * a {@link GridNode GridNode} instance, any instance of {@link NodeIdentifiable},
     * or by specifying the exact location directly. 
     * @param o Any subclass of {@link NodeIdentifiable} with compatable hashing
     * @return <code>true</code> if this matrix was modified as a result of this call
     */
    @Override
    public boolean remove(Object o) {
        return set.remove(o);
    }

    /**
     * Remove a Node from this NodeSet. Nodes can be removed by supplying
     * a {@link GridNode GridNode} instance, any instance of {@link NodeIdentifiable},
     * or by specifying the exact location directly. 
     * @param pos block position in the minecraft world to look for
     * @param index index at the given BlockPos
     * @return <code>true</code> if this matrix was modified as a result of this call
     */
    @SuppressWarnings("unlikely-arg-type")
    public boolean remove(BlockPos pos, int index) {
        return set.remove(new NodeIdentifier.Key(pos, index));
    }

    /**
     * Remove a Node from this NodeSet. Nodes can be removed by supplying
     * a {@link GridNode GridNode} instance, any instance of {@link NodeIdentifiable},
     * or by specifying the exact location directly. 
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param index index at the given XYZ to search for
     * @return <code>true</code> if this matrix was modified as a result of this call
     */
    @SuppressWarnings("unlikely-arg-type")
    public boolean remove(int x, int y, int z, int index) {
        return set.remove(new NodeIdentifier.Key(new BlockPos(x, y, z), index));
    }

    @Override
    public boolean add(GridNode e) {
        return set.add(e);
    }

    /**
     * Retrieve a Node from this NodeSet. Nodes can be acquired by supplying
     * a {@link GridNode GridNode} instance, any instance of {@link NodeIdentifiable},
     * or by specifying the exact location directly. 
     * @param o Any subclass of {@link NodeIdentifiable} with compatable hashing
     * @return The GridNode object at the given address, or <code>null</code> if none could be found.
     */
    public GridNode get(Object o) {
        return set.get(o);
    }

    /**
     * Retrieve a Node from this NodeSet. Nodes can be acquired by supplying
     * a {@link GridNode GridNode} instance, any instance of {@link NodeIdentifiable},
     * or by specifying the exact location directly. 
     * @param pos block position in the minecraft world to look for
     * @param index index at the given BlockPos
     * @return The GridNode object at the given address, or <code>null</code> if none could be found.
     */
    public GridNode get(BlockPos pos, int index) {
        GridNode node = set.get(new NodeIdentifier.Key(pos, index));
        return node;
    }

    /**
     * Retrieve a Node from this NodeSet. Nodes can be acquired by supplying
     * a {@link GridNode GridNode} instance, any instance of {@link NodeIdentifiable},
     * or by specifying the exact location directly. 
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param index index at the given XYZ to search for
     * @return The GridNode object at the given address, or <code>null</code> if none could be found.
     */
    public GridNode get(int x, int y, int z, int index) {
        GridNode node = set.get(new NodeIdentifier.Key(new BlockPos(x, y, z), index));
        return node;
    }


    /**
     * Since GridNodes in the underlying hash set are wrapped in {@link NodeIdentifiable} objects,
     * iterators are <strong> !! unsupported !! </strong>
     */
    @Override
    public ObjectIterator<GridNode> iterator() {
        return set.iterator();
    }

    /***
     * Comparing NodeSets for equivalence is a brute-force operation that's only ever
     * really useful for performing unit tests
     */
    @Override
    public boolean equals(Object o) {
        if(o == this) return true;
        if(!(o instanceof NodeSet that)) return false;
        if(this.size() != that.size()) return false;
        for(GridNode node : set) {
            if(!that.contains(node)) 
                return false;
        }
        return true;
    }

    public ListTag write() {
        ListTag output = new ListTag();
        for(GridNode node : set) {
            if(node == null || !node.isValid()) continue;
            output.add(node.writeTo(new CompoundTag()));
        }
        return output;
    }
}
