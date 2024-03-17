package com.quattage.mechano.foundation.electricity.power;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.power.features.GID;
import com.quattage.mechano.foundation.electricity.power.features.GIDPair;
import com.quattage.mechano.foundation.electricity.power.features.GridClientEdge;
import com.quattage.mechano.foundation.electricity.power.features.GridEdge;
import com.quattage.mechano.foundation.electricity.power.features.GridPath;
import com.quattage.mechano.foundation.electricity.power.features.GridVertex;
import com.quattage.mechano.foundation.helper.VectorHelper;
import com.simibubi.create.foundation.utility.Color;
import com.quattage.mechano.foundation.network.GridSyncPacketType;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

/***
 * A LocalGrid stores GridVertices in an undirected graph, forming an adjacency list.
 * Connections between these vertices are stored in two ways: <p>
 * - Each GridVertex contains a LinkedList of vertices that said vertex is connected to <p>
 * - Each LocalTransferGrid contains a HashMap of each edge for direct access. <p>
 */
public class LocalTransferGrid {

    private int memberCount = 0;
    
    private Map<GID, GridVertex> vertMatrix = new Object2ObjectOpenHashMap<>();
    private Map<GIDPair, GridEdge> edges = new Object2ObjectOpenHashMap<>();
    private Map<GIDPair, GridPath> paths = new Object2ObjectOpenHashMap<>();

    private final GlobalTransferGrid parent;

    // private int netEnergyPushed = 0;
    // private int netEnergyPulled = 0;

    /***
     * Instantiates a blank LocalTransferGrid
     */
    public LocalTransferGrid(GlobalTransferGrid parent) {
        this.parent = parent;
    }

    /***
     * Populates this LocalTransferGrid with members of a list
     * @param cluster ArrayList of GridVertexs to add to this LocalTransferGrid upon creation
     */
    public LocalTransferGrid(GlobalTransferGrid parent, ArrayList<GridVertex> cluster,  Map<GIDPair, GridEdge> edgeMatrix) {
        for(GridVertex vertex : cluster)
            vertMatrix.put(vertex.getGID(), vertex);
        this.edges = ((Object2ObjectOpenHashMap<GIDPair, GridEdge>)edgeMatrix).clone();
        this.parent = parent;
    }

    public LocalTransferGrid(GlobalTransferGrid parent, CompoundTag in, Level world) {
        this.parent = parent;
        ListTag net = in.getList("nt", Tag.TAG_COMPOUND);
        for(int x = 0; x < net.size(); x++) {

            CompoundTag vertC = net.getCompound(x);

            // if the grid vertex doesn't exist, new it. if it does, add edges to it.
            boolean newVert = true;
            GridVertex n = getVert(GID.of(vertC));
            if(n == null) {
                n = new GridVertex(this, net.getCompound(x), world);
            } else {
                newVert = false;
                ListTag links = vertC.getList("l", Tag.TAG_COMPOUND);
                if(links == null || links.isEmpty()) continue;
                n.readLinks(links, world);
            }

            if(vertC.contains("m") && vertC.getBoolean("m"))
                n.setIsMember();

            BlockPos check = n.getPos();
            // preventative measure to stop vertices from being added if they have bad data
            if((!(world.getBlockEntity(check) instanceof WireAnchorBlockEntity)) || (n.isEmpty())) {
                Mechano.LOGGER.warn("LocalTransferGrid skipping registration of GridVertex at [" 
                + check.getX() + ", " + check.getY() + ", " + check.getZ() + "]");
                continue;
            }
            if(newVert) n.syncOnLoad();
            vertMatrix.put(n.getGID(), n);
        }

        ListTag edgeList = in.getList("ed", Tag.TAG_COMPOUND);
        for(int x = 0; x < edgeList.size(); x++) {
            GridEdge edge = new GridEdge(this.parent, edgeList.getCompound(x));
            edges.put(edge.getID(), edge);
        }

        findAllPaths(false);
    }

