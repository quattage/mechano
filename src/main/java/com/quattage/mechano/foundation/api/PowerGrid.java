package com.quattage.mechano.foundation.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.landmark.GridLink;
import com.quattage.mechano.foundation.api.landmark.GridNode;
import com.quattage.mechano.foundation.api.landmark.GridPath;
import com.quattage.mechano.foundation.api.landmark.NodeIdentifiable;
import com.quattage.mechano.foundation.api.landmark.NodeIdentifier;
import com.quattage.mechano.foundation.api.landmark.NodeSet;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;


public class PowerGrid {

    protected GlobalServerGrid global;
    public int gridIndex = -1;
    public NodeSet nodes;

    public PowerGrid(PowerGrid original, @Nullable NodeSet newContents) {
        Objects.requireNonNull(original);
        this.global = original.global;
        this.gridIndex = original.gridIndex;
        this.nodes = newContents == null ? new NodeSet() : newContents;
        this.global.subgrids.set(gridIndex, this);
    }

    public PowerGrid(GlobalServerGrid parent, @Nullable NodeSet newContents) {
        Objects.requireNonNull(parent);
        this.gridIndex = parent.subgrids.size();
        this.global = parent;
        this.nodes = newContents == null ? new NodeSet() : newContents;
        this.global.subgrids.add(this);
    }

    protected PowerGrid(GlobalServerGrid parent, int preload) {
        Objects.requireNonNull(parent);
        this.gridIndex = parent.subgrids.size();
        this.nodes = new NodeSet(new ObjectOpenHashSet<GridNode>(preload));
        this.global = parent;
        this.global.subgrids.add(this);
    }

    /**
     * Gets the node at the given address, or create a new one if
     * no node at this address exists. This method is mainly designed
     * to be used during the loading process defined in {@link GlobalServerGrid#makeProvisionalNodeAndLinks}
     * <p>
     * Note that, if the returned GridNode is newly created, it will be blank. 
     * Blank GridNodes that have no links should not persist in the PowerGrid 
     * for long, since they represent dead ends.
     * @param address Address to get or add (Compatable with any type outlined by {@link NodeSet#get})
     * @return The GridNode at this address, or the new one that was created at the specified address. Will be null if there is no PGBE at the address.
     * @throws IllegalStateException if this PowerGrid has been {@link PowerGrid#destroy destroyed.}
     */
    public @Nullable GridNode getOrCreateProvisional(NodeIdentifiable address) {
        assertNotDestroyed();
        GridNode node = nodes.get(address);
        if(node != null) return node;
        PowerGridBlockEntity pgbe = address.getHost(global.getLevelReader());
        if(pgbe == null) return null;
        node = new GridNode(this, pgbe, address.getIndex());
        pgbe.surrogate.owner = this;
        this.nodes.add(node);
        return node;
    }

    /**
     * Splits discontinuities and removes empty or stale {@link GridNode}
     * instances from this PowerGrid. The instance that this is run on will be stale,
     * and a new list of instnaces will be added to the {@link GlobalTransferGrid}
     * that this local belongs to.
     */
    public void cleanup(boolean deepClean) {
        if(global == null) {
            if(nodes == null) return;
            if(deepClean) {
                for(GridNode node : nodes) {
                    node.links.clear();
                    node.notifyHost();
                    node.nullify();
                }
            }
            this.nullify();
            return;
        }

        if(nodes == null || nodes.isEmpty()) {
            global.destroyGrid(this);
            return;
        }

        if(nodes.size() < 2) {
            for(GridNode node : nodes) {
                node.links.clear();
                node.notifyHost();
                node.nullify();
            }
            global.destroyGrid(this);
            return;
        }

        if(this.nodes.size() > 3) {
            List<PowerGrid> clusters = splitDiscontinuities();
            if(clusters.size() > 1) {
                global.destroyGrid(this);
                global.addAll(clusters);
            }
        }
    }

    /**
     * Performs a Flood-Fill to locate discontinuities in this PowerGrid's
     * underlying matrix. (https://en.wikipedia.org/wiki/Flood_fill) 
     * <p>
     * Calls to this method will <strong>not</strong> modify this PowerGrid
     * in-place. Instead, a list of PowerGrids is formed as a result of the 
     * discontinuities contained within this one.
     * @return List of new PowerGrid instances. The list will be empty if this 
     * PowerGrid contains no discontinuities.
     * @throws IllegalStateException if this PowerGrid has been {@link PowerGrid#destroy destroyed.}
     */
    public @Nullable List<PowerGrid> splitDiscontinuities() {
        assertNotDestroyed();
        final Set<NodeIdentifiable> visited = new HashSet<>();
        final List<PowerGrid> output = new ArrayList<>();
        nodes.forEach(node -> {
            if(visited.contains(node)) return;
            NodeSet cluster = new NodeSet();
            floodFillRecurse(node, visited, cluster);
            if(cluster.size() > 1)
                output.add(new PowerGrid(this, cluster));
        });
        return output;
    }

    // recursive implementation for the method ^^ up there
    private void floodFillRecurse(NodeIdentifiable start, Set<NodeIdentifiable> visited, NodeSet clusterResult) {
        GridNode iteration = nodes.get(start);
        visited.add(start);
        if(iteration == null || iteration.links.isEmpty()) return;
        clusterResult.add(iteration);
        iteration.forEachLink(link -> {
            GridNode adjacent = link.getEnd();
            if(!visited.contains(adjacent))
                floodFillRecurse(adjacent, visited, clusterResult);
        });
    }

