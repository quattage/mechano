package com.quattage.mechano.foundation.electricity.system;

import java.util.LinkedList;

import net.minecraft.core.BlockPos;

/***
 * A SystemNode is an approximation of a NodeBank. 
 * The lication of this SystemNode is stored as a SystemVertex, 
 * and links to other SystemVertices are stored in a list.
 */
public class SystemNode {

    /***
     * A System node can be a <strong>Member,</strong> or an <strong>Actor.</strong> <p>
     * 
     * <strong>Members</strong> within the TransferSystem are active participants which control the TransferSystem.
     * <strong>Actors</strong> within the TransferSystem are passive and have no control over the system.
     * Implementation should use this boolean to prevent excessive calls when iterating over a TransferSystem.
     * (You don't want to push or pull power when the node isn't connected to any external source)
     * If this boolean is true, this SystemNode is marked as being an active participant in this network, which can
     * send or receive power. If it is false, this node is not connected to any external producers/consumers, and will
     * basically just be "fake."
     */
    protected boolean isMember;

    /***
     * Since a SystemNode is an approximation of a NodeBank, we must store the BlockPos of that NodeBank.
     * When in the right context, this BlockPos can be used to pull a NodeBank instance from the world.
     */
    protected SystemVertex parent;
    
    /***
     * Storing links is required to build the matrix. A link between nodes can be thought of as an "edge," where
     * all links in the list are between this vertcex, and the specified vertex in this list.
     */
    protected LinkedList<SystemVertex> linkedVertices = new LinkedList<SystemVertex>();

    public SystemNode(SystemVertex parent) {
        this.parent = parent;
        isMember = true;
    }

    public SystemNode(SystemVertex parent, boolean isMember) {
        this.parent = parent;
        this.isMember = isMember;
    }

    public SystemNode(SystemVertex parent, boolean isMember, int[] connections) {
        this.parent = parent;
        this.isMember = isMember;
    }

    public LinkedList<SystemVertex> values() {
        return linkedVertices;
    }

    /***
     * Adds a link from the given SystemNode to this SystemNode.
     * @param other Other SystemNode within the given TransferSystem to add to this SystemNode
     * @return True if the list of connections within this SystemNode was changed.
     */
    protected boolean linkTo(SystemNode other) {
        SystemVertex otherParent = other.getParent();
        if(linkedVertices.contains(otherParent)) return false;
        return linkedVertices.add(otherParent);
    }

    /***
     * Adds a link from the given SystemNode to this SystemNode.
     * Does not perform any sanity checks to ensure that there is a valid
     * SystemNode at the givem BlockPos.
     * @param other Other SystemNode within the given TransferSystem to add to this SystemNode
     * @return True if the list of connections within this SystemNode was changed.
     */
    protected boolean linkTo(BlockPos otherPos, int subIndex) {
        SystemVertex newLink = new SystemVertex(otherPos, subIndex);
        if(linkedVertices.contains(newLink)) return false;
        return linkedVertices.add(newLink);
    }

    /***
     * Removes the link from this SystemNode to the given SystemNode.
     * @param other
     */
    public void unlinkFrom(SystemNode other) {
        linkedVertices.remove(other.getParent());
    }

    /***
     * Removes the link from this SystemNode to the given SystemNode.
     * Does not perform any sanity checks to ensure that there is a valid
     * SystemNode at the givem BlockPos.
     * @param other
     */
    public void unlinkFrom(BlockPos otherPos, int subIndex) {
        linkedVertices.remove(new SystemVertex(otherPos, subIndex));
    }

    /***
     * Checks whether this SystemNode is linked to the given SystemNode.
     * @param other SystemNode to check for 
     * @return True if this SystemNode contains a link to the given SystemNode
     */
    public boolean isLinkedTo(SystemNode other) {
        return this.linkedVertices.contains(other.getParent());
    }

    /***
     * Checks whether this SystemNode is linked to the given BlockPos.
     * Does not perform any sanity checks to ensure that there is a valid
     * SystemNode at the givem BlockPos.
     * @param matrix TransferSystem to check
     * @param other SystemNode to get from the links within this SystemNode
     * @return True if this SystemNode contains a link to the given SystemNode
     */
    public boolean isLinkedTo(BlockPos otherPos, int subIndex) {
        return this.linkedVertices.contains(new SystemVertex(otherPos, subIndex));
    }

    protected void unlinkAll() {
        linkedVertices.clear();
    }

    /***
     * Checks whether this SystemNode has any links attached to it
     * @return True if this SystemNode doesn't contain any links
     */
    public boolean isEmpty() {
        return linkedVertices.isEmpty();
    }

    public String toString() {
        String sig = isMember ? "M" : "A" + ", [";
        String content = "";
        for(int x = 0; x < linkedVertices.size(); x++) {
            SystemVertex thisLink = linkedVertices.get(x);
            content += thisLink.toString();
            if(x < linkedVertices.size() - 1)
                content += ", ";
        }
        return sig + " -> " + content;
    }

    public boolean equals(Object other) {
        if(other instanceof SystemNode n)
            return this.parent == n.parent;
        return false;
    }

    public int hashCode() {
        return this.parent.hashCode();
    }

    public SystemVertex getParent() {
        return parent;
    }

    public BlockPos getPos() {
        return parent.getPos();
    }
}