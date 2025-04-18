package com.quattage.mechano.foundation.api.grid.landmarks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.quattage.mechano.foundation.api.grid.PowerGrid;

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
 * 
 * This class is backed by an {@link it.unimi.dsi.fastutil.objects.ObjectOpenHashSet ObjectOpenHashSet},
 * where nodes are treated simultaneously as the key and the value. 
 * The {@link NodeSet#get get} method can be used with any subclass of {@link NodeIdentifiable} 
 * to look up nodes in the set.
 */
public class NodeSet extends AbstractObjectSet<GridNode> {

    public final ObjectOpenHashSet<NodeIdentifiable<GridNode>> set;

    public NodeSet() {
        this.set = new ObjectOpenHashSet<NodeIdentifiable<GridNode>>();
    }

    public NodeSet(ObjectOpenHashSet<NodeIdentifiable<GridNode>> set) {
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
     * Remove a Node from this NodeMatrix. Nodes can be removed by supplying
     * a {@link GridNode GridNode} instance, any instance of {@link NodeIdentifiable},
     * or by specifying the exact location directly. 
     * @param o Any subclass of {@link NodeIdentifiable} with compatable hashing
     * @return <code>TRUE</code> if this matrix was modified as a result of this call
     */
    @Override
    public boolean remove(Object o) {
        return set.remove(o);
    }

    /**
     * Remove a Node from this NodeMatrix. Nodes can be removed by supplying
     * a {@link GridNode GridNode} instance, any instance of {@link NodeIdentifiable},
     * or by specifying the exact location directly. 
     * @param pos block position in the minecraft world to look for
     * @param index index at the given BlockPos
     * @return <code>TRUE</code> if this matrix was modified as a result of this call
     */
    public boolean remove(BlockPos pos, int index) {
        return set.remove(new GridNode.Address(pos, index));
    }

    /**
     * Remove a Node from this NodeMatrix. Nodes can be removed by supplying
     * a {@link GridNode GridNode} instance, any instance of {@link NodeIdentifiable},
     * or by specifying the exact location directly. 
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param index index at the given XYZ to search for
     * @return <code>TRUE</code> if this matrix was modified as a result of this call
     */
    public boolean remove(int x, int y, int z, int index) {
        return set.remove(new GridNode.Address(new BlockPos(x, y, z), index));
    }

    @Override
    public boolean add(GridNode e) {
        return set.add(e);
    }

    /**
     * Retrieve a Node from this NodeMatrix. Nodes can be acquired by supplying
     * a {@link GridNode GridNode} instance, any instance of {@link NodeIdentifiable},
     * or by specifying the exact location directly. 
     * @param o Any subclass of {@link NodeIdentifiable} with compatable hashing
     * @return The GridNode object at the given address, or <code>null</code> if none could be found.
     */
    public GridNode get(Object o) {
        NodeIdentifiable<GridNode> ni = set.get(o);
        if(ni == null) return null;
        return ni.getValue();
    }

    /**
     * Retrieve a Node from this NodeMatrix. Nodes can be acquired by supplying
     * a {@link GridNode GridNode} instance, any instance of {@link NodeIdentifiable},
     * or by specifying the exact location directly. 
     * @param pos block position in the minecraft world to look for
     * @param index index at the given BlockPos
     * @return The GridNode object at the given address, or <code>null</code> if none could be found.
     */
    public GridNode get(BlockPos pos, int index) {
        NodeIdentifiable<GridNode> ni = set.get(new GridNode.Address(pos, index));
        if(ni == null) return null;
        return ni.getValue();
    }

    /**
     * Retrieve a Node from this NodeMatrix. Nodes can be acquired by supplying
     * a {@link GridNode GridNode} instance, any instance of {@link NodeIdentifiable},
     * or by specifying the exact location directly. 
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param index index at the given XYZ to search for
     * @return The GridNode object at the given address, or <code>null</code> if none could be found.
     */
    public GridNode get(int x, int y, int z, int index) {
        NodeIdentifiable<GridNode> ni = set.get(new GridNode.Address(new BlockPos(x, y, z), index));
        if(ni == null) return null;
        return ni.getValue();
    }

    /**
     * Retrieve every node in this NodeMatrix belonging to the given
     * BlockPos, regardless of index
     * @param pos block position in the minecraft world to look for
     * @return A list of all GridNode objects belonging to the given BlockPos
     */
    public List<GridNode> getAllOccurancesOf(BlockPos pos) {
        List<GridNode> output = new ArrayList<>();
        for(int x = 0; x < 8; x++) {
            GridNode link = get(new GridNode.Address(pos, x));
            if(link == null) break;
            output.add(link);
        }
        return output;
    }


    /**
     * Retrieve every node in this NodeMatrix belonging to the given
     * BlockPos, regardless of index
     * @param pos block position in the minecraft world to look for
     * @return A list of all GridNode objects belonging to the given BlockPos
     */
    public List<GridNode> getAllOccurancesOf(int x, int y, int z) {
        return getAllOccurancesOf(new BlockPos(x, y, z));
    }


    /**
     * Since GridNodes in the underlying hash set are wrapped in {@link NodeIdentifiable} objects,
     * iterators are <strong> !! unsupported !! </strong>
     */
    @Override
    public ObjectIterator<GridNode> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forEach(Consumer<? super GridNode> action) {
        set.forEach(ni -> {
            action.accept(ni.getValue());
        });
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
        for(NodeIdentifiable<?> node : set) {
            if(!that.contains(node)) 
                return false;
        }
        return true;
    }

    public ListTag write() {
        ListTag output = new ListTag();
        for(NodeIdentifiable<?> address : set)
            output.add(address.writeTo(new CompoundTag()));
        return output;
    }
}
