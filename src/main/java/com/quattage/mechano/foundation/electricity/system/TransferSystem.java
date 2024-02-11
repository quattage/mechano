package com.quattage.mechano.foundation.electricity.system;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.system.edge.ISystemEdge;
import com.quattage.mechano.foundation.electricity.system.edge.SVIDPair;
import com.quattage.mechano.foundation.helper.VectorHelper;
import com.quattage.mechano.foundation.electricity.system.edge.ElectricSystemEdge;
import com.simibubi.create.foundation.utility.Color;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;

/***
 * A TransferSystem stores approximate versions of NodeConnections (edges) 
 * and NodeBanks (vertices) objects in an undirected graph represented by 
 * an adjacency matrix. <p>
 * A TransferSystem allows energy to be pushed and pulled from connected nodes
 * in an addressable and optimized way. <p>
 * 
 * * I have been informed that the use of a LinkedList in the Y axis of this matrix actually makes it an adjacency LIST. 
 * * I will continue to refer to this as an adjacency matrix anyway, because it sounds cooler.
 */
public class TransferSystem {
    
    /***
     * Stores the X axis of the adjacency matrix
     */
    private HashMap<SVID, SystemVertex> vertMatrix = new HashMap<>();

    // TODO hashing edges like this is probably not necessary
    private HashMap<SVIDPair, ISystemEdge> edgeMatrix = new HashMap<>();

    private int netEnergyPushed = 0;
    private int netEnergyPulled = 0;

    /***
     * Instantiates a blank TransferSystem
     */
    public TransferSystem() {}

    /***
     * Populates this TransferSystem with members of a list
     * @param cluster ArrayList of SystemNodes to add to this TransferSystem upon creation
     */
    @SuppressWarnings("unchecked")
    public TransferSystem(ArrayList<SystemVertex> cluster,  HashMap<SVIDPair, ISystemEdge> edgeMatrix) {
        for(SystemVertex node : cluster)
            vertMatrix.put(node.toSVID(), node);
        this.edgeMatrix = (HashMap<SVIDPair, ISystemEdge>)(edgeMatrix.clone());
    }

    public TransferSystem(CompoundTag in, ServerLevel world) {
        ListTag net = in.getList("sub", Tag.TAG_COMPOUND);
        for(int x = 0; x < net.size(); x++) {
            SystemVertex n = new SystemVertex(net.getCompound(x));
            BlockPos check = n.getPos();

            // preventative measure to stop vertices from being added if they have bad data
            if((!(world.getBlockEntity(check) instanceof WireAnchorBlockEntity)) || (n.isEmpty())) {
                Mechano.LOGGER.warn("TransferSystem skipping registration of SystemVertex at [" 
                + check.getX() + ", " + check.getY() + ", " + check.getZ() + "]");
                continue;
            }

            surmiseLinks(n);
            vertMatrix.put(n.toSVID(), n);
        }
    }

    public CompoundTag writeTo(CompoundTag in) {
        in.put("sub", writeMatrix());
        return in;
    }

    public ListTag writeMatrix() {
        ListTag out = new ListTag();
        for(SystemVertex v : vertMatrix.values())
            out.add(v.writeTo(new CompoundTag()));
        return out;
    }

    public void surmiseLinks(SystemVertex vert) {
        for(SystemVertex connected : vert.links)
            addEdge(vert.toSVID(), connected.toSVID());
    }

    public void onSystemUpdated() {
        Mechano.log("SYSTEM UPDATED");
        printEdgesForTest();
    }

    private void printEdgesForTest() {
        int x = 1;
        for(ISystemEdge edge : edgeMatrix.values()) {
            Mechano.log(x + ": " + VectorHelper.asString(edge.getPosA()) 
                + " -> " + VectorHelper.asString(edge.getPosB()) + "\n");
            x++;
        }
    }

    public boolean addVert(SystemVertex node) {
        if(node == null) 
        throw new NullPointerException("Failed to add node to TransferSystem - Cannot store a null node!");
        if(vertMatrix.containsKey(node.toSVID()))
            return false;
        vertMatrix.put(node.toSVID(), node);
        onSystemUpdated();
        return true;
    }

