package com.quattage.mechano.foundation.electricity.power;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.power.features.GID;
import com.quattage.mechano.foundation.electricity.power.features.GIDPair;
import com.quattage.mechano.foundation.electricity.power.features.GridEdge;
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
 * A LocalGrid stores GridVertices in an undirected graph, forming an adjacency matrix.
 * Connections between these vertices are stored in two ways: <p>
 * - Each GridVertex contains a LinkedList of vertices that it is connected to <p>
 * - Each LocalGrid contains a HashMap of each edge for direct access. <p>
 * 
 * * I have been informed that the use of a LinkedList in the Y axis of this matrix actually 
 *  makes it an adjacency LIST.
 * * I will continue to refer to this as an adjacency matrix anyway, because it sounds cooler.
 */
public class LocalTransferGrid {
    
    private Map<GID, GridVertex> vertMatrix = new Object2ObjectOpenHashMap<>();
    private Map<GIDPair, GridEdge> edgeMatrix = new Object2ObjectOpenHashMap<>();

    private final GlobalTransferGrid parent;

    private int netEnergyPushed = 0;
    private int netEnergyPulled = 0;

    /***
     * Instantiates a blank TransferSystem
     */
    public LocalTransferGrid(GlobalTransferGrid parent) {
        this.parent = parent;
    }

    /***
     * Populates this TransferSystem with members of a list
     * @param cluster ArrayList of SystemNodes to add to this TransferSystem upon creation
     */
    public LocalTransferGrid(GlobalTransferGrid parent, ArrayList<GridVertex> cluster,  Map<GIDPair, GridEdge> edgeMatrix) {
        for(GridVertex node : cluster)
            vertMatrix.put(node.getGID(), node);
        this.edgeMatrix = ((Object2ObjectOpenHashMap<GIDPair, GridEdge>)edgeMatrix).clone();
        this.parent = parent;
    }

