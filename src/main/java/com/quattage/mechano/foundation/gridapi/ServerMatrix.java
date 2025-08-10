package com.quattage.mechano.foundation.gridapi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.gridapi.anchor.SurrogateNode;
import com.quattage.mechano.foundation.gridapi.landmark.GridLink;
import com.quattage.mechano.foundation.gridapi.landmark.GridNode;
import com.quattage.mechano.foundation.gridapi.landmark.GridPath;
import com.quattage.mechano.foundation.gridapi.landmark.NodeMap;
import com.quattage.mechano.foundation.gridapi.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.gridapi.landmark.identifier.HeuristicUUID;
import com.quattage.mechano.foundation.gridapi.switchboard.GridResponse;
import com.quattage.mechano.foundation.helper.Worldly;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.level.ServerLevel;


/**
 * Represents a single localized cluster made of nodes and links,
 * where each node has continuity with every other node in the cluster.
 */
public class ServerMatrix implements Worldly {

    private ServerGrid globalGrid;
    private int index = -1;
    public NodeMap nodes;

    public static ServerMatrix createAndPrepare(ServerGrid parent) {
        Objects.requireNonNull(parent);
        if(!parent.isValid()) throw new IllegalArgumentException("Can't instantiate a new ServerMatrix - The parent matrix is invalid!");
        ServerMatrix matrix = new ServerMatrix(parent, new NodeMap(2));
        matrix.index = parent.matrices.size();
        parent.matrices.add(matrix);
        return matrix;
    }

    private ServerMatrix(ServerGrid parent, NodeMap nodes) {
        Objects.requireNonNull(parent);
        Objects.requireNonNull(nodes);
        this.globalGrid = parent;
        this.nodes = nodes;
        this.index = parent.matrices.size();
    }

    /**
     * Unique constructor used by {@link ServerGrid#loadFrom}
     * @param preload
     */
    protected ServerMatrix(ServerGrid parent, int preload) {
        this.globalGrid = parent;
        this.nodes = new NodeMap(new Object2ObjectOpenHashMap<>(preload));
    }

    /**
     * Unique constructor used by {@link #splitDiscontinuities()}
     */
    private ServerMatrix(ServerMatrix original, @Nullable NodeMap newContents) {
        Objects.requireNonNull(original);
        if(original.globalGrid == null) 
            throw new IllegalArgumentException("Tried to clone a ServerMatrix from one with bad data!");
        this.globalGrid = original.globalGrid;
        this.index = original.index;
        this.nodes = newContents == null ? new NodeMap() : newContents;
        this.globalGrid.matrices.set(index, this);
        // TOOD find some way to do this non-iteratively
        for(GridNode node : nodes)
            node.swapOwner(this);
    }

    /**
     * Adds this ServerMatrix to the given {@link ServerGrid}.
     * Useful for situations where constructor functionality
     * needs to be deferred. This is used by {@link ServerGrid#loadFrom}
     * to prevent modifications being made in cases where deserialization
     * fails.
     * @param parent
     */
    public void loadInto(ServerGrid parent) {
        Objects.requireNonNull(parent);
        Iterator<GridNode> nodeIterator = nodes.iterator();
        while(nodeIterator.hasNext()) {
            GridNode node = nodeIterator.next();
            if(node.getLinkCount() <= 0) {
                nodeIterator.remove();
                if(node.getAddress() == null) {
                    node.nullify(); 
                    continue;
                }
                SurrogateNode surrogate = node.getAddress().getSurrogate(parent.getWorld());
                if(surrogate != null) surrogate.forgetIfNeeded(parent.getWorld());
                node.nullify();
            }
        }
        if(nodes.isEmpty()) {
            destroy();
            return;
        }
        this.globalGrid = parent;
        this.index = globalGrid.matrices.size();
        globalGrid.matrices.add(this);
    }