    protected CompoundTag writeTo(CompoundTag in) {
        in.put("nt", writeMatrix());
        in.put("ed", writeEdges());
        return in;
    }

    private ListTag writeMatrix() {
        ListTag out = new ListTag();
        for(GridVertex v : vertMatrix.values())
            out.add(v.writeTo(new CompoundTag()));
        return out;
    }

    private ListTag writeEdges() {
        ListTag out = new ListTag();
        for(GridEdge edge : edges.values())
            out.add(edge.writeTo(new CompoundTag()));
        return out;
    }

    public void getEdgesWithin(SectionPos section) {
        for(GridEdge edge : edges.values()) {
            if(edge == null) continue;
            
        }
    }

    protected boolean addVert(GridVertex vertex) {
        if(vertex == null) 
        throw new NullPointerException("Failed to add vertex to LocalTransferGrid - Cannot store a null vertex!");
        return vertMatrix.put(vertex.getGID(), vertex) != null;
    }

    public boolean removeVert(GID id) {
        if(id == null) throw new NullPointerException("Error removing GridVertex - The provided GID is null!");
        GridVertex poppedVert = vertMatrix.remove(id);
        if(poppedVert == null) return false;
        removePaths(poppedVert);

        for(GridVertex linked : poppedVert.links) {
            if(linked.unlinkFrom(poppedVert)) {
                GridEdge found = edges.remove(new GIDPair(linked.getGID(), id));
                if(found == null) continue;
                GridSyncDirector.informPlayerEdgeUpdate(GridSyncPacketType.REMOVE, found.toLightweight());
            }
        }

        GridSyncDirector.informPlayerVertexDestroyed(GridSyncPacketType.REMOVE, id);
        return true;
    }

    /***
     * Gets all vertices assigned to the given BlockPos
     * @param pos BlockPos to check
     * @return An array of SystemVerticies at this BlockPos
     */
    public ArrayList<GridVertex> getVertsAt(BlockPos pos) {
        ArrayList<GridVertex> out = new ArrayList<>();
        for(GridVertex vert : vertMatrix.values())
            if(vert.getPos().equals(pos)) out.add(vert);
        return out;
    }

    /***
     * @return True if this LocalTransferGrid contains the given GridVertex
     */
    public boolean contains(GridVertex vert) {
        return contains(vert.getGID());
    }

    /***
     * @return True if this LocalTransferGrid contains a GridVertex at the given GID
     */
    public boolean contains(GID id) {
        return vertMatrix.containsKey(id);
    }

    /***
     * Creates a link (edge) between two GridVertices
     * @throws NullPointerException If either provided GridVertex
     * does not exist within this LocalTransferGrid.
     * @param first GridVertex to link
     * @param second GridVertex to link
     * @return True if the GridVertices were modified as a result of this call,
     */
    public boolean linkVerts(GridVertex first, GridVertex second, int wireType, boolean shouldPath) {
        requireValidNode("Failed to link GridVertices", first, second);
        addEdge(first, second, wireType);
        if(first.linkTo(second) && second.linkTo(first)) {
            first.syncToHostBE();
            second.syncToHostBE();
            if(shouldPath) findAllPaths();
            return true;
        }
        return false;
    }

    public boolean linkVerts(GridVertex first, GridVertex second, int wireType) {
        return linkVerts(first, second, wireType, true);
    }