    /**
     * Performs a path traversal to create a {@link GridPath} 
     * connecting <code>start</code> and <code>end</code>. <p>
     * Uses the A* pathfinding algorithm:
     * https://en.wikipedia.org/wiki/A*_search_algorithm
     * @param start Address to begin searching from
     * @param end Address to search for
     * @return The resulting {@link GridPath} or null if no path could be found
     * @throws IllegalStateException if this PowerGrid has been {@link PowerGrid#destroy destroyed.}
     */
    public @Nullable GridPath findPathBetween(NodeIdentifiable start, NodeIdentifiable end) {
        assertNotDestroyed();

        if(start == null || !nodes.contains(start)) return null;
        if(end == null || !nodes.contains(end)) return null;
        if(start.equals(end)) return null;

        final Queue<GridNode.Tracker> open = new PriorityQueue<>(11);
        final GridPath output = GridPath.makeProvisional();
        final ObjectOpenHashSet<GridNode.Tracker> trackedNodes = new ObjectOpenHashSet<>();
        open.add(start.makeTrackable().estimateCostTo(end));

        while(!open.isEmpty()) {
            final GridNode.Tracker local = open.poll();
            if(local.equals(end)) return output;

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
     * @throws IllegalStateException if this PowerGrid has been {@link PowerGrid#destroy destroyed.}
     */
    public List<GridNode> getAllOccurancesOf(BlockPos pos) {
        assertNotDestroyed();
        List<GridNode> output = new ArrayList<>(NodeIdentifier.MAX_OCCUPANCY);
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
     * Merges the contents of the provided {@link NodeSet}
     * into this PowerGrid.
     * @param otherNodes Nodes to add
     * @return <code>true</code> if this PowerGrid was modified.
     */
    public boolean addAll(NodeSet otherNodes) {
        assertNotDestroyed();
        if(otherNodes.isEmpty()) return false;
        int oldSize = this.nodes.set.size();
        this.nodes.set.ensureCapacity(oldSize + otherNodes.set.size());
        otherNodes.forEach(node -> {
            this.nodes.set.add(node);
        });
        return oldSize != this.nodes.set.size();
    }

    /**
     * Clears this PowerGrid, erasing its matrix and resizing its hash table.
     * Broadcasts updates as a result.
     */
    public void clear() {
        assertNotDestroyed();
        Iterator<GridNode> it = nodes.set.iterator();
        while (it.hasNext()) {
            GridNode node = it.next();
            node.wipeLinks(true);
            node.nullify();
            it.remove();
        }
        nodes.set.trim(4);
        nullify();
    }

    /**
     * Removes the node at the given address from this PowerGrid.
     * If this PowerGrid is empty as a result of this call, this
     * method will also remove and destroy this PowerGrid.
     * @param address
     * @return <code>true</code> if this PowerGrid was modified as a result
     * of this call.
     */
    public boolean removeNode(NodeIdentifiable address) {
        assertNotDestroyed();        

        // remove the node in question
        GridNode removed = nodes.get(address);
        if(address == null) return false;
        nodes.remove(removed);
        if(removed.links.isEmpty()) {
            if(nodes != null && nodes.isEmpty())
                global.destroyGrid(this);
            removed.nullify();
            return true;
        }

        final Set<GridNode> altered = new HashSet<>();

        // remove links symmetrically while tracking changes
        removed.forEachLink(link -> {
            if(link == null) return;
            GridNode endNode = link.getEnd();
            Iterator<GridLink> linksIter = endNode.links.iterator();
            while(linksIter.hasNext()) {
                GridLink linkToRemove = linksIter.next();
                if(linkToRemove.endsWith(address)) {
                    linksIter.remove();
                    altered.add(linkToRemove.getStart());
                    altered.add(linkToRemove.getEnd());
                }
            }
        });

        // notify adjacent nodes of the removal
        for(GridNode node : altered) {
            node.notifyHost();
            if(node.links.isEmpty()) {
                node.host.surrogate.forget(getWorld());
                nodes.remove(node);
                if(node.owner.nodes.isEmpty())
                    global.destroyGrid(node.owner);
                node.nullify();
            }
        }

        cleanup(true);
        removed.nullify();
        return true;
    }

    /**
     * Nullifies references in this PowerGrid for when it is removed.<p>
     * Note that this method does <strong>NOT</strong> broadcast
     * changes or do any syncing - This method is specifically
     * to mark grids as stale so they aren't used anymore.
     * <p>
     * If this method is called on a PowerGrid that's actively being
     * used, all hell will break lose.
     */
    public void nullify() {
        gridIndex = -1;
        nodes = null;
    }

    private void assertNotDestroyed() {
        if(nodes == null || global == null) 
            throw new IllegalStateException("An operation attempted to run on a PowerGrid that has already been destroyed. (A PowerGrid was probably leaked!)");
    }

    public ServerLevel getWorld() {
        assertNotDestroyed();
        return global.getWorld();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof PowerGrid that)) return false;
        return this.gridIndex == that.gridIndex;
    }
}