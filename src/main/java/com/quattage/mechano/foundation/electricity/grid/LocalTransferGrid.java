package com.quattage.mechano.foundation.electricity.grid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GID;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridEdge;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridPath;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridVertex;
import com.quattage.mechano.foundation.electricity.grid.network.GridSyncHelper;
import com.quattage.mechano.foundation.electricity.grid.network.GridSyncPacketType;
import com.quattage.mechano.foundation.helper.VectorHelper;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.createmod.catnip.theme.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

/**
 * The LocalTransferGrid provides the framework for managing, caching, finding, and updating GridVertices, GridPaths, and GridEdges.
 * <p>
 * For clarification: 
 *    - GridVertex -> Connector, or a wire source/destination
 *    - GridEdge -> The wire itself
 *    - GridPath -> A chain of wires connecting a power creator to a power consumer
 * <p>
 * This object stores GridVertex objects in a Map. This structure defines a 
 * (sort of) simple implementation of an undirected graph defined by a hashed adjacency list.
 * More info on graph theory: 
 * https://en.wikipedia.org/wiki/Graph_theory
 * https://en.wikipedia.org/wiki/Adjacency_list
 * and also: https://en.wikipedia.org/wiki/A*_search_algorithm
 */
public class LocalTransferGrid {

    private final Map<GID, GridVertex> vertMatrix = new Object2ObjectOpenHashMap<>();
    private final TransferPathManager pathManager = new TransferPathManager();

    private final GlobalTransferGrid parent;

    /**
     * Instantiates a blank LocalTransferGrid
     */
    public LocalTransferGrid(GlobalTransferGrid parent) {
        this.parent = parent;
    }

    /**
     * Creates a new shallow-copied LocalTransferGrid which contains the contents of both provided LocalTransferGrids. <p>
     * <strong>Important note:</strong> The resulting grid will contain discontinuities, as there there is no guarantee that
     * there are any connections, inferered or otherwise, between both provided grids.
     * Discontinuities <strong>must</strong> be dealt with by ASAP creating at least one 
     * connection between the disconnected clusters within the resulting LocalTransferGrid object.
     * @param parent The GlobalTransferGrid parent (usually the object that is initiating this merge)
     * @param sync If <code>TRUE</code>, all GridVertices will be synced to their host BlockEntities before being added to the resulting LocalTransferGrid. This is rather expensive, and shouldn't be used very often.
     * @param grids Any amount of LocalTransferGrids to pull matrix values from - The resulting grid will contain all vertices and edges, but no paths.
     * @return a new LocalTransferGrid object.
     */
    public static LocalTransferGrid ofMerged(GlobalTransferGrid parent, boolean sync, LocalTransferGrid... grids) {
        LocalTransferGrid out = new LocalTransferGrid(parent);
        
        for(LocalTransferGrid grid : grids) {
            for(GridVertex vert : grid.allVerts()) {
                vert.replaceParent(out);
                if(sync) vert.doFullSync(false);
                out.addVert(vert);
            }
            collectPaths(grid, out);
        }

        return out;
    }

    /**
     * Copies all paths from <code>source</code> to <code>destination</code> conditionally.
     * A path is said to "belong" to a LocalTransferGrid if its starting and ending vertices 
     * actually exist in said LocalTransferGrid. The destination LocalTransferGrid must contain
     * these vertices in order for the associated path to be added. If this is not the case, the path is 
     * simply skipped.
     * @param source LocalTransferGrid to copy from. This LocalTransferGrid's paths are not affected.
     * @param destination LocalTransferGrid to copy to. This LocalTransferGrid's paths will be added to.
     */
    public static void collectPaths(LocalTransferGrid source, LocalTransferGrid destination) {
        TransferPathManager pathsToCollect = source.pathManager;
        TransferPathManager pathsToPut = destination.pathManager;
        for(Set<GridPath> pathSet : pathsToCollect.getAll().values()) {
            for(GridPath path : pathSet) {
                if(destination.hasVertAt(path.getStart().getID()) && destination.hasVertAt(path.getEnd().getID()))
                    pathsToPut.skipUpdates().putPath(path);
            }
        }
    }