    /**
     * Performs a Flood-Fill to locate discontinuities in this LocalMatrix's
     * underlying matrix. (https://en.wikipedia.org/wiki/Flood_fill) 
     * <p>
     * Calls to this method will <strong>not</strong> modify this LocalMatrix
     * in-place. Instead, a list of ServerMatrices is formed as a result of the 
     * discontinuities contained within this one.
     * @return List of new LocalMatrix instances. The list will be empty if this 
     * LocalMatrix contains no discontinuities.
     * @throws IllegalStateException if this LocalMatrix has been {@link ServerMatrix#destroy destroyed.}
     */
    public @Nullable List<ServerMatrix> splitDiscontinuities() {
        assertNotDestroyed();
        final Set<GridUUID> visited = new HashSet<>();
        final List<ServerMatrix> output = new ArrayList<>();
        nodes.forEach(node -> {
            if(visited.contains(node.getAddress())) return;
            NodeMap cluster = new NodeMap();
            floodFillRecurse(node, visited, cluster);
            if(cluster.size() > 1)
                output.add(new ServerMatrix(this, cluster));
        });
        return output;
    }

    // recursive implementation for the method ^^ up there
    private void floodFillRecurse(GridNode start, Set<GridUUID> visited, NodeMap clusterResult) {
        GridNode iteration = nodes.get(start);
        visited.add(start.getAddress());
        if(iteration == null || iteration.getLinkCount() <= 0) return;
        clusterResult.add(iteration);
        for(GridLink link : iteration) {
            GridNode adjacent = link.getEndNode();
            if(!visited.contains(adjacent.getAddress()))
                floodFillRecurse(adjacent, visited, clusterResult);
        }
    }

    /**
     * Performs a path traversal to create a {@link GridPath} 
     * connecting <code>start</code> and <code>end</code>. <p>
     * Uses the A* pathfinding algorithm:
     * https://en.wikipedia.org/wiki/A*_search_algorithm
     * @param start Address to begin searching from
     * @param end Address to search for
     * @return The resulting {@link GridPath} or null if no path could be found
     * @throws IllegalStateException if this LocalMatrix has been {@link ServerMatrix#destroy destroyed.}
     */
    public @Nullable GridPath findPathBetween(GridUUID start, GridUUID end) {
        assertNotDestroyed();

        if(start == null || !nodes.contains(start)) return null;
        if(end == null || !nodes.contains(end)) return null;
        if(start.equals(end)) return null;

        final Queue<HeuristicUUID> open = new PriorityQueue<>(11);
        final GridPath output = GridPath.makeProvisional();
        final ObjectOpenHashSet<HeuristicUUID> trackedNodes = new ObjectOpenHashSet<>();
        open.add(start.makeTrackable().estimateCostTo(getWorld(), end));

        while(!open.isEmpty()) {
            final HeuristicUUID local = open.poll();
            if(local.getAddress().equals(end)) return output;

            trackedNodes.add(local);
            local.markVisited();

            GridNode localNode = nodes.get(local.getAddress());
            if(localNode == null) {
                throw new IllegalStateException("Error encountered while finding path between " + start 
                    + " and " + end + " - Traversal at " + local.getAddress() + " returned null!");
            }

            for(GridLink adjacentLink : localNode) {
                if(!adjacentLink.canTraverse()) continue;
                HeuristicUUID neighbor = trackedNodes.get(adjacentLink.getEndNode());
                if(neighbor == null) {
                    neighbor = adjacentLink.getEndNode().getAddress().makeTrackable();
                    trackedNodes.add(neighbor);
                }
                if(local.investigateAcross(adjacentLink, neighbor)) {
                    output.add(adjacentLink);
                    if(!open.contains(neighbor))
                        open.add(neighbor);
                }
            }
        }
        return null;
    }

    /**
     * Retrieve every node in this LocalMatrix belonging to the given
     * Address, while ignoring that address's index. All {@link GridNode GridNodes} 
     * that point to the given address will be added to the returned list.
     * @param addr Address to get all occurances of
     * @return A list of all GridNode objects belonging to the given BlockPos
     * @throws IllegalStateException if this LocalMatrix has been {@link ServerMatrix#destroy destroyed.}
     */
    public List<GridNode> getAllOccurancesOf(GridUUID addr) {
        assertNotDestroyed();
        List<GridNode> output = new ArrayList<>(GridUUID.MAX_SHARED_OCCUPANCY);
        for(int x = 0; x < GridUUID.MAX_SHARED_OCCUPANCY; x++) {
            GridNode link = nodes.get(addr.indexedCopy(x));
            if(link == null) break;
            output.add(link);
        }
        return output;
    }

