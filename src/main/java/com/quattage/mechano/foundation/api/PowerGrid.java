package com.quattage.mechano.foundation.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.landmarks.GridNode;
import com.quattage.mechano.foundation.api.landmarks.GridPath;
import com.quattage.mechano.foundation.api.landmarks.NodeIdentifiable;
import com.quattage.mechano.foundation.api.landmarks.NodeIdentifier;
import com.quattage.mechano.foundation.api.landmarks.NodeSet;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;


public class PowerGrid {

    protected GlobalServerGrid global;
    public NodeSet nodes;

    protected PowerGrid(GlobalServerGrid parent, int preload) {
        this.nodes = new NodeSet(new ObjectOpenHashSet<NodeIdentifiable<GridNode>>(preload));
        this.global = parent;
    }

    public PowerGrid(PowerGrid original, NodeSet newContents) {
        this.nodes = newContents;
        this.global = original.global;
    }

    /**
     * Gets the node at the give address, or creates a new one if
     * no node at this address exists. This method is mainly designed
     * to be used during the loading process defined in {@link GlobalServerGrid#makeProvisionalNodeAndLinks}
     * <p>
     * Note that, if the returned GridNode is newly created, it will be blank. 
     * Blank GridNodes that have no links should not persist in the PowerGrid 
     * for long, since they represent dead ends.
     * @param address Address to get or add (Compatable with any type outlined by {@link NodeSet#get})
     * @return The GridNode at this address, or a new one. Will never be <code>null</code>.
     * 
     * @throws IllegalStateException If <code>address</code> does not point to a valid BlockEntity, or the BlockEntity isn't able to host a GridNode at the address - See {@link NodeIdentifiable#getHost}
     */
    public GridNode getOrCreateProvisional(NodeIdentifiable<?> address) {
        NodeIdentifiable<GridNode> preexisting = nodes.get(address);
        if(preexisting != null) return preexisting.getValue();
        PowerGridBlockEntity pgbe = address.getHost(global.getLevelReader());
        if(pgbe == null) throw new IllegalStateException("Cannot instantiate a Provisional GridNode at " + address + " - there is no valod host BlockEntity at this location!");
        preexisting = new GridNode(this, pgbe, address.getPos(), address.getIndex());
        nodes.add(preexisting.getValue());
        return preexisting.getValue();
    }

    /**
     * Performs a Flood-Fill to locate discontinuities in this PowerGrid's
     * underlying matrix. (https://en.wikipedia.org/wiki/Flood_fill) <p>
     * 
     * Calls to this method will <strong>not</strong> modify this PowerGrid
     * in-place. Instead, a list of PowerGrids is formed as a result of the 
     * discontinuities contained within this one.
     * @return List of new PowerGrid instances. The list will be empty if this PowerGrid contains no discontinuities.
     */
    public @Nullable List<PowerGrid> splitDiscontinuities() {
        final Set<NodeIdentifiable<?>> visited = new HashSet<>();
        final List<PowerGrid> output = new ArrayList<>();
        nodes.forEach(node -> {
            if(visited.contains(node)) return;
            NodeSet cluster = new NodeSet();
            floodFillRecurse(node, visited, cluster);
            if(!cluster.set.isEmpty())
                output.add(new PowerGrid(this, cluster));
        });
        return output;
    }

    // recursive implementation for the method ^^ up there
    private void floodFillRecurse(NodeIdentifiable<?> start, Set<NodeIdentifiable<?>> visited, NodeSet clusterResult) {
        GridNode iteration = nodes.get(start);
        visited.add(start);
        if(!iteration.isValid()) return;
        clusterResult.add(iteration);
        iteration.forEachLink(link -> {
            if(!visited.contains(link.getEnd()))
                floodFillRecurse(iteration, visited, clusterResult);
        });
    }


    /**
     * Performs a path traversal to create a {@link GridPath} connecting <code>start</code>
     * and <code>end</code>. <p>
     * Uses the A* pathfinding algorithm:
     * https://en.wikipedia.org/wiki/A*_search_algorithm
     * @param start Address to begin searching from
     * @param destination Address to search for. If this address does not point to a valid target in this PowerGrid, the search is terminated immediately.
     * @return The resulting {@link GridPath} or null if no path could be found
     */
    public @Nullable GridPath findPathBetween(NodeIdentifiable<?> start, NodeIdentifiable<?> destination) {

        if(!nodes.contains(destination)) return null;
        if(start.equals(destination)) return null;

        final Queue<GridNode.Tracker> open = new PriorityQueue<>(11);
        open.add(start.makeTrackable().prime(destination));

        final GridPath output = GridPath.makeProvisional();
        final ObjectOpenHashSet<GridNode.Tracker> trackedNodes = new ObjectOpenHashSet<>();
    
        while(!open.isEmpty()) {
            final GridNode.Tracker local = open.poll();
            if(local.equals(destination))
                return output;
            trackedNodes.add(local);
            local.markVisited();
            local.node.forEachLink(adjacentLink -> {
                if(!adjacentLink.canTraverse()) return;
                GridNode.Tracker neighbor = trackedNodes.get(adjacentLink.getEnd());
                if(neighbor == null) {
                    neighbor = adjacentLink.getEnd().makeTrackable();
                    trackedNodes.add(neighbor);
                }
                if(local.investigateAcross(adjacentLink, neighbor)) {
                    output.add(adjacentLink);
                    if(!open.contains(neighbor))
                        open.add(neighbor);
                }
            });
        }
        return null;
    }

    /**
     * Retrieve every node in this PowerGrid belonging to the given
     * BlockPos, regardless of index
     * @param pos block position in the minecraft world to look for
     * @return A list of all GridNode objects belonging to the given BlockPos
     */
    public List<GridNode> getAllOccurancesOf(BlockPos pos) {
        List<GridNode> output = new ArrayList<>();
        for(int x = 0; x < NodeIdentifier.MAX_OCCUPANCY; x++) {
            GridNode link = nodes.get(new NodeIdentifier.Key(pos, x));
            if(link == null) break;
            output.add(link);
        }
        return output;
    }

    /**
     * Retrieve every node in this PowerGrid belonging to the given
     * BlockPos, regardless of index
     * @param pos block position in the minecraft world to look for
     * @return A list of all GridNode objects belonging to the given BlockPos
     */
    public List<GridNode> getAllOccurancesOf(int x, int y, int z) {
        return getAllOccurancesOf(new BlockPos(x, y, z));
    }

    /**
     * Clears this PowerGrid, erasing its matrix and resizing its hash table.
     * Broadcasts updates as a result.
     */
    public void clear() {
        Iterator<NodeIdentifiable<GridNode>> it = nodes.set.iterator();
        while (it.hasNext()) {
            GridNode node = it.next().getValue();
            node.wipeLinks(true);
            it.remove();
        }
        nodes.set.trim(4);
    }

    public ServerLevel getWorld() {
        return global.getWorld();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof PowerGrid that)) return false;
        return this.nodes.equals(that.nodes);
    }
}