    /***
     * Creates a link (edge) between two GridVertices at the provided GIDs
     * @throws NullPointerException If either provided GID doesn't 
     * indicate the location of a GridVertex in this LocalTransferGrid.
     * @param first GID to locate and link
     * @param second GID object to locate and link
     * @return True if the GridVertices were modified as a result of this call,
     */
    public boolean linkVerts(GID first, GID second, int wireType, boolean shouldPath) {
        requireValidLink("Failed to link GridVertices", first, second);
        GridVertex vertF = vertMatrix.get(first);
        GridVertex vertT = vertMatrix.get(second);
        addEdge(first, second, wireType);
        if(vertF.linkTo(vertT) && vertT.linkTo(vertF)) {
            vertF.syncToHostBE();
            vertT.syncToHostBE();
            if(shouldPath) findAllPaths();
            return true;
        }
        return false;
    }

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
    public List<LocalTransferGrid> trySplit() {
        HashSet<GID> visited = new HashSet<>();
        List<LocalTransferGrid> clusters = new ArrayList<LocalTransferGrid>();

        for(GID identifier : vertMatrix.keySet()) {
            if(visited.contains(identifier)) continue;
            ArrayList<GridVertex> clusterVerts = new ArrayList<>();
            depthFirstPopulate(identifier, visited, clusterVerts);
            if(clusterVerts.size() > 1) {
                LocalTransferGrid clusterResult = new LocalTransferGrid(parent, clusterVerts, edges);
                clusterResult.cleanEdges();
                clusters.add(clusterResult);
            }
        }
        return clusters;
    }

    private void depthFirstPopulate(GID vertex, HashSet<GID> visited, ArrayList<GridVertex> vertices) {
        GridVertex thisIteration = getVert(vertex);
        visited.add(vertex);

        if(thisIteration == null) return;
        vertices.add(thisIteration);

        if(thisIteration.isEmpty()) return;
        for(GridVertex neighbor : thisIteration.links) {
            GID currentID = neighbor.getGID();
            if(!visited.contains(currentID))
                depthFirstPopulate(currentID, visited, vertices);
        }
    }

    /***
     * Appends all elements from the supplied LocalTransferGrid to this
     * LocalTransferGrid, modifying it in-place.
     * @return This LocalTransferGrid (for chaining)
     */
    public LocalTransferGrid mergeWith(LocalTransferGrid other) {
        if(other.size() > 0) {
            vertMatrix.putAll(other.vertMatrix);
            edges.putAll(other.edges);
            paths.putAll(other.paths);
        }
        return this;
    }

    /***
     * Retrieves a vertex in this network.
     * @param BlockPos
     * @return GridVertex at the given BlockPos
     */
    public GridVertex getVert(GID pos) {
        return vertMatrix.get(pos);
    }

    /***
     * Gets all GridVertices regardless of subIndex
     * @param pos
     * @return A List of all GridVertices ta the given BlockPos
     */
    public List<GridVertex> getAllNodesAt(BlockPos pos) {
        ArrayList<GridVertex> vertices = new ArrayList<GridVertex>();
        for(GridVertex vert : vertMatrix.values())
            if(vert.getGID().getPos().equals(pos)) vertices.add(vert);
        return vertices;
    }

    /***
     * @return True if this LocalTransferGrid contains the given GridVertex
     */
    public boolean containsNode(GridVertex vertex) {
        return vertMatrix.containsValue(vertex);
    }

    /***
     * @return False if ALL of the GridVertexs in this TranferSystem are empty
     * (This network has no edges)
     */
    public boolean hasLinks() {
        for(GridVertex vertex : vertMatrix.values())
            if(!vertex.isEmpty()) return true;
        return false;
    }

    /***
     * Examines the edge map and the vertex map to find weirdness.
     * If there are any edges that point to vertices that aren't in this LocalTransferGrid,
     * this call will remove them.
     * @return True if this LocalTransferGrid was modified as a result of this call.
     */
    public boolean cleanEdges() {
        Iterator<Map.Entry<GIDPair, GridEdge>> matrixIterator = edges.entrySet().iterator();
        boolean changed = false;
        while(matrixIterator.hasNext()) {
            GridEdge currentEdge = matrixIterator.next().getValue();
            if(!(vertMatrix.containsKey(currentEdge.getSideA()) && vertMatrix.containsKey(currentEdge.getSideB()))) {
                changed = true;
                matrixIterator.remove();
            }
        }
        return changed;
    }

