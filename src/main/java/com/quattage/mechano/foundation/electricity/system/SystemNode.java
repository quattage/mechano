package com.quattage.mechano.foundation.electricity.system;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import net.minecraft.core.BlockPos;

/***
 * A SystemNode represents 3 things: <p>
 * - A LinkedList of all other SystemNodes this node is connected to (the Y axis of the adjacency matrix 
 *  stored within a TransferSystem instance), <p>
 * - An additional dimension, containing the BlockPos of the NodeBank this SystemNode represents <p>
 * - A boolean value, true if this SystemNode is a member (see {@link #isMember})
 */
public class SystemNode {

    /***
     * A System node can be a <strong>Member,</strong> or an <strong>Actor.</strong> <p>
     * 
     * <strong>Members</strong> within the TransferSystem are active participants which control the TransferSystem.
     * <strong>Actors</strong> within the TransferSystem are passive and have no control over the system.
     * Implementation should use this boolean to prevent excessive calls when iterating over a TransferSystem.
     * (You don't want to push or pull power when the node isn't connected to any external source) 
     * 
     * The end result: True if this SystemNode's parent NodeBank is connected to an external energy producer/consumer
     */
    protected boolean isMember;

    /***
     * Since a SystemNode is an approximation of a NodeBank, we must store the BlockPos of that NodeBank.
     * When in the right context, this BlockPos can be used to pull a NodeBank instance from the world.
     */
    protected BlockPos pos;

    /***
     * Represents the indexes of the other nodes that this SystemNode is connected to
     * within the parent TransferSystem.
     */
    protected LinkedList<Integer> links = new LinkedList<Integer>();

    // allows links to be sorted based on numeric value
    private static final Comparator<Integer> linkComparator = 
        new Comparator<Integer>() {
        public int compare(Integer n1, Integer n2) {
            return n1.compareTo(n2);
        }
    };

    public SystemNode(BlockPos pos) {
        this.pos = pos;
        isMember = true;
    }

    public SystemNode(BlockPos pos, boolean isMember) {
        this.pos = pos;
        this.isMember = isMember;
    }

    public SystemNode(BlockPos pos, boolean isMember, int[] connections) {
        this.pos = pos;
        this.isMember = isMember;
    }

    /***
     * Adds a link to this SystemNode. This link is just an arbitrary integer and isn't checked.
     * @param connectionIndex index in the TransferSystem to add to this SystemNode
     * @return True if the list of connections within this SystemNode was changed.
     */
    protected boolean addLink(int connectionIndex) {
        if(links.contains(connectionIndex)) return false;
        links.addLast(connectionIndex); return true;
    }

    /***
     * Adds a link from the given SystemNode to this SystemNode.
     * @param matrix TransferSystem to search within
     * @param other Other SystemNode within the given TransferSystem to add to this SystemNode
     * @return True if the list of connections within this SystemNode was changed.
     */
    protected boolean addLink(TransferSystem matrix, SystemNode other) {
        int x = matrix.getIndexOf(other);
        if(x == -1) throw new NullPointerException("Failed to add link - SystemNode '" + other + "' not found in the provided matrix!");
        return addLink(x);
    }

    /***
     * Sorts the connections made to this SystemNode.
     */
    public void sortLinks() {
        Collections.sort(links, linkComparator);
    }

    public void removeLink(int connection) {
        if(isLinkWithinBounds(connection))
            links.remove(connection);
    }

    /***
     * Checks whether this SystemNode is linked to the given SystemNode.
     * @param matrix TransferSystem to check
     * @param other SystemNode to get from the links within this SystemNode
     * @return True if this SystemNode contains a link to the given SystemNode
     */
    public boolean isLinkedTo(TransferSystem matrix, SystemNode other) {
        return this.links.contains(matrix.getIndexOf(other));
    }

    /***
     * Checks whether this SystemNode contains a link to the given index
     * @param matrix TransferSystem to check
     * @param index Index of the other SystemNode in the given TransferSystem
     * @return True if the SystemNode found at the provided index is connected to this SystemNode
     */
    public boolean isLinkedTo(TransferSystem matrix, int other) {
        return this.links.contains(other);
    }

    /***
     * Checks whether this SystemNode has a link at the given index
     * @param index Index within this SystemNode's connections list (the Y axis) to check.
     * @return True if this SystemNode has a link at the given index 
     */
    public boolean isLinkWithinBounds(int index) {
        return (index > -1) && index < (links.size() - 1);
    }

    public String toString() {
        String sig = isMember ? "M" : "A" + ", [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]";
        String content = "";
        for(int x = 0; x < links.size(); x++) {
            content += links.get(x);
            if(x < links.size() - 1)
                content += ", ";
        }
        return sig + " -> " + content;
    }

    public boolean equals(Object other) {
        if(other instanceof SystemNode n)
            return this.pos == n.pos;
        return false;
    }

    public int hashCode() {
        return this.pos.hashCode();
    }
}