    /**
     * Merges the contents of the provided {@link NodeMap}
     * into this LocalMatrix.
     * @param otherNodes Nodes to add
     * @return <code>true</code> if this LocalMatrix was modified.
     */
    public boolean addAll(NodeMap otherNodes) {
        assertNotDestroyed();
        if(otherNodes.isEmpty()) return false;
        int oldSize = this.nodes.size();
        this.nodes.ensureCapacity(oldSize + otherNodes.size());
        otherNodes.forEach(node -> {
            this.nodes.add(node);
            node.swapOwner(this);
        });
        this.nodes.trim();
        return oldSize != this.nodes.size();
    }

    /**
     * Removes the node at the given non-indexed address.
     * Starts at index 0, and works up to the maximum expected
     * or provided number. 
     * @param address
     * @param expectedCount (Optional) How many nodes should be looked 
     * for at the non-indexed address. Defaults to {@link GridUUID#MAX_SHARED_OCCUPANCY}
     * @return <code>true</code> if this LocalMatrix was modified as a result
     * of this call.
     */
    public boolean destroy(GridUUID address) {
        return destroy(address, GridUUID.MAX_SHARED_OCCUPANCY);
    }

    /**
     * Removes the node at the given non-indexed address.
     * Starts at index 0, and works up to the maximum expected
     * or provided number. 
     * @param address
     * @param expectedCount (Optional) How many nodes should be looked 
     * for at the non-indexed address. Defaults to {@link GridUUID#MAX_SHARED_OCCUPANCY}
     * @return <code>true</code> if this LocalMatrix was modified as a result
     * of this call.
     */
    public boolean destroy(GridUUID address, int expectedCount) {
        assertNotDestroyed();
        Objects.requireNonNull(address);
        boolean modified = false;
        final Set<GridUUID> empties = new HashSet<>();
        for(int x = 0; x < expectedCount; x++) {
            GridNode removed = nodes.remove(address.indexedCopy(x));
            if(removed == null ) continue;
            isolate(removed, empties, true);
            modified = true;
        }
        // TODO increase specificity so that cleanup is called only when it needs to be
        if(modified) cleanup(empties, true);
        return modified;
    }

    /**
     * Removes all links from this ServerMatrix that involve the given node.
     * Fills <code>empties</code> with any GridUUIDs that have no links as a result
     * of this call.
     * @param node
     * @param empties A set of GridUUIDs to collect empty nodes. 
     */
    protected void isolate(GridNode node, Set<GridUUID> empties, boolean broadcast) {
        for(GridLink link : node) {
            GridNode destination = link.getEndNode();
            Iterator<GridLink> linksIterator = destination.iterator();
            while(linksIterator.hasNext()) {
                GridLink linkToTest = linksIterator.next();
                if(linkToTest.endsWith(node.getAddress())) {
                    linksIterator.remove();
                    if(broadcast) linkToTest.broadcast(globalGrid.getWorld(), GridResponse.TASK_DESTROY_LINK);
                }
            }
            if(!destination.hasLinks()) 
                empties.add(destination.getAddress());
        }
        node.broadcast(globalGrid.getWorld(), GridResponse.TASK_FORGET_ANCHORS);
        node.nullify();
    }

    /**
     * Removes a singular link at the given start and end points
     * @param start
     * @param end
     */
    public GridLink deLink(GridNode startNode, GridNode endNode, Set<GridUUID> empties) {
        GridLink removedStart = startNode == null ? null : removeLink(startNode, endNode.getAddress(), empties);
        GridLink removedEnd = endNode == null ? null : removeLink(endNode, startNode.getAddress(), empties);
        return removedStart == null ? removedEnd : removedStart;
    }