    /***
     * Populates this LocalTransferGrid with members of a list
     * No pathfinding is performed as a result of this call, that must be done seperately
     * @param cluster ArrayList of GridVertexs to add to this LocalTransferGrid upon creation
     */
    public LocalTransferGrid(GlobalTransferGrid parent, ArrayList<GridVertex> cluster) {
        this.parent = parent;
        for(GridVertex vert : cluster) {
            vert.replaceParent(this);
            vertMatrix.put(vert.getID(), vert);
        }
    }

    public LocalTransferGrid(GlobalTransferGrid parent, CompoundTag in, Level world) {
        this.parent = parent;
        ListTag net = in.getList("nt", Tag.TAG_COMPOUND);
        for(int x = 0; x < net.size(); x++) {

            CompoundTag vertTag = net.getCompound(x);

            // Whenever we add a new GridVertex we first check if it already exists, 
            // since it could've been created by a previous iteration.
            GridVertex vert = getVertAt(GID.of(vertTag));
            if(vert == null) 
                vert = new GridVertex(world, this, vertTag);
            else 
                vert.readRetroactive(world, vertTag);

            // if this GridVertex has bad data, don't add it
            if(vert.hasNoHost()) {
                Mechano.LOGGER.warn("LocalTransferGrid skipping registration of GridVertex at " + vert.getID());
                continue;
            }

            vert.doSimplifiedSync();
            vertMatrix.put(vert.getID(), vert);
        }

        findAllPaths(false);
    }

    //  Write this LocalTransferGrid to NBT.  //
    protected CompoundTag writeTo(CompoundTag in) {
        ListTag out = new ListTag();
        for(GridVertex v : vertMatrix.values())
            out.add(v.writeTo(new CompoundTag()));
        in.put("nt", out);
        return in;
    }

    /**
     * @return <code>TRUE</code> if this LocalTranferGrid contains a GridVertex 
     * mapped to the given GID.
     */
    protected boolean hasVertAt(GID id) {
        return vertMatrix.containsKey(id);
    }

    /**
     * Adds the provided vertex to this LocalTransferGrid's vertex matrix
     * @throws NullPointerException if the provided GridVertex is null
     * @param vertex GridVertex to add
     * @return <code>TRUE</code> if this LocalTransferGrid was modified as a result of this call
     */
    protected boolean addVert(GridVertex vertex) {
        if(vertex == null) 
            throw new NullPointerException("Error adding vertex to LocalTransferGrid - Cannot store a null vertex!");
        return vertMatrix.put(vertex.getID(), vertex) == null;
    }

    /**
     * Removes the vertex at the provided GID from this LocalTransferGrid's vertex matrix
     * and performs additional operations to remove outdated edge and path data. 
     * @param id ID to remove
     * @return The GridVertex that was removed, or null if none could be found.
     */
    protected GridVertex popVert(GID id) {
        if(id == null) throw new NullPointerException("Error removing GridVertex - The provided GID is null!");
        
        // pop the vertex and return if none could be found
        GridVertex poppedVert = vertMatrix.remove(id);
        if(poppedVert == null) return null;

        // if the vertex is empty, just mark it and send packets
        if(poppedVert.isEmpty()) {
            poppedVert.markRemoved();
            GridSyncHelper.informPlayerVertexUpdate(GridSyncPacketType.REMOVE, id);
            return poppedVert;
        }

        // remove all paths that interact with this vertex
        pathManager.withUpdates().removeAllPathsInvolving(poppedVert.getID());

        // tell neighboring vertices to "forget" this vertex
        for(GridEdge linkedEdge : poppedVert.links) {
            GridVertex neighbor = linkedEdge.getDestinationVertex();
            GridEdge poppedEdge = neighbor.popLink(id);
            if(poppedEdge != null) {
                GridSyncHelper.informPlayerEdgeUpdate(GridSyncPacketType.REMOVE, poppedEdge.toLightweight());
                if(neighbor.isEmpty()) popVert(neighbor.getID());
            }
        }

        // mark the vertex and send packets
        poppedVert.markRemoved(); 
        GridSyncHelper.informPlayerVertexUpdate(GridSyncPacketType.REMOVE, id);
        return poppedVert;
    }

