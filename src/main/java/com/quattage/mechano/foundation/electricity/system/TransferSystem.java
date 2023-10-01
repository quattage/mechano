package com.quattage.mechano.foundation.electricity.system;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.electricity.WireNodeBlockEntity;
import com.quattage.mechano.foundation.helper.VectorHelper;
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
    private HashMap<SVID, SystemVertex> systemMatrix = new HashMap<>();

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
    public TransferSystem(ArrayList<SystemVertex> cluster) {
        for(SystemVertex node : cluster)
            systemMatrix.put(node.getSVID(), node);
    }

    public TransferSystem(CompoundTag in, ServerLevel world) {
        ListTag net = in.getList("sub", Tag.TAG_COMPOUND);
        for(int x = 0; x < net.size(); x++) {
            SystemVertex n = new SystemVertex(net.getCompound(x));
            BlockPos check = n.getPos();

            // preventative measure to stop networks from being built where they shouldn't if something goes wrong
            if(!(world.getBlockEntity(check) instanceof WireNodeBlockEntity)) {
                Mechano.LOGGER.warn("TransferSystem skipping registration of SystemVertex at [" 
                    + check.getX() + ", " + check.getY() + ", " + check.getZ() + "] - No valid BlockEntity was found at this location!" +
                    " If you've recently experienced a crash, this is probably worth reporting.");
                continue;
            }

            systemMatrix.put(n.getSVID(), n);
        }
    }

    public CompoundTag writeTo(CompoundTag in) {
        in.put("sub", writeMatrix());
        return in;
    }

    public ListTag writeMatrix() {
        ListTag out = new ListTag();
        for(SystemVertex v : systemMatrix.values())
            out.add(v.writeTo(new CompoundTag()));
        return out;
    }

    public boolean addNode(SystemVertex node) {
        if(node == null) 
        throw new NullPointerException("Failed to add node to TransferSystem - Cannot store a null node!");
        if(systemMatrix.containsKey(node.getSVID()))
            return false;
        systemMatrix.put(node.getSVID(), node);
        return true;
    }

    public boolean addNode(SVID id) {
        if(id == null)
            throw new NullPointerException("Failed to add node to TransferSystem - The provided SVID is null!");
        if(systemMatrix.containsKey(id))
            return false;
        systemMatrix.put(id, id.toVertex());
        return true;
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
    public boolean link(BlockPos fP, int fI, BlockPos tP, int tI) {
        return link(new SVID(fP, fI), new SVID(tP, tI));
    }

    /***
     * Creates a link (edge) between two SystemVertices
     * @throws NullPointerException If either provided SystemVertex
     * does not exist within this TransferSystem.
     * @param first SystemVertex to link
     * @param second SystemVertex to link
     * @return True if the SystemVertices were modified as a result of this call,
     */
    public boolean link(SystemVertex first, SystemVertex second) {
        requireValidNode("Failed to link SystemNodes", first, second);
        return first.linkTo(second) && second.linkTo(first);
    }

    /***
     * Creates a link (edge) between two SystemVertices at the provided SVIDs
     * @throws NullPointerException If either provided SVID doesn't 
     * indicate the location of a SystemVertex in this TransferSystem.
     * @param first SVID to locate and link
     * @param second SVID object to locate and link
     * @return True if the SystemVertices were modified as a result of this call,
     */
    public boolean link(SVID first, SVID second) {
        requireValidLink("Failed to link SystemNodes", first, second);
        SystemVertex vertF = systemMatrix.get(first);
        SystemVertex vertT = systemMatrix.get(second);
        return vertF.linkTo(vertT) && vertT.linkTo(vertF);
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

        for(SVID identifier : systemMatrix.keySet()) {
            if(visited.contains(identifier)) continue;
            ArrayList<SystemVertex> clusterContents = new ArrayList<>();
            depthFirstPopulate(identifier, visited, clusterContents);
            if(clusterContents.size() > 1)
                clusters.add(new TransferSystem(clusterContents));
        }
        return clusters;
    }

    /***
     * Populates the given cluster ArrayList with all SystemNodes directly and 
     * indirectly connected to the node at the given BlockPos. The TransferSystem 
     * is traversed recursively, so calls must instantiate their own HashSet and 
     * ArrayList for storage.
     * @param vertex Vertex to begin the saerch outwards from
     * @param visted HashSet (usually just instantiated directly and empty when called) to store visited vertices
     * @param cluster ArrayList which will be populated with all nodes that can be found connected to the given vertex.
     */
    public void depthFirstPopulate(SVID vertex, HashSet<SVID> visited, ArrayList<SystemVertex> cluster) {
        SystemVertex thisIteration = getNode(vertex);
        visited.add(vertex);
        cluster.add(thisIteration);

        for(SystemVertex neighbor : thisIteration.links) {
            SVID currentID = neighbor.getSVID();
            if(!visited.contains(currentID))
                depthFirstPopulate(currentID, visited, cluster);
        }
    }

    /***
     * Appends all elements from the supplied TransferSystem to this
     * TransferSystem, modifying it in-place.
     * @return This TransferSystem (for chaining)
     */
    public TransferSystem mergeWith(TransferSystem other) {
        systemMatrix.putAll(other.systemMatrix);
        return this;
    }

    /***
     * Retrieves a node in this network based on SystemLink.
     * @param BlockPos
     * @return SystemNode at the given BlockPos
     */
    public SystemVertex getNode(SVID pos) {
        return systemMatrix.get(pos);
    }



    /***
     * @return True if this TransferSystem contains the given SystemNode
     */
    public boolean containsNode(SystemVertex node) {
        return systemMatrix.containsValue(node);
    }

    /***
     * Gets this TransferSystem as a raw Collection. <p>
     * <strong>Modifying this map is not reccomended.</strong>
     * @return Collection containing all SystemNodes in this TransferSystem
     */
    public Collection<SystemVertex> all() {
        return systemMatrix.values();
    }

    /***
     * @return True if this TransferSystem doesn't contain any SystemNodes.
     */
    public boolean isEmpty() {
        return systemMatrix.isEmpty();
    }

    /***
     * @return False if ALL of the SystemNodes in this TranferSystem are empty
     * (This network has no edges)
     */
    public boolean hasLinks() {
        for(SystemVertex node : systemMatrix.values())
            if(!node.isEmpty()) return true;
        return false;
    }

    /***
     * Genreates a Color for this TransferSystem. Useful for debugging.
     * @return a Color representing this TransferSystem
     */
    public Color getDebugColor() {
        return VectorHelper.toColor(((SystemVertex)systemMatrix.values().toArray()[0]).getPos().getCenter());
    }

    public int size() {
        return systemMatrix.size();
    }
    
    public SystemVertex[] getAllConnections() {
        return systemMatrix.values().toArray(size -> new SystemVertex[size]);
    }

    public String toString() {
        String output = "";
        int x = 1;
        for(SystemVertex node : systemMatrix.values()) {
            output += "\tNode " + x + ": " + node + "\n";
            x++;
        }
        return output;
    }

    public void requireValidLink(String failMessage, SVID... idSet) {
        for(SVID id : idSet) {
            if(id == null) 
                throw new NullPointerException(failMessage + " - The provided SystemLink is null!");
            if(!systemMatrix.containsKey(id))
                throw new NullPointerException(failMessage + " - No valid SystemNode matching SystemID " 
                + id + " could be found!");
        }
    }

    public void requireValidNode(String failMessage, SystemVertex... nodeSet) {
        for(SystemVertex node : nodeSet) {
            if(node == null)
                throw new NullPointerException(failMessage + " - The provided SystemNode is null!");
            if(!systemMatrix.containsValue(node))
                throw new NullPointerException(failMessage + " - The provided SystemNode does not exist in this TransferSystem!");
        }       
    }

    
}