    /***
     * Finds the optimal path between the given GridVertex and all other GridVertices marked 
     * as members and stores those GridPath objects in this LocalTransferGrid.
     * @param origin
     * @return True if this LocalTransferGrid was modified as a result of this call. (if any paths were found)
     */
    public boolean pathfindFrom(GridVertex origin, boolean shouldUpdate) {
        boolean exists = false;
        for(GridVertex vert : vertMatrix.values()) {
            if(vert.equals(origin)) continue;
            if(!vert.isMember()) continue;

            if(paths.containsKey(new GIDPair(origin.getGID(), vert.getGID()))) continue;

            GridPath path = astar(origin, vert);
            if(path != null) {
                exists = true;
                addPath(path);
                if(shouldUpdate) onPathsUpdated(path, true);
            }
        }
        return exists;
    }

    public boolean pathfindFrom(GridVertex origin) {
        return pathfindFrom(origin, true);
    }

    /***
     * Finds optimal paths between every relevent GridVertex in this grid.
     * @return True if the LocalTransferGrid was modified as a result of this call.
     */
    public boolean findAllPaths(boolean shouldUpdate) {        
        boolean exists = false;
        for(GridVertex vert : vertMatrix.values()) {
            if(!vert.isMember()) continue;
            if(pathfindFrom(vert, shouldUpdate)) exists = true;
        }

        return exists;
    }

    public boolean findAllPaths() {
        return findAllPaths(true);
    }

    /***
     * Called internally whenever a GridPath is added or removed
     * @param path The path in question
     * @param add True if this path was added, false if this path was removed
     */
    private void onPathsUpdated(GridPath path, boolean add) {
        Mechano.log("PATHS UPDATED");
        if(add)
            GridSyncDirector.sendPathDebug(path, GridSyncPacketType.ADD);
        else
            GridSyncDirector.sendPathDebug(path, GridSyncPacketType.REMOVE);
    }

    /***
     * may god have mercy on my soul
     * @return GridPath object representing the optimal path between 
     * the two vertices, or null if no path could be fouund.
     */
    private GridPath astar(GridVertex start, GridVertex goal) {
        final Queue<GridVertex> openVerts = new PriorityQueue<>(11, GridVertex.getComparator());
        final Set<GridVertex> visited = new HashSet<>();
        final Map<GridVertex, GridVertex> path = new HashMap<GridVertex, GridVertex>();
        int slowestRate = Integer.MAX_VALUE;

        start.setCumulative(0);
        start.getAndStoreHeuristic(goal);
        openVerts.add(start);

        while(!openVerts.isEmpty()) {
            final GridVertex local = openVerts.poll();

            // loop terminates here if successful
            if(local.equals(goal)) {
                resetPathData(visited);
                return makePath(path, goal, slowestRate);
            }

            visited.add(local);
            for(GridVertex neighbor : local.connections()) {

                if(visited.contains(neighbor)) continue;
                GridEdge edge = lookupEdge(local, neighbor);

                if(edge == null) throw new NullPointerException("Error initiating A* - edge from " + local + " to " + neighbor + " could not be found.");
                if(!edge.canTransfer()) continue;

                int rate = edge.getTransferRate();
                if(rate < slowestRate) slowestRate = rate;

                float tentative = local.getAndStoreHeuristic(neighbor) + local.getCumulative();
                if(tentative < neighbor.getCumulative()) {

                    neighbor.setCumulative(tentative);
                    neighbor.getAndStoreHeuristic(local);
                    
                    path.put(neighbor, local);
                    if(!openVerts.contains(neighbor))
                        openVerts.add(neighbor);
                }
            }
        }
        resetPathData(visited);
        return null;
    }

    private void resetPathData(Set<GridVertex> verts) {
        for(GridVertex vert : verts)
            vert.reset();
    }

    private GridPath makePath(Map<GridVertex, GridVertex> path, GridVertex goal, int slowestRate) {
        final List<GridVertex> pathList = new ArrayList<GridVertex>();
        pathList.add(goal);
        while(path.containsKey(goal)) {
            goal = path.get(goal);
            pathList.add(goal);
        }

        return (!pathList.isEmpty()) ? new GridPath(pathList, slowestRate) : null;
    }