    /***
     * Creates a link (edge) between two GridVertices
     * @throws NullPointerException If either provided GridVertex
     * does not exist within this LocalTransferGrid.
     * @param first GridVertex to link
     * @param second GridVertex to link
     * @param edgeType The type of edge to create - Inidcates wire transfer characteristics
     * @param shouldPath if <code>TRUE</code> paths will be re-addressed after this link is added.
     * @return True if the GridVertices were modified as a result of this call,
     */
    protected boolean linkVerts(GridVertex first, GridVertex second, int edgeType, boolean shouldPath) {
        requireValidVertex("Failed to link GridVertices", first, second);
        if(first.addLink(second, edgeType) && second.addLink(first, edgeType)) {
            first.doFullSync(false);
            second.doFullSync(false);
            if(shouldPath) findAllPaths(true);
            return true;
        }
        return false;
    }

    /***
     * Creates a link (edge) between two GridVertices at the provided GIDs
     * @throws NullPointerException If either provided GID doesn't 
     * indicate the location of a GridVertex in this LocalTransferGrid.
     * @param first GID to locate and link
     * @param second GID object to locate and link
     * @param edgeType The type of edge to create - Inidcates wire transfer characteristics
     * @param shouldPath if <code>TRUE</code> paths will be re-addressed after this link is added.
     * @return True if the GridVertices were modified as a result of this call,
     */
    protected boolean linkVerts(GridVertex first, GridVertex second, int edgeType) {
        return linkVerts(first, second, edgeType, true);
    }

    /***
     * Creates a link (edge) between two GridVertices at the provided GIDs
     * @throws NullPointerException If either provided GID doesn't 
     * indicate the location of a GridVertex in this LocalTransferGrid.
     * @param first GID to locate and link
     * @param second GID object to locate and link
     * @param edgeType The type of edge to create - Inidcates wire transfer characteristics
     * @param shouldPath if <code>TRUE</code> paths will be re-addressed after this link is added.
     * @return True if the GridVertices were modified as a result of this call,
     */
    protected boolean linkVerts(GID first, GID second, int edgeType, boolean shouldPath) {
        requireValidID("Failed to link GridVertices", first, second);
        GridVertex vertF = getVertAt(first);
        GridVertex vertT = getVertAt(second);
        if(vertF.addLink(vertT, edgeType) && vertT.addLink(vertF, edgeType)) {
            vertF.doFullSync(false);
            vertT.doFullSync(false);
            if(shouldPath) findAllPaths(true);
            return true;
        }
        return false;
    }

    /***
     * Creates a link (edge) between two GridVertices at the provided GIDs
     * @throws NullPointerException If either provided GID doesn't 
     * indicate the location of a GridVertex in this LocalTransferGrid.
     * @param first GID to locate and link
     * @param second GID object to locate and link
     * @param shouldPath if <code>TRUE</code> paths will be re-addressed after this link is added.
     * @return True if the GridVertices were modified as a result of this call,
     */
    public boolean linkVerts(GID first, GID second, int wireType) {
        return linkVerts(first, second, wireType, true);
    }

    /***
     * Performs a DFS to determine all of the different "clusters"
     * that form this LocalTransferGrid. Individual vertices that are found to 
     * possess no connections are discarded, and are not included in the 
     * resulting clusters. <strong>Does not modify this system in-place.</strong>
     * 
     * @return ArrayList of LocalTransferGrids formed from the individual clusters 
     * within this LocalTransferGrid.
     */
    protected List<LocalTransferGrid> trySplit() {
        HashSet<GID> visited = new HashSet<>();
        List<LocalTransferGrid> clusters = new ArrayList<LocalTransferGrid>();

        for(GID identifier : vertMatrix.keySet()) {
            if(visited.contains(identifier)) continue;
            ArrayList<GridVertex> clusterVerts = new ArrayList<>();
            depthFirstPopulate(identifier, visited, clusterVerts);
            if(clusterVerts.size() > 1) {
                LocalTransferGrid clusterResult = new LocalTransferGrid(parent, clusterVerts);
                LocalTransferGrid.collectPaths(this, clusterResult);
                clusters.add(clusterResult);
            }
        }
        return clusters;
    }

