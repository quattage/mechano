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

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.anchor.AnchorPointable;
import com.quattage.mechano.foundation.api.landmark.GridLink;
import com.quattage.mechano.foundation.api.landmark.GridNode;
import com.quattage.mechano.foundation.api.landmark.GridPath;
import com.quattage.mechano.foundation.api.landmark.NodeMap;
import com.quattage.mechano.foundation.api.landmark.classifier.GridUUID;
import com.quattage.mechano.foundation.api.landmark.classifier.HeuristicUUID;
import com.quattage.mechano.foundation.api.switchboard.LinkResponsePacket;
import com.quattage.mechano.foundation.api.switchboard.Response;
import com.quattage.mechano.foundation.api.switchboard.Response.LinkResponseHolder;
import com.quattage.mechano.foundation.api.transmitter.MechanoTransmissionTypes;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;

/**
 * Represents a single localized cluster made of nodes and links,
 * where each node has continuity with every other node in the cluster.
 */
public class ServerMatrix {

    protected ServerGrid global;
    public int gridIndex = -1;
    public NodeMap nodes;

    protected ServerMatrix(ServerGrid parent, int preload) {
        Objects.requireNonNull(parent);
        this.gridIndex = parent.matrices.size();
        this.nodes = new NodeMap(new Object2ObjectOpenHashMap<>(preload));
        this.global = parent;
        this.global.matrices.add(this);
    }

    protected ServerMatrix(int preload) {
        this.nodes = new NodeMap(new Object2ObjectOpenHashMap<>(preload));
    }

    public ServerMatrix(ServerGrid parent, @Nullable NodeMap newContents) {
        Objects.requireNonNull(parent);
        this.gridIndex = parent.matrices.size();
        this.global = parent;
        this.nodes = newContents == null ? new NodeMap() : newContents;
        this.global.matrices.add(this);
    }

    public ServerMatrix(ServerMatrix original, @Nullable NodeMap newContents) {
        Objects.requireNonNull(original);
        this.global = original.global;
        this.gridIndex = original.gridIndex;
        this.nodes = newContents == null ? new NodeMap() : newContents;
        this.global.matrices.set(gridIndex, this);
    }

    /**
     * Gets the node at the given address, or create a new one if
     * no node at this address exists. This method is mainly designed
     * to be used during the loading process defined in {@link ServerGrid#makeProvisionalNodeAndLinks}
     * <p>
     * Note that if the returned GridNode is newly created, it will be blank. 
     * Blank GridNodes that have no links should not persist in the LocalMatrix 
     * for long, since they represent dead ends.
     * @param address Address to get or add (Compatable with any type outlined by {@link NodeMap#get})
     * @return The GridNode at this address, or the new one that was created at the specified address. Will be null if there is no PGBE at the address.
     * @throws IllegalStateException if this LocalMatrix has been {@link ServerMatrix#destroy destroyed.}
     */
    public @Nullable GridNode getOrCreateProvisional(LevelReader world, GridUUID address, boolean log) {
        assertNotDestroyed();
        GridNode node = nodes.get(address);
        if(node != null) return node;
        AnchorPointable<?> points = address.getAnchorPoints(world);
        if(points == null) {
            if(log) Mechano.LOGGER.error("Failed to instantiate provisional node at " + address 
                + " - No in-world reference to this address could be found!");
            return null;
        }
        node = new GridNode(this, points, address);
        points.getSurrogate().sync(world, this);
        this.nodes.add(node);
        return node;
    }