    private @Nullable GridLink removeLink(GridNode fromNode, GridUUID toAddress, @Nullable Set<GridUUID> empties) {
        if(!fromNode.hasLinks()) 
            return null;
        /**
         * I'm looping over the entire iterator here with no early return just in case there
         * are duplicates. For now, we can incur the cost of iterative stuff like this until 
         * the GridAPI is proven to be watertight.
         */
        final Iterator<GridLink> linksIterator = fromNode.iterator();
        GridLink firstRemoved = null;
        while(linksIterator.hasNext()) {
            GridLink link = linksIterator.next();
            if(!link.endsWith(toAddress))
                continue;
            if(firstRemoved != null) {
                linksIterator.remove();
                continue;
            }
            linksIterator.remove();
            firstRemoved = link;
        }
        if(firstRemoved == null) return null;
        if(!fromNode.hasLinks())
            empties.add(fromNode.getAddress());
        if(!firstRemoved.getEndNode().hasLinks()) 
            empties.add(firstRemoved.getEndNode().getAddress());
        return firstRemoved;
    }

    /**
     * Removes transient empty nodes and performs conditional dirty nullification
     * depending on the amount of {@link GridNode GridNodes} that this matrix
     * has in it. This method is useful to call at the end of operations that
     * modify the {@link NodeMap node map}, as a means to remove 
     * @param forRemoval Nodes that will be removed. Usually, this will just 
     * be a set of empty nodes that were accumulated by previous calls. 
     * @param split <code>true</code> if the matrix is suspected to have 
     * {@link #splitDiscontinuities() discontinuities} that need to be split.
     */
    public void cleanup(@Nullable Set<GridUUID> forRemoval, boolean split) {
        assertNotDestroyed();
        if(forRemoval != null) {
            for(GridUUID addr : forRemoval) {
                GridNode node = nodes.remove(addr);
                if(node == null) continue;
                node.broadcast(globalGrid.getWorld(), GridResponse.TASK_FORGET_ANCHORS);
                node.nullify();
            }
        }

        // if this matrix only has one (or zero) node(s), destory this matrix
        if(nodes == null || nodes.size() < 2) {
            for(GridNode node : nodes) {
                if(node == null) continue;
                node.broadcast(globalGrid.getWorld(), GridResponse.TASK_FORGET_ANCHORS);
                node.nullify();
            }
            globalGrid.destroyMatrix(this);
            return;
        }
        // if this matrix has 4 or more nodes, try to split it
        if(split && this.nodes.size() > 3) {
            final List<ServerMatrix> clusters = splitDiscontinuities();
            if(clusters.size() > 1) {
                globalGrid.destroyMatrix(this);
                globalGrid.addAll(clusters);
                this.destroy();
            } 
        }
    }

    public int getIndex() {
        return index;
    }

    /**
     * Only called by the {@Link ServerGrid}
     * when {@link ServerGrid#updateGridIndices 
     * operations shift the array. }
     * @param index
     */
    protected void setIndex(int index) {
        this.index = index;
    }

    /**
     * Nullifies references in this LocalMatrix for when it is removed.<p>
     * Note that this method does <strong>NOT</strong> broadcast
     * changes or do any syncing - This method is specifically
     * to mark grids as stale so they aren't used anymore.
     * <p>
     * If this method is called on a LocalMatrix that's actively being
     * used, all hell will break lose.
     */
    public void destroy() {
        index = -1;
        nodes = null;
    }

    public void assertNotDestroyed() {
        assertNotDestroyed("An operation attempted to run on a LocalMatrix that has already been destroyed. (A LocalMatrix was probably leaked!)");
    }

    protected boolean isDestroyed() {
        return nodes == null || globalGrid == null || globalGrid.getWorld() == null;
    }

    public void assertNotDestroyed(String message) {
        if(nodes == null) 
            throw new IllegalStateException(message + " (Caused by: Nullification of internal node matrix)");
        if(globalGrid == null || globalGrid.getWorld() == null) 
            throw new IllegalStateException(message + " (Caused by: Destruction of parent ServerGrid)");
    }

    @Override
    public @Nullable ServerLevel getWorld() {
        return globalGrid == null ? null : (ServerLevel)globalGrid.getWorld();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ServerMatrix that)) return false;
        if(this.isDestroyed() && that.isDestroyed())
            return this.index == that.index;
        return this.index == that.index && Worldly.areWorldsEqual(this.getWorld(), that.getWorld());
    }

    @Override
    public int hashCode() {
        return index;
    }

    @Override
    public String toString() {
        return "ServerMatrix[index " + this.index + ", dim '" + this.globalGrid == null ? "N/A" : this.globalGrid.getDimensionName() + "']";
    }
}