package com.quattage.mechano.foundation.electricity.power;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

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

    private int netEnergyPushed = 0;
    private int netEnergyPulled = 0;

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
            GridVertex n = new GridVertex(this, net.getCompound(x));
            BlockPos check = n.getPos();
            // preventative measure to stop vertices from being added if they have bad data
            if((!(world.getBlockEntity(check) instanceof WireAnchorBlockEntity)) || (n.isEmpty())) {
                Mechano.LOGGER.warn("LocalTransferGrid skipping registration of GridVertex at [" 
                + check.getX() + ", " + check.getY() + ", " + check.getZ() + "]");
                continue;
            }
            vertMatrix.put(n.getGID(), n);
        }

        ListTag edgeList = in.getList("ed", Tag.TAG_COMPOUND);
        for(int x = 0; x < edgeList.size(); x++) {
            GridEdge edge = new GridEdge(this.parent, edgeList.getCompound(x));
            edges.put(edge.getID(), edge);
        }
    }

    public CompoundTag writeTo(CompoundTag in) {
        in.put("nt", writeMatrix());
        in.put("ed", writeEdges());
        return in;
    }

    public ListTag writeMatrix() {
        ListTag out = new ListTag();
        for(GridVertex v : vertMatrix.values())
            out.add(v.writeTo(new CompoundTag()));
        return out;
    }

    public ListTag writeEdges() {
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

    public void onSystemUpdated() {
    
    }

    public boolean addVert(GridVertex vertex) {
        if(vertex == null) 
        throw new NullPointerException("Failed to add vertex to LocalTransferGrid - Cannot store a null vertex!");
        if(vertMatrix.containsKey(vertex.getGID()))
            return false;
        vertMatrix.put(vertex.getGID(), vertex);
        onSystemUpdated();
        return true;
    }

    public boolean destroyVertsAt(GID id) {
        if(id == null) throw new NullPointerException("Error destroying GridVertex - The provided GID is null!");

        Iterator<Map.Entry<GID, GridVertex>> matrixIterator = vertMatrix.entrySet().iterator();
        boolean changed = false;
        while(matrixIterator.hasNext()) {
            GridVertex vert = matrixIterator.next().getValue();
            if(vert.getPos().equals(id.getPos())) {
                changed = true;
                matrixIterator.remove();
            }
            else {
                if(vert.unlinkEdgesToThisVertex(id, this)) {
                    GridSyncDirector.informPlayerEdgeUpdate(GridSyncPacketType.REMOVE, new GridClientEdge(new GIDPair(vert.getGID(), id), -1));
                    changed = true; 
                }
            }
        }

        if(changed) {
            onSystemUpdated();
            GridSyncDirector.informPlayerVertexDestroyed(GridSyncPacketType.REMOVE, id);
        }
        return changed;
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
        return contains(new GID(vert.getPos(), vert.getSubIndex()));
    }

    /***
     * @return True if this LocalTransferGrid contains a GridVertex at the given GID
     */
    public boolean contains(GID id) {
        return vertMatrix.containsKey(id);
    }

    /***
     * Creates a link (edge) between two GridVertices located at the given BlockPos
     * and index combinations.
     * @throws NullPointerException If the provided BlockPos and sub index combinations
     * don't indicate the location of a GridVertex in this LocalTransferGrid.
     * @param fP BlockPos of first connection
     * @param fI sub index of second connection
     * @param tP BlockPos of second connection
     * @param tI sub index of second connection
     * @return True if the GridVertices were modified as a result of this call.
     */
    public boolean linkVerts(BlockPos fP, int fI, BlockPos tP, int tI, int wireType) {
        return linkVerts(new GID(fP, fI), new GID(tP, tI), wireType);
    }

    /***
     * Creates a link (edge) between two GridVertices
     * @throws NullPointerException If either provided GridVertex
     * does not exist within this LocalTransferGrid.
     * @param first GridVertex to link
     * @param second GridVertex to link
     * @return True if the GridVertices were modified as a result of this call,
     */
    public boolean linkVerts(GridVertex first, GridVertex second, int wireType) {
        requireValidNode("Failed to link GridVertexs", first, second);
        addEdge(first, second, wireType);
        if(first.linkTo(second) && second.linkTo(first)) {
            onSystemUpdated();
            first.syncToHostBE();
            second.syncToHostBE();
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
     * @return True if the GridVertices were modified as a result of this call,
     */
    public boolean linkVerts(GID first, GID second, int wireType) {
        requireValidLink("Failed to link GridVertexs", first, second);
        GridVertex vertF = vertMatrix.get(first);
        GridVertex vertT = vertMatrix.get(second);
        addEdge(first, second, wireType);
        if(vertF.linkTo(vertT) && vertT.linkTo(vertF)) {
            onSystemUpdated();
            vertF.syncToHostBE();
            vertT.syncToHostBE();
            return true;
        }
        return false;
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
    public ArrayList<LocalTransferGrid> trySplit() {
        HashSet<GID> visited = new HashSet<>();
        ArrayList<LocalTransferGrid> clusters = new ArrayList<LocalTransferGrid>();

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

    /***
     * Populates the given cluster ArrayList with all GridVertices directly and 
     * indirectly connected to the vertex at the given BlockPos. The LocalTransferGrid 
     * is traversed recursively, so calls must instantiate their own HashSet and 
     * ArrayList for storage.
     * @param vertex Vertex to begin the saerch outwards from
     * @param visted HashSet (usually just instantiated directly and empty when called) to cache visited vertices
     * @param vertices ArrayList which will be populated with all GridVertices that can be found connected to the given vertex.
     */
    public void depthFirstPopulate(GID vertex, HashSet<GID> visited, ArrayList<GridVertex> vertices) {
        GridVertex thisIteration = getNode(vertex);
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
            onSystemUpdated();
        }
        return this;
    }

    /***
     * Retrieves a vertex in this network based on SystemLink.
     * @param BlockPos
     * @return GridVertex at the given BlockPos
     */
    public GridVertex getNode(GID pos) {
        return vertMatrix.get(pos);
    }

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
        onSystemUpdated();
        return changed;
    }

    public Color getDebugColor() {
        return VectorHelper.toColor(((GridVertex)vertMatrix.values().toArray()[0]).getPos().getCenter());
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

    public String toString() {
        String output = "";
        int x = 1;
        for(GridVertex vert : vertMatrix.values()) {
            output += "\tVertex " + x + ": " + vert + "\n";
            x++;
        }
        return output;
    }

    public Map<GIDPair, GridEdge> getEdges() {
        return edges;
    }

    public GlobalTransferGrid getParent() {
        return parent;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void pathfindFrom(GridVertex origin) {
        for(GridVertex vert : vertMatrix.values()) {
            if(vert.equals(origin)) continue;
            if(!vert.isMember()) continue;

            GridPath path = astar(origin, vert);
            if(path != null) paths.put(path.getHashable(), path);
        }
    }

    /***
     * may god have mercy on my soul
     * @param start
     * @param goal
     * @return
     */
    private GridPath astar(GridVertex start, GridVertex goal) {
        PriorityQueue<GridVertex> openVerts = new PriorityQueue<>(Comparator.comparingDouble(vert -> vert.getF()));
        HashSet<GridVertex> closedVerts = new HashSet<>();

        Mechano.log("Starting A* from " + start.getGID() + " to " + goal.getGID());

        int slowestRate = Integer.MAX_VALUE;

        start.setCumulative(0);
        start.getAndStoreHeuristic(goal);
        openVerts.add(start);

        while(!openVerts.isEmpty()) {
            final GridVertex local = openVerts.poll();

            if(local.equals(goal)) {
                closedVerts.add(goal);
                return new GridPath(closedVerts, slowestRate);
            }

            closedVerts.add(local);
            for(GridVertex neighbor : local.connections()) {

                if(closedVerts.contains(neighbor)) continue;
                
                GridEdge edge = lookupEdge(local, neighbor);
                if(edge == null) throw new NullPointerException("Error initiating A* - edge from " + local + " to " + neighbor + " could not be found.");
                int rate = edge.getTransferRate();
                if(!(rate > 0 && edge.canTransfer())) continue;
                if(rate < slowestRate) slowestRate = rate;

                float tentative = edge.getDistance() + local.getCumulative();

                if(tentative < neighbor.getCumulative()) {

                    neighbor.setCumulative(tentative);
                    neighbor.getAndStoreHeuristic(edge);
                    
                    closedVerts.add(neighbor);

                    if(!openVerts.contains(neighbor))
                        openVerts.add(neighbor);
                }
            }
        }

        Mechano.log("No path found");

        return null;
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

    protected boolean removeEdge(GridVertex vertA, GridVertex vertB) {
        return removeEdge(new GIDPair(vertA.getGID(), vertB.getGID()));
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