    /**
     * The recursive DFS initiated by {@link LocalTransferGrid#trySplit() <code>trySplit()</code>} is performed here.
     */
    private void depthFirstPopulate(GID startID, HashSet<GID> visited, ArrayList<GridVertex> vertices) {
        GridVertex thisIteration = getVertAt(startID);
        visited.add(startID);

        if(thisIteration == null) return;
        vertices.add(thisIteration);

        if(thisIteration.isEmpty()) return;
        for(GridEdge link : thisIteration.links) {
            GID currentID = link.getDestinationVertex().getID();
            if(!visited.contains(currentID))
                depthFirstPopulate(currentID, visited, vertices);
        }
    }

    /***
     * Retrieves a vertex in this network.
     * @param BlockPos
     * @return GridVertex at the given BlockPos
     */
    public GridVertex getVertAt(GID pos) {
        return vertMatrix.get(pos);
    }

    /***
     * Finds the optimal path between the given GridVertex and all other GridVertices marked 
     * as members and stores those GridPath objects in this LocalTransferGrid.
     * @param origin GridVertex to find paths from/to. All resulting paths start or end with this GridVertex.
     * @param shouldUpdate If <code>TRUE</code>, each new path that is found will call {@link LocalTransferGrid#onPathsUpdated(GridPath, boolean) <code>onPathsUpdated()</code>}
     * @param forceOpportunities If <code>TRUE</code> the member status of each involved GridVertex will be manually re-addressed. This process is more expensive but is required
     * in some scenarios.
     * @return <code>TRUE</code> if this LocalTransferGrid was modified as a result of this call. (if any paths were found)
     */
    public boolean pathfindFrom(GridVertex origin, boolean shouldSendPackets, boolean forceOpportunities) {
        boolean exists = false;
        if(forceOpportunities) origin.doFullSync(false);
        for(GridVertex vert : vertMatrix.values()) {

            if(forceOpportunities) vert.doFullSync(false);
            if(!vert.canFormPathTo(origin)) continue;

            GridPath path = astar(origin, vert);
            if(path != null) {
                exists = true;
                pathManager.shouldUpdate(shouldSendPackets).putPath(path);
            }
        }

        return exists;
    }

    /***
     * Finds optimal paths between every relevent GridVertex in this grid.
     * @param shouldUpdate If <code>TRUE</code>, each new path that is found will send packets to sync.
     * @return <code>TRUE</code> if the LocalTransferGrid was modified as a result of this call.
     */
    public boolean findAllPaths(boolean shouldSendPackets) {        
        boolean exists = false;
        for(GridVertex vert : vertMatrix.values()) {
            if(!vert.isMember()) continue;
            if(pathfindFrom(vert, shouldSendPackets, false)) exists = true;
        }

        return exists;
    }

    /***
     * Forms a path between <code>start</code> and <code>goal</code>
     * by leaping between connected vertices to the destination.
     * @param start GridVertex to start from
     * @param end GridVertex the algorithm is looking for
     * @return GridPath object representing the optimal path between 
     * the two vertices, or null if no path could be fouund.
     */
    @Nullable
    private GridPath astar(GridVertex start, GridVertex goal) {

        final Queue<GridVertex> openVerts = new PriorityQueue<>(11, GridVertex.GUIDANCE_COMPARATOR);
        final Map<GridVertex, GridVertex> outputPath = new HashMap<>();
        final Set<GridVertex> addressedVertices = new HashSet<>();

        float resultingTransferRate = Float.MAX_VALUE;

        start.setCumulative(0);
        start.getAndStoreHeuristic(goal);
        openVerts.add(start);

        while(!openVerts.isEmpty()) {
            final GridVertex local = openVerts.poll();

            // loop terminates here if successful
            if(local.equals(goal)) {
                resetPathData(addressedVertices);
                return GridPath.ofUnwound(outputPath, goal, resultingTransferRate);
            }

            local.markVisited();

            for(GridEdge potentialTraverse : local.links) {

                if(!potentialTraverse.canTransfer()) continue;
                GridVertex neighbor = potentialTraverse.getDestinationVertex();
                if(neighbor.hasBeenVisited()) continue;

                // As the path is traversed, calculate the lowest transfer rate of every adge addressed,
                // and store heuristics in the currently addressed edge
                resultingTransferRate = Math.min(resultingTransferRate, potentialTraverse.getMaximumWatts());
                float tentative = local.getAndStoreFastHeuristic(potentialTraverse) + local.getCumulative();
                addressedVertices.add(local);

                // if the tentative is less than the cumulative (that is, if we have points to spend here) address this path
                // TODO take transfer rate into accout when calculating heuristic to prioritize edges with less resistance
                if(tentative < neighbor.getCumulative()) {

                    neighbor.setCumulative(tentative);
                    neighbor.getAndStoreFastHeuristic(potentialTraverse);
                    addressedVertices.add(neighbor);

                    outputPath.put(neighbor, local);
                    if(!openVerts.contains(neighbor)) 
                        openVerts.add(neighbor);
                }
            }
        }
        resetPathData(addressedVertices);
        return null;
    }

