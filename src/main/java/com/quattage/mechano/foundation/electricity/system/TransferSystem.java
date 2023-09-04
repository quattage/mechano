package com.quattage.mechano.foundation.electricity.system;

import java.util.LinkedList;

import net.minecraft.core.BlockPos;

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
     */ //TODO maybe don't use a linkedlist for this if it turns out not to be necessary
    private LinkedList<SystemNode> networkMatrix = new LinkedList<SystemNode>();

    public boolean addNode(SystemNode node) {
        if(node == null) 
            throw new NullPointerException("Failed to add node to TransferSystem - Cannot store a null node!");
        if(networkMatrix.contains(node))
            return false;
        return networkMatrix.add(node);
    }

    public SystemNode popNode(int nodeIndex) {
        return networkMatrix.remove(nodeIndex);
    }

    public boolean removeNode(int nodeIndex) {
        SystemNode removed = networkMatrix.remove(nodeIndex);
        return removed == null;
    }

    public boolean removeNode(SystemNode node) {
        return networkMatrix.remove(node);
    }

    /***
     * Checks whether a link exists between given SystemNodes. 
     * @param first
     * @param second
     * @return True if a connection exists between the two nodes.
     */
    public boolean doesLinkExistBetween(SystemNode first, SystemNode second) {
        if(networkMatrix.contains(first) && networkMatrix.contains(second))
            if(first.isLinkedTo(this, second)) return true;
        return false;
    }

    /***
     * Checks whether a link exists between the given indexes. The matrix within 
     * this TransferSystem undirected, so indexFrom and indexTo are interchangable.
     * @param indexFrom 
     * @param indexTo
     * @return True if the SystemNodes at the given indexes are connected.
     */
    public boolean doesLinkExistBetween(int indexFrom, int indexTo) {
        if(isNodeInBounds(indexFrom)) {
            SystemNode nF = networkMatrix.get(indexFrom);
            return nF.isLinkWithinBounds(indexTo);
        }
        if(isNodeInBounds(indexTo)) {
            SystemNode nT = networkMatrix.get(indexTo);
            return nT.isLinkWithinBounds(indexFrom);
        }
        return false;
    }

    /***
     * Create a link (edge) between the SystemNodes located at the 
     * given indexes. This link is non-directed. It is added 
     * symmetrically to both nodes at both provided indexes.
     * 
     * @throws ArrayIndexOutOfBoundsException If either provided index doesn't 
     * exist within this TransferSystem
     * @param indexFrom Index of one SystemNode to link
     * @param indexTo Index of the other SystemNode to link
     * @return True if the SystemNodes were modified
     */
    public boolean link(int indexFrom, int indexTo) {
        if(!isNodeInBounds(indexFrom)) 
            throw new ArrayIndexOutOfBoundsException("Failed to add a link to this TransferSystem - Index " + 
                indexFrom + " is out of bounds for a TransferSystem of size " + networkMatrix.size());
        if(!isNodeInBounds(indexTo)) 
            throw new ArrayIndexOutOfBoundsException("Failed to add a link to this TransferSystem - Index " + 
                indexTo + " is out of bounds for a TransferSystem of size " + networkMatrix.size());

        SystemNode nodeF = networkMatrix.get(indexFrom);
        SystemNode nodeT = networkMatrix.get(indexTo);

        return nodeF.addLink(indexTo) && nodeT.addLink(indexFrom);
    }

    /***
     * Create a link (edge) between the two given SystemNodes
     * This link is non-directed. It is added symmetrically to 
     * both provided nodes.
     * 
     * @throws NullPointerException If either provided SystemNode
     * doesn't exist within this TransferSystem
     * @param from SystemNode to link
     * @param to SystemNode to link
     * @return True if the SystemNodes were modified
     */
    public boolean link(SystemNode from, SystemNode to) {
        int indexFrom = getIndexOf(from);
        int indexTo = getIndexOf(to);
        if(indexFrom == -1)
            throw new NullPointerException("Failed to add a link to this TransferSystem - SystemNode " + 
                from + " does not exist in this TransferSystem!");
        if(indexTo == -1)
            throw new NullPointerException("Failed to add a link to this TransferSystem - SystemNode " + 
                to + " does not exist in this TransferSystem!");

        return from.addLink(indexTo) && to.addLink(indexFrom);
    }

    public boolean isNodeInBounds(int index) {
        return -1 < index && index < networkMatrix.size();
    }

    /***
     * Gets the numerical index of the provided SystemNode 
     * @param node Node to look for
     * @return Index of the node, or -1 if none is found.
     */
    public int getIndexOf(SystemNode node) {
        return networkMatrix.indexOf(node);
    }

    /***
     * Get a node in this network at the given index
     * @throws IndexOutOfBoundsException only when searching by index
     * @param index 
     * @return SystemNode at the given index
     */
    public SystemNode getNode(int index) {
        return networkMatrix.get(index);
    }

    /***
     * Get a node in this network at the given index
     * @throws IndexOutOfBoundsException only when searching by index
     * @param index 
     * @return SystemNode at the given index, or null if one couldn't be found
     */
    public SystemNode getNode(SystemNode member) {
        for(SystemNode compare : networkMatrix)
            if(compare.equals(member)) return compare;
        return null;
    }

    /***
     * Get a node in this network at the given index
     * @throws IndexOutOfBoundsException only when searching by index
     * @param index 
     * @return SystemNode at the given index, or null if one couldn't be found
     */
    public SystemNode getNode(BlockPos pos) {
        for(SystemNode compare : networkMatrix)
            if(compare.pos.equals(pos)) return compare;
        return null;
    }

    public LinkedList<SystemNode> getNetworkMatrix() {
        return networkMatrix;
    }
}