    public LocalTransferGrid(GlobalTransferGrid parent, CompoundTag in, Level world) {
        this.parent = parent;
        ListTag net = in.getList("nt", Tag.TAG_COMPOUND);
        for(int x = 0; x < net.size(); x++) {
            GridVertex n = new GridVertex(this.parent, net.getCompound(x));
            BlockPos check = n.getPos();
            // preventative measure to stop vertices from being added if they have bad data
            if((!(world.getBlockEntity(check) instanceof WireAnchorBlockEntity)) || (n.isEmpty())) {
                Mechano.LOGGER.warn("TransferSystem skipping registration of SystemVertex at [" 
                + check.getX() + ", " + check.getY() + ", " + check.getZ() + "]");
                continue;
            }
            vertMatrix.put(n.getGID(), n);
        }

        ListTag edgeList = in.getList("ed", Tag.TAG_COMPOUND);
        for(int x = 0; x < edgeList.size(); x++) {
            GridEdge edge = new GridEdge(this.parent, edgeList.getCompound(x));
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
        for(GridEdge edge : edgeMatrix.values())
            out.add(edge.writeTo(new CompoundTag()));
        return out;
    }

    public void getEdgesWithin(SectionPos section) {
        for(GridEdge edge : edgeMatrix.values()) {
            if(edge == null) continue;
            
        }
    }

    public void onSystemUpdated() {
    
    }

    public boolean addVert(GridVertex node) {
        if(node == null) 
        throw new NullPointerException("Failed to add node to TransferSystem - Cannot store a null node!");
        if(vertMatrix.containsKey(node.getGID()))
            return false;
        vertMatrix.put(node.getGID(), node);
        onSystemUpdated();
        return true;
    }

    public boolean addVert(GID id) {
        if(id == null)
            throw new NullPointerException("Failed to add node to TransferSystem - The provided SVID is null!");
        if(vertMatrix.containsKey(id))
            return false;
        vertMatrix.put(id, new GridVertex(parent, id.getPos(), id.getSubIndex()));
        onSystemUpdated();
        return true;
    }

    /***
     * Removes all SystemVerticiees at the given SVID (ignores subIndex) and 
     * removes all references to those verticies from every other vertex. The 
     * graph is likely to be discontinuous after this call, so it must be cleaned.
     * @param id SVID to remove
     * @return True if the SystemVertex was successfully removed (false if it didn't exist)
     */
    public boolean destroyVertsAt(GID id) {
        if(id == null) throw new NullPointerException("Error destroying SystemVertex - The provided SVID is null!");

        Iterator<Map.Entry<GID, GridVertex>> matrixIterator = vertMatrix.entrySet().iterator();
        boolean changed = false;
        while(matrixIterator.hasNext()) {
            GridVertex vert = matrixIterator.next().getValue();
            if(vert.getPos().equals(id.getPos())) {
                changed = true;
                matrixIterator.remove();
            }
            else {
                if(vert.unlinkEdgesToThisVertex(id, this) == true)
                    changed = true; 
            }
        }

        if(changed) {
            onSystemUpdated();
            GridSyncDirector.informPlayerVertexDestroyed(GridSyncPacketType.REMOVE, id);
        }
        return changed;
    }

    /***
     * Gets all verticies assigned to the given BlockPos
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
     * @return True if this TransferSystem contains the given SystemVertex
     */
    public boolean contains(GridVertex vert) {
        return contains(new GID(vert.getPos(), vert.getSubIndex()));
    }

    /***
     * @return True if this TransferSystem contains a SystemVertex at the given SVID
     */
    public boolean contains(GID id) {
        return vertMatrix.containsKey(id);
    }

    /***
     * Creates a link (edge) between two SystemVertices located at the given BlockPos
     * and index combinations.
     * @throws NullPointerException If the provided BlockPos and sub index combinations
     * don't indicate the location of a SystemVertex in this TransferSystem.
     * @param fP BlockPos of first connection
     * @param fI sub index of second connection
     * @param tP BlockPos of second connection
     * @param tI sub index of second connection
     * @return True if the SystemVertices were modified as a result of this call.
     */
    public boolean linkVerts(BlockPos fP, int fI, BlockPos tP, int tI, int wireType) {
        return linkVerts(new GID(fP, fI), new GID(tP, tI), wireType);
    }

    /***
     * Creates a link (edge) between two SystemVertices
     * @throws NullPointerException If either provided SystemVertex
     * does not exist within this TransferSystem.
     * @param first SystemVertex to link
     * @param second SystemVertex to link
     * @return True if the SystemVertices were modified as a result of this call,
     */
    public boolean linkVerts(GridVertex first, GridVertex second, int wireType) {
        requireValidNode("Failed to link SystemNodes", first, second);
        addEdge(first, second, wireType);
        if(first.linkTo(second) && second.linkTo(first)) {
            onSystemUpdated();
            return true;
        }
        return false;
    }

    /***
     * Creates a link (edge) between two SystemVertices at the provided SVIDs
     * @throws NullPointerException If either provided SVID doesn't 
     * indicate the location of a SystemVertex in this TransferSystem.
     * @param first SVID to locate and link
     * @param second SVID object to locate and link
     * @return True if the SystemVertices were modified as a result of this call,
     */
    public boolean linkVerts(GID first, GID second, int wireType) {
        requireValidLink("Failed to link SystemNodes", first, second);
        GridVertex vertF = vertMatrix.get(first);
        GridVertex vertT = vertMatrix.get(second);
        addEdge(first, second, wireType);
        if(vertF.linkTo(vertT) && vertT.linkTo(vertF)) {
            onSystemUpdated();
            return true;
        }
        return false;
    }

    /***
     * Performs a DFS to determine all of the different "clusters"
     * that form this TransferSystem. Individual vertices that are found to 
     * possess no connections are discarded, and are not included in the 
     * resulting clusters. <strong>Does not modify this system in-place.</strong>
     * 
     * @return ArrayList of TransferSystems formed from the individual clusters 
     * within this TransferSystem.
     */
    public ArrayList<LocalTransferGrid> trySplit() {
        HashSet<GID> visited = new HashSet<>();
        ArrayList<LocalTransferGrid> clusters = new ArrayList<LocalTransferGrid>();

        for(GID identifier : vertMatrix.keySet()) {
            if(visited.contains(identifier)) continue;
            ArrayList<GridVertex> clusterVerts = new ArrayList<>();
            depthFirstPopulate(identifier, visited, clusterVerts);
            if(clusterVerts.size() > 1) {
                LocalTransferGrid clusterResult = new LocalTransferGrid(parent, clusterVerts, edgeMatrix);
                clusterResult.cleanEdges();
                clusters.add(clusterResult);
            }
        }
        return clusters;
    }

    /***
     * Populates the given cluster ArrayList with all SystemVertices directly and 
     * indirectly connected to the node at the given BlockPos. The TransferSystem 
     * is traversed recursively, so calls must instantiate their own HashSet and 
     * ArrayList for storage.
     * @param vertex Vertex to begin the saerch outwards from
     * @param visted HashSet (usually just instantiated directly and empty when called) to cache visited vertices
     * @param vertices ArrayList which will be populated with all nodes that can be found connected to the given vertex.
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
     * Appends all elements from the supplied TransferSystem to this
     * TransferSystem, modifying it in-place.
     * @return This TransferSystem (for chaining)
     */
    public LocalTransferGrid mergeWith(LocalTransferGrid other) {
        if(other.size() > 0) {
            vertMatrix.putAll(other.vertMatrix);
            edgeMatrix.putAll(other.edgeMatrix);
            onSystemUpdated();
        }
        return this;
    }

    /***
     * Retrieves a node in this network based on SystemLink.
     * @param BlockPos
     * @return SystemNode at the given BlockPos
     */
    public GridVertex getNode(GID pos) {
        return vertMatrix.get(pos);
    }

    /***
     * @return True if this TransferSystem contains the given SystemNode
     */
    public boolean containsNode(GridVertex node) {
        return vertMatrix.containsValue(node);
    }

    /***
     * @return False if ALL of the SystemNodes in this TranferSystem are empty
     * (This network has no edges)
     */
    public boolean hasLinks() {
        for(GridVertex node : vertMatrix.values())
            if(!node.isEmpty()) return true;
        return false;
    }

    /***
     * Examines the edge map and the vertex map to find weirdness.
     * If there are any edges that point to verticies that aren't in this TransferSystem,
     * this call will remove them.
     * @return True if this TransferSystem was modified as a result of this call.
     */
    public boolean cleanEdges() {
        Iterator<Map.Entry<GIDPair, GridEdge>> matrixIterator = edgeMatrix.entrySet().iterator();
        boolean changed = false;
        while(matrixIterator.hasNext()) {
            GIDPair currentKey = matrixIterator.next().getKey();
            if(!(vertMatrix.containsKey(currentKey.getSideA()) && vertMatrix.containsKey(currentKey.getSideB()))) {
                changed = true;
                matrixIterator.remove();
            }
        }
        onSystemUpdated();
        return changed;
    }

    /***
     * Genreates a Color for this TransferSystem. Useful for debugging.
     * @return a Color representing this TransferSystem
     */
    public Color getDebugColor() {
        return VectorHelper.toColor(((GridVertex)vertMatrix.values().toArray()[0]).getPos().getCenter());
    }

    public int size() {
        return vertMatrix.size();
    }

    /***
     * @return True if this TransferSystem doesn't contain any SystemNodes.
     */
    public boolean isEmpty() {
        return vertMatrix.isEmpty();
    }

    /***
     * Gets this TransferSystem's vertex graph as a raw Collection.
     * @return Collection containing all SystemNodes in this TransferSystem
     */
    public Collection<GridVertex> allVerts() {
        return vertMatrix.values();
    }

    /***
     * Gets this TransferSystem's edge graph as a raw Collection. 
     * @return Collection containing all SystemNodes in this TransferSystem
     */
    public Collection<GridEdge> allEdges() {
        return edgeMatrix.values();
    }

    public String toString() {
        String output = "";
        int x = 1;
        for(GridVertex node : vertMatrix.values()) {
            output += "\tNode " + x + ": " + node + "\n";
            x++;
        }
        return output;
    }

    public void requireValidLink(String failMessage, GID... idSet) {
        for(GID id : idSet) {
            if(id == null) 
                throw new NullPointerException(failMessage + " - The provided SystemLink is null!");
            if(!vertMatrix.containsKey(id))
                throw new NullPointerException(failMessage + " - No valid SystemNode matching SystemID " 
                + id + " could be found!");
        }
    }

    public void requireValidNode(String failMessage, GridVertex... nodeSet) {
        for(GridVertex node : nodeSet) {
            if(node == null)
                throw new NullPointerException(failMessage + " - The provided SystemNode is null!");
            if(!vertMatrix.containsValue(node))
                throw new NullPointerException(failMessage + " - The provided SystemNode does not exist in this TransferSystem!");
        }       
    }

    public Map<GIDPair, GridEdge> getEdgeMatrix() {
        return edgeMatrix;
    }

    // to be used internally, other methods deal with this for u //
    protected void addEdge(GridVertex first, GridVertex second, int wireType) {
        addEdge(first.getGID(), second.getGID(), wireType);
    }

    protected void addEdge(GID idA, GID idB, int wireType) {
        edgeMatrix.put(new GIDPair(idA, idB), new GridEdge(parent, new GIDPair(idA, idB), wireType));
    }   

    protected boolean removeEdge(GIDPair key) {
        return edgeMatrix.remove(key) != null;
    }

    protected boolean removeEdge(GridVertex vertA, GridVertex vertB) {
        return removeEdge(new GIDPair(vertA.getGID(), vertB.getGID()));
    }
    // to be used internally, other methods deal with this for u //
}