    /***
     * Removes all valid paths that involve the given GridVertex
     * @param vert GridVertex to compare
     */
    private void removePaths(GridVertex vert) { 
        Iterator<GridPath> pathIter = paths.values().iterator();
        while(pathIter.hasNext()) {
            GridPath path = pathIter.next();
            if(path.contains(vert)) {
                pathIter.remove();
                onPathsUpdated(path, false);
            }
        }
    }

    /***
     * Removes paths whose ends (at index 0 or length - 1)
     * are equal to the given vertex
     * @param vert GridVertex to compare
     */
    public void removePathsEndingIn(GridVertex vert) {
        Iterator<GridPath> pathIter = paths.values().iterator();
        while(pathIter.hasNext()) {
            GridPath path = pathIter.next();
            if(path.getEnd().equals(vert) ||  path.getStart().equals(vert)) {
                pathIter.remove();
                onPathsUpdated(path, false);
            }
        }
    }

    public void removePath(GIDPair id) {
        GridPath path = paths.remove(id);
        if(path != null) {
            onPathsUpdated(path, false);
        }
    }

    /***
     * 
     * Gets the GridVertex at the given ID, or creates a new one & adds it to this 
     * LocalTransferGrid, and returns it.
     * To be called only by GridVertex during the loading process from NBT.
     * The loading process attempts to link GridVertices, but its likely during
     * the loading process that these vertices don't exist yet.
     * @param id ID of GridVertex to find
     * @return The GridVertex that was found or created as a result of this call.
     */
    public GridVertex getOrCreateOnLoad(Level world, GID id) {
        GridVertex target = getVert(id);
        if(target != null) return target;
        target = new GridVertex(world, this, id);
        addVert(target);
        return target;
    }

    public void addPath(GridPath path) {
        paths.put(path.getHashable(), path);
    }

    public GridEdge lookupEdge(GridVertex a, GridVertex b) {
        return edges.get(new GIDPair(a.getGID(), b.getGID()));
    }

    protected void addEdge(GridVertex first, GridVertex second, int wireType) {
        addEdge(first.getGID(), second.getGID(), wireType);
    }

    protected void addEdge(GID idA, GID idB, int wireType) {
        edges.put(new GIDPair(idA, idB), new GridEdge(parent, new GIDPair(idA, idB), wireType));
    }   

    protected boolean removeEdge(GIDPair key) {
        return edges.remove(key) != null;
    }

    public Color getDebugColor() {
        return VectorHelper.toColor(((GridVertex)vertMatrix.values().toArray()[0]).getPos().getCenter());
    }

    public GlobalTransferGrid getParent() {
        return parent;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public int size() {
        return vertMatrix.size();
    }

    public boolean isEmpty() {
        return vertMatrix.isEmpty();
    }

    public Collection<GridVertex> allVerts() {
        return vertMatrix.values();
    }

    public Collection<GridEdge> allEdges() {
        return edges.values();
    }

    public Map<GIDPair, GridEdge> getEdgeMap() {
        return edges;
    }

    public Collection<GridPath> allPaths() {
        return paths.values();
    }

    public String toString() {
        String output = "\n";
        int x = 1;
        for(GridVertex vert : vertMatrix.values()) {
            output += "\tVertex " + x + ": " + vert + "\n";
            x++;
        }
        return output;
    }


    /////////
    private void requireValidLink(String failMessage, GID... idSet) {
        for(GID id : idSet) {
            if(id == null) 
                throw new NullPointerException(failMessage + " - The provided SystemLink is null!");
            if(!vertMatrix.containsKey(id))
                throw new NullPointerException(failMessage + " - No valid GridVertex matching SystemID " 
                + id + " could be found!");
        }
    }

    private void requireValidNode(String failMessage, GridVertex... vertexSet) {
        for(GridVertex vertex : vertexSet) {
            if(vertex == null)
                throw new NullPointerException(failMessage + " - The provided GridVertex is null!");
            if(!vertMatrix.containsValue(vertex))
                throw new NullPointerException(failMessage + " - The provided GridVertex does not exist in this LocalTransferGrid!");
        }       
    }
}