    public boolean addVert(SVID id) {
        if(id == null)
            throw new NullPointerException("Failed to add node to TransferSystem - The provided SVID is null!");
        if(vertMatrix.containsKey(id))
            return false;
        vertMatrix.put(id, id.toVertex());
        onSystemUpdated();
        return true;
    }

    /***
     * Removes the specified SystemVertex and removes all
     * references to that vertex from all other verticies. The graph
     * is likely to be discontinuous after this call, so it must be cleaned.
     * @param vertToRemove SystemVertex to remove
     * @return Boolean if this TransferSystem was modified as a result of this call
     */
    public boolean destroyVert(SystemVertex vertToRemove) {
        if(vertToRemove == null)
            throw new NullPointerException("Failed to destroy node - The provided SystemVertex is null!");
        if(!vertMatrix.containsValue(vertToRemove))
            return false;

        Iterator<SystemVertex> matrixIterator = vertMatrix.values().iterator();
        while(matrixIterator.hasNext()) {
            SystemVertex vert = matrixIterator.next();
            if(vert.equals(vertToRemove)) {
                matrixIterator.remove();
            }
            else
                vert.unlinkFrom(vertToRemove);
        }
        onSystemUpdated();
        return true;
    }

    /***
     * Removes all SystemVerticiees at the given SVID (ignores subIndex) and 
     * removes all references to those verticies from every other vertex.. The 
     * graph is likely to be discontinuous after this call, so it must be cleaned.
     * @param id SVID to remove
     * @return True if the SystemVertex was successfully removed (false if it didn't exist)
     */
    public boolean destroyVertsAt(SVID id) {
        if(id == null)
            throw new NullPointerException("Failed to destroy node - The provided SVID is null!");

        Iterator<Map.Entry<SVID, SystemVertex>> matrixIterator = vertMatrix.entrySet().iterator();
        boolean changed = false;
        while(matrixIterator.hasNext()) {
            SystemVertex vert = matrixIterator.next().getValue();
            if(vert.getPos().equals(id.getPos())) {
                changed = true;
                matrixIterator.remove();
            }
            else {
                Mechano.log("Attempting to unlink " + vert);
                if(vert.unlinkFromContextually(id, this) == true) changed = true;
            }
                
        }
        if(changed) onSystemUpdated();
        return changed;
    }

    /***
     * Gets all verticies assigned to the given BlockPos
     * @param pos BlockPos to check
     * @return An array of SystemVerticies at this BlockPos
     */
    public ArrayList<SystemVertex> getVertsAt(BlockPos pos) {
        ArrayList<SystemVertex> out = new ArrayList<>();
        for(SystemVertex vert : vertMatrix.values())
            if(vert.getPos().equals(pos)) out.add(vert);
        return out;
    }

    /***
     * @return True if this TransferSystem contains the given SystemVertex
     */
    public boolean contains(SystemVertex vert) {
        return contains(new SVID(vert.getPos(), vert.getSubIndex()));
    }