    /**
     * Splits discontinuities and removes empty or stale {@link GridNode}
     * instances from this LocalMatrix. The instance that this is run on will be stale,
     * and a new list of instnaces will be added to the {@link GlobalTransferGrid}
     * that this local belongs to.
     */
    public void cleanup(boolean deepClean) {
        if(global == null) {
            if(nodes == null) {
                Mechano.LOGGER.warn("Attempted to run cleanup on a ServerMatrix that has already been disposed!");
                return;
            }
            for(GridNode node : nodes) {
                node.notifyHost();
                node.nullify();
            }
            this.nullify();
            global.destroyMatrix(this);
            return;
        }

        if(nodes == null || nodes.isEmpty()) {
            this.nullify();
            global.destroyMatrix(this);
            return;
        }

        if(nodes.size() < 2) {
            for(GridNode node : nodes) {
                node.notifyHost();
                node.nullify();
            }
            this.nullify();
            global.destroyMatrix(this);
            return;
        }

        if(this.nodes.size() > 3) {
            List<ServerMatrix> clusters = splitDiscontinuities();
            if(clusters.size() > 1) {
                global.destroyMatrix(this);
                global.addAll(clusters);
                this.nullify();
            }
        }
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
        iteration.forEachLink(link -> {
            GridNode adjacent = link.getEndNode();
            if(!visited.contains(adjacent.getAddress()))
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

            localNode.forEachLink(adjacentLink -> {
                if(!adjacentLink.canTraverse()) return;
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
            });
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
        });
        return oldSize != this.nodes.size();
    }

    /**
     * Clears this LocalMatrix, erasing its matrix and resizing its hash table.
     * Broadcasts updates as a result.
     */
    public void clear() {
        assertNotDestroyed();
        Iterator<GridNode> it = nodes.iterator();
        while (it.hasNext()) {
            GridNode node = it.next();
            node.wipeLinks(true);
            node.nullify();
            it.remove();
        }
        nodes.trim();
        nullify();
    }

    /**
     * Removes the node at the given address from this LocalMatrix.
     * If this LocalMatrix is empty as a result of this call, this
     * method will also remove and destroy this LocalMatrix.
     * @param address
     * @return <code>true</code> if this LocalMatrix was modified as a result
     * of this call.
     */
    public boolean removeNode(GridUUID address) {
        assertNotDestroyed();        

        // remove the node in question
        GridNode removed = nodes.get(address);
        if(address == null) return false;
        nodes.remove(removed.getAddress());
        if(removed.getLinkCount() <= 0) {
            if(nodes != null && nodes.isEmpty())
                global.destroyMatrix(this);
            removed.nullify();
            return true;
        }

        boolean requiresCleaning = removed.getLinkCount() > 1;

        // remove links symmetrically while tracking changes
        removed.forEachLink(link -> {
            if(link == null) return;
            GridNode endNode = link.getEndNode();
            Iterator<GridLink> linksIter = endNode.iterator();
            while(linksIter.hasNext()) {
                GridLink linkToRemove = linksIter.next();
                if(linkToRemove.endsWith(address)) {
                    linksIter.remove();
                    linkToRemove.removeFrom(getWorld());
                    CatnipServices.NETWORK.sendToAllClients(
                        new LinkResponsePacket(
                            link.getStart(), link.getEnd(), 
                            LinkResponseHolder.of(link, Response.SUCCESS), 
                            MechanoTransmissionTypes.PERFECT_CONDUCTOR, Response.Task.DESTROY
                        ));
                    removeIfEmpty(linkToRemove.getStartNode());
                    removeIfEmpty(linkToRemove.getEndNode());
                }
            }
        });
        if(requiresCleaning) cleanup(true);
        removed.nullify();
        return true;
    }

    private void removeIfEmpty(GridNode node) {
        if(node.getLinkCount() <= 0) {
            node.getAnchorPoints().getSurrogate().forget(getWorld());
            nodes.remove(node.getAddress());
            if(node.getOwner().nodes.isEmpty())
                global.destroyMatrix(node.getOwner());
            node.nullify();
        }
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
    public void nullify() {
        gridIndex = -1;
        nodes = null;
    }

    private void assertNotDestroyed() {
        if(nodes == null) 
            throw new IllegalStateException("An operation attempted to run on a LocalMatrix that has already been destroyed. (A LocalMatrix was probably leaked!)");
    }

    public ServerLevel getWorld() {
        assertNotDestroyed();
        return (ServerLevel)global.getWorld();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ServerMatrix that)) return false;
        return this.gridIndex == that.gridIndex;
    }
}