package com.quattage.mechano.foundation.api.grid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.grid.landmarks.GridNode;
import com.quattage.mechano.foundation.api.grid.landmarks.GridPath;
import com.quattage.mechano.foundation.api.grid.landmarks.NodeIdentifiable;
import com.quattage.mechano.foundation.api.grid.landmarks.NodeSet;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;


public class PowerGrid {

    protected GlobalServerGrid global;
    public NodeSet nodes;

    public static PowerGrid loadFrom(GlobalServerGrid dispatcher, LevelReader world, ListTag in) {
        PowerGrid freshInstance = new PowerGrid(dispatcher);
        ObjectOpenHashSet<NodeIdentifiable<GridNode>> deserialized = new ObjectOpenHashSet<>(in.size());
        for(int x = 0; x < in.size(); x++) {
            GridNode newNode = GridNode.loadFrom(freshInstance, world, in.getCompound(x));
            deserialized.add(newNode);
        }
        freshInstance.nodes = new NodeSet(deserialized);
        return freshInstance;
    }

    private PowerGrid(GlobalServerGrid parent) {
        this.nodes = new NodeSet();
        this.global = parent;
    }

    public PowerGrid(PowerGrid original, NodeSet newContents) {
        this.nodes = newContents;
        this.global = original.global;
    }

    /**
     * Performs a Flood-Fill to locate discontinuities in this PowerGrid's
     * underlying matrix. (https://en.wikipedia.org/wiki/Flood_fill) <p>
     * 
     * Calls to this method will <strong>not</strong> modify this PowerGrid
     * in-place. Instead, a list of PowerGrids is formed as a result of the 
     * discontinuities contained within this one.
     * @return List of new PowerGrid instances. Returns <code>null</code> if this PowerGrid contains no discontinuities.
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
        if(output.size() <= 1) return null;
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
     * Clears this PowerGrid, erasing its matrix and resizing its hash table.
     * Broadcasts updates as a result.
     */
    public void clear() {
        Iterator<NodeIdentifiable<GridNode>> it = nodes.set.iterator();
        while (it.hasNext()) {
            GridNode node = it.next().getValue();
            node.wipeLinks();
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