    /***
     * @return True if this TransferSystem contains a SystemVertex at the given SVID
     */
    public boolean contains(SVID id) {
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
    public boolean linkVerts(BlockPos fP, int fI, BlockPos tP, int tI) {
        return linkVerts(new SVID(fP, fI), new SVID(tP, tI));
    }

    /***
     * Creates a link (edge) between two SystemVertices
     * @throws NullPointerException If either provided SystemVertex
     * does not exist within this TransferSystem.
     * @param first SystemVertex to link
     * @param second SystemVertex to link
     * @return True if the SystemVertices were modified as a result of this call,
     */
    public boolean linkVerts(SystemVertex first, SystemVertex second) {
        requireValidNode("Failed to link SystemNodes", first, second);
        addEdge(first, second);
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
    public boolean linkVerts(SVID first, SVID second) {
        requireValidLink("Failed to link SystemNodes", first, second);
        SystemVertex vertF = vertMatrix.get(first);
        SystemVertex vertT = vertMatrix.get(second);
        addEdge(first, second);
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
    public ArrayList<TransferSystem> trySplit() {
        HashSet<SVID> visited = new HashSet<>();
        ArrayList<TransferSystem> clusters = new ArrayList<TransferSystem>();

        for(SVID identifier : vertMatrix.keySet()) {
            if(visited.contains(identifier)) continue;
            ArrayList<SystemVertex> clusterVerts = new ArrayList<>();
            depthFirstPopulate(identifier, visited, clusterVerts);
            if(clusterVerts.size() > 1) {
                TransferSystem clusterResult = new TransferSystem(clusterVerts, edgeMatrix);
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
    public void depthFirstPopulate(SVID vertex, HashSet<SVID> visited, ArrayList<SystemVertex> vertices) {
        SystemVertex thisIteration = getNode(vertex);
        visited.add(vertex);

        if(thisIteration == null) return;
        vertices.add(thisIteration);

        if(thisIteration.isEmpty()) return;
        for(SystemVertex neighbor : thisIteration.links) {
            SVID currentID = neighbor.toSVID();
            if(!visited.contains(currentID))
                depthFirstPopulate(currentID, visited, vertices);
        }
    }

    /***
     * Appends all elements from the supplied TransferSystem to this
     * TransferSystem, modifying it in-place.
     * @return This TransferSystem (for chaining)
     */
    public TransferSystem mergeWith(TransferSystem other) {
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
    public SystemVertex getNode(SVID pos) {
        return vertMatrix.get(pos);
    }

    /***
     * @return True if this TransferSystem contains the given SystemNode
     */
    public boolean containsNode(SystemVertex node) {
        return vertMatrix.containsValue(node);
    }

    /***
     * @return False if ALL of the SystemNodes in this TranferSystem are empty
     * (This network has no edges)
     */
    public boolean hasLinks() {
        for(SystemVertex node : vertMatrix.values())
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
        Iterator<Map.Entry<SVIDPair, ISystemEdge>> matrixIterator = edgeMatrix.entrySet().iterator();
        boolean changed = false;
        while(matrixIterator.hasNext()) {
            SVIDPair currentKey = matrixIterator.next().getKey();
            if(!(vertMatrix.containsKey(currentKey.getA()) && vertMatrix.containsKey(currentKey.getB()))) {
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
        return VectorHelper.toColor(((SystemVertex)vertMatrix.values().toArray()[0]).getPos().getCenter());
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
    public Collection<SystemVertex> allVerts() {
        return vertMatrix.values();
    }

    /***
     * Gets this TransferSystem's edge graph as a raw Collection. 
     * @return Collection containing all SystemNodes in this TransferSystem
     */
    public Collection<ISystemEdge> allEdges() {
        return edgeMatrix.values();
    }

    public String toString() {
        String output = "";
        int x = 1;
        for(SystemVertex node : vertMatrix.values()) {
            output += "\tNode " + x + ": " + node + "\n";
            x++;
        }
        return output;
    }

    public void requireValidLink(String failMessage, SVID... idSet) {
        for(SVID id : idSet) {
            if(id == null) 
                throw new NullPointerException(failMessage + " - The provided SystemLink is null!");
            if(!vertMatrix.containsKey(id))
                throw new NullPointerException(failMessage + " - No valid SystemNode matching SystemID " 
                + id + " could be found!");
        }
    }

    public void requireValidNode(String failMessage, SystemVertex... nodeSet) {
        for(SystemVertex node : nodeSet) {
            if(node == null)
                throw new NullPointerException(failMessage + " - The provided SystemNode is null!");
            if(!vertMatrix.containsValue(node))
                throw new NullPointerException(failMessage + " - The provided SystemNode does not exist in this TransferSystem!");
        }       
    }

    // to be used internally, other methods deal with this for u //
    protected void addEdge(SystemVertex first, SystemVertex second) {
        addEdge(first.toSVID(), second.toSVID());
    }

    protected void addEdge(SVID idA, SVID idB) {
        edgeMatrix.put(new SVIDPair(idA, idB), new ElectricSystemEdge(idA, idB));
    }   

    protected boolean removeEdge(SVIDPair key) {
        Mechano.log("EDGE? MORE LIKE DEAD AMIRITE FELLAS UP TOP");
        return edgeMatrix.remove(key) != null;
    }

    protected boolean removeEdge(SystemVertex vertA, SystemVertex vertB) {
        return removeEdge(new SVIDPair(vertA.toSVID(), vertB.toSVID()));
    }
    // to be used internally, other methods deal with this for u //
}