    // resets A* guidance (see above for context)
    private void resetPathData(Set<GridVertex> verts) {
        for(GridVertex vert : verts)
            vert.resetHeuristics();
    }

    /***
     * Gets the GridVertex at the given ID, or creates a new one & adds it to this 
     * LocalTransferGrid, and returns it. To be called during the loading process.<p>
     * <strong>This method is public because it is called by the GridVertex's constructor, but
     * it is not designed to be called in any other context.</strong>
     * @param world World to operate within
     * @param id ID of GridVertex to find
     * @return The GridVertex that was found or created as a result of this call.
     */
    public GridVertex getOrCreateOnLoad(Level world, GID id) {
        
        GridVertex target = getVertAt(id);
        if(target != null) return target;

        target = new GridVertex(world, this, id);
        addVert(target);
        return target;
    }

    public Color getDebugColor() {
        return VectorHelper.toColor(((GridVertex)vertMatrix.values().toArray()[0]).getID().getBlockPos().getCenter());
    }

    public GlobalTransferGrid getParent() {
        return parent;
    }

    /**
     * @return Int representing how many GridVertices are in this LocalTransferGrid
     */
    public int size() {
        return vertMatrix.size();
    }

    /**
     * @return <code>TRUE</code> if this LocalTransferGrid has no GridVertices
     */
    public boolean isEmpty() {
        return vertMatrix.isEmpty();
    }

    public Collection<GridVertex> allVerts() {
        return vertMatrix.values();
    }

    public String toString() {
        
        String output = "\nAll grid data:\n";
        
        int x = 1;
        for(GridVertex vert : vertMatrix.values()) {
            String vertDescriptor = "\tVertex " + x + ": " + vert + "\n";

            for(GridEdge edge : vert.links)
                vertDescriptor += "\t\t> " + edge.getDestinationVertex() + "\n";

            output += vertDescriptor + "\n";
            x++;
        }

        return output;
    }

    /** 
     * Callable by debuggers to produce formatted text for minecraft chat
     * @param target BlockPos, usually of the targeted vertex. Vertices at this BlockPos will
     * be marked with a <code>'>>>'</code> in the resulting string.
     */
    public String toFormattedString(@Nullable BlockPos target) {
        String output = "\n";
        int x = 1;

        if(vertMatrix.size() == 0) {
            return "\n            §6this grid is... blank?? §lhow did you do that";
        }

        for(GridVertex vert : vertMatrix.values()) {
            if(vert.getID().getBlockPos().equals(target))
                output += ("    §6§l>>>  §r§7Vertex §r§b§l" + x + ": §r§7") + vert.toFormattedString() + "\n";
            else output += ("            §r§7Vertex §r§b§l" + x + ": §r§7") + vert.toFormattedString() + "\n";
            x++;
        }
        return output;
    }

    public TransferPathManager getPathManager() {
        return pathManager;
    }

    /////////////////////// sanity check helpers ///////////////////////////////////////////////////////
    private void requireValidID(String failMessage, GID... idSet) {
        for(GID id : idSet) {
            if(id == null) 
                throw new NullPointerException(failMessage + " - The provided SystemLink is null!");
            if(!vertMatrix.containsKey(id))
                throw new NullPointerException(failMessage + " - No valid GridVertex matching SystemID " 
                + id + " could be found!");
        }
    }

    private void requireValidVertex(String failMessage, GridVertex... vertexSet) {
        for(GridVertex vertex : vertexSet) {
            if(vertex == null)
                throw new NullPointerException(failMessage + " - The provided GridVertex is null!");
            if(!vertMatrix.containsValue(vertex))
                throw new NullPointerException(failMessage + " - The provided GridVertex does not exist in this LocalTransferGrid!");
        }       
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////
}