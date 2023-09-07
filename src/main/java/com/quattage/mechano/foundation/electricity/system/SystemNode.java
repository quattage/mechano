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
     * Storing links is required to build the matrix. They're only stored by BlockPos, because this data is lightweight
     * and any additional information is easy enough to retrieve.
     */
    protected LinkedList<BlockPos> links = new LinkedList<BlockPos>();

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
     * Adds a link from the given SystemNode to this SystemNode.
     * @param other Other SystemNode within the given TransferSystem to add to this SystemNode
     * @return True if the list of connections within this SystemNode was changed.
     */
    protected boolean linkTo(SystemNode other) {
        if(links.contains(other.pos)) return false;
        return links.add(other.pos);
    }

    /***
     * Adds a link from the given SystemNode to this SystemNode.
     * Does not perform any sanity checks to ensure that there is a valid
     * SystemNode at the givem BlockPos.
     * @param other Other SystemNode within the given TransferSystem to add to this SystemNode
     * @return True if the list of connections within this SystemNode was changed.
     */
    protected boolean linkTo(BlockPos otherPos) {
        if(links.contains(otherPos)) return false;
        return links.add(otherPos);
    }

    /***
     * Removes the link from this SystemNode to the given SystemNode.
     * @param other
     */
    public void unlinkFrom(SystemNode other) {
        links.remove(other.getPos());
    }

    /***
     * Removes the link from this SystemNode to the given SystemNode.
     * Does not perform any sanity checks to ensure that there is a valid
     * SystemNode at the givem BlockPos.
     * @param other
     */
    public void unlinkFrom(BlockPos otherPos) {
        links.remove(otherPos);
    }

    /***
     * Checks whether this SystemNode is linked to the given SystemNode.
     * @param other SystemNode to check for 
     * @return True if this SystemNode contains a link to the given SystemNode
     */
    public boolean isLinkedTo(SystemNode other) {
        return this.links.contains(other.getPos());
    }

    /***
     * Checks whether this SystemNode is linked to the given BlockPos.
     * Does not perform any sanity checks to ensure that there is a valid
     * SystemNode at the givem BlockPos.
     * @param matrix TransferSystem to check
     * @param other SystemNode to get from the links within this SystemNode
     * @return True if this SystemNode contains a link to the given SystemNode
     */
    public boolean isLinkedTo(BlockPos otherPos) {
        return this.links.contains(otherPos);
    }

    /// This stuff was here because SystemNodes used to be stored in the TransferSystem by index.
    /// this caused problems. They're hashed based on BlockPos now.
    /// I'm not sure if this code was ever committed so it'll stay commented out in case it is needed later.
    // /***
    //  * Modifies all linked indicies by the given integer.
    //  * Used for combining and splitting TransferSystems.
    //  * @param amount Amount to shift by
    //  */
    // protected void shift(int amount) {
    //     for(int x = 0; x < links.size(); x++) {
    //         int shifted = links.get(x) + amount;
    //         links.set(x, shifted);
    //     }
    // }

    // /***
    //  * Unlinks all SystemNodes after the given index.
    //  * @param index Index to start removing at
    //  * @param inclusive True if the operation includes the node at the index
    //  */
    // protected void unlinkGreater(int index, boolean inclusive) {
    //     if(!isLinkWithinBounds(index)) 
    //         throw new ArrayIndexOutOfBoundsException("Failed to unlink - Index '" 
    //             + index + "' is out of bounds for this SystemNode!");
    //     for(int x = inclusive ? index : index + 1; x < links.size(); x++)
    //         removeLink(x);
    // }

    // // TODO ENSURE INSERTION ORDER IS ENFORCED IN ASCENDING ORDER OR THESE WONT WORK

    // /***
    //  * Unlinks all SystemNodes before the given index.
    //  * @param index Index to stop removing at
    //  * @param inclusive True if the operation includes the node at the index
    //  */
    // protected void unlinkLesser(int index, boolean inclusive) {
    //     if(!isLinkWithinBounds(index)) 
    //         throw new ArrayIndexOutOfBoundsException("Failed to unlink - Index '" 
    //             + index + "' is out of bounds for this SystemNode!");
    //     for(int x = 0; inclusive ? x <= index : x < index; x++)
    //         removeLink(x);
    // }

    protected void unlinkAll() {
        links.clear();
    }

    /***
     * Checks whether this SystemNode has any links attached to it
     * @return True if this SystemNode doesn't contain any links
     */
    public boolean isEmpty() {
        return links.isEmpty();
    }

    public String toString() {
        String sig = isMember ? "M" : "A" + ", [";
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

    public BlockPos getPos() {
        return pos;
    }
}
