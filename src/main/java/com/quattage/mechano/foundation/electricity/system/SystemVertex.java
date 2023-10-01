package com.quattage.mechano.foundation.electricity.system;

import java.util.LinkedList;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/***
 * A SystemNode is an approximation of a NodeBank. 
 * The lication of this SystemNode is stored as a SystemVertex, 
 * and links to other SystemVertices are stored in a list.
 */
public class SystemVertex {

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
    public boolean isMember;

    /***
     * Since a SystemNode is an approximation of a NodeBank, we must store the BlockPos of that NodeBank.
     * When in the right context, this BlockPos can be used to pull a NodeBank instance from the world.
     */
    //protected SystemVertex parent;

    private final BlockPos pos;
    private final int subIndex;


    /***
     * Storing links is required to build the matrix. A link between nodes can be thought of as an "edge," where
     * all links in the list are between this vertcex, and the specified vertex in this list.
     */
    protected LinkedList<SystemVertex> links = new LinkedList<SystemVertex>();


    public SystemVertex(BlockPos pos, int subIndex) {
        this.pos = pos;
        this.subIndex = subIndex;
        this.isMember = false;
    }

    public SystemVertex(BlockPos pos, int subIndex, boolean isMember) {
        this.pos = pos;
        this.subIndex = subIndex;
        this.isMember = isMember;
    }

    public SystemVertex(CompoundTag in) {
        this.pos = new BlockPos(
            in.getInt("x"),
            in.getInt("y"),
            in.getInt("z"));
        this.subIndex = in.getInt("i");

        if(in.contains("m"))
            this.isMember = in.getBoolean("m");
        else 
            this.isMember = false;

        readLinks(in.getList("link", Tag.TAG_COMPOUND));
    }

    public CompoundTag writeTo(CompoundTag in) {
        in.putInt("x", pos.getX());
        in.putInt("y", pos.getY());
        in.putInt("z", pos.getZ());
        in.putInt("i", subIndex);
        in.putBoolean("m", isMember);
        in.put("link", writeLinks());
        return in;
    }

    private ListTag writeLinks() {
        ListTag out = new ListTag();
        for(SystemVertex v : links) {
            CompoundTag coord = new CompoundTag();
            coord.putInt("x", v.pos.getX());
            coord.putInt("y", v.pos.getY());
            coord.putInt("z", v.pos.getZ());
            coord.putInt("i", v.subIndex);
            out.add(coord);
        }
        return out;
    }

    private void readLinks(ListTag list) {
        for(int x = 0; x < list.size(); x++)
            links.add(new SystemVertex(list.getCompound(x)));
    }

    /**
     * Gets the real-world position of this SystemNode
     * @return BlockPos
     */
    public BlockPos getPos() {
        return pos;
    }

    /***
     * Gets the sub index of this SystemNode
     * @return Int representing the index of this SystemNode's ElectricNode representation
     */
    public int getSubIndex() {
        return subIndex;
    }

    /***
     * Generate a unique identifier for this SystemVertex for storing in datasets
     * that require hashing.
     * @return a new SVID object representing this SystemVertex's BlockPos and SubIndex summarized as a String.
     */
    public SVID getSVID() {
        return new SVID(this);
    }

    public LinkedList<SystemVertex> connections() {
        return links;
    }

    /***
     * Returns a copy of this SystemNode at a given location
     * @param newPos Position that the copied node will hold
     * @return A copy of this SystemNode with its position set to the provided BlockPos
     */
    public SystemVertex getMovedCopy(BlockPos newPos) {
        return new SystemVertex(newPos, this.subIndex, this.isMember);
    }

    /***
     * Adds a link from the given SystemNode to this SystemNode.
     * @param other Other SystemNode within the given TransferSystem to add to this SystemNode
     * @return True if the list of connections within this SystemNode was changed.
     */
    protected boolean linkTo(SystemVertex other) {
        if(links.contains(other)) return false;
        return links.add(other);
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
        if(links.contains(newLink)) return false;
        return links.add(newLink);
    }

    /***
     * Removes the link from this SystemNode to the given SystemNode.
     * @param other
     */
    public void unlinkFrom(SystemVertex other) {
        links.remove(other);
    }

    /***
     * Removes the link from this SystemNode to the given SystemNode.
     * Does not perform any sanity checks to ensure that there is a valid
     * SystemNode at the givem BlockPos.
     * @param other
     */
    public void unlinkFrom(BlockPos otherPos, int subIndex) {
        links.remove(new SystemVertex(otherPos, subIndex));
    }

    /***
     * Checks whether this SystemNode is linked to the given SystemNode.
     * @param other SystemNode to check for 
     * @return True if this SystemNode contains a link to the given SystemNode
     */
    public boolean isLinkedTo(SystemVertex other) {
        return this.links.contains(other);
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
        return this.links.contains(new SystemVertex(otherPos, subIndex));
    }

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
            SystemVertex connectionTarget = links.get(x);
            content += connectionTarget.posAsString();
            if(x < links.size() - 1)
                content += ", ";
        }
        return sig + " -> " + content;
    }

    public String posAsString() {
        return "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ", i" + subIndex + "]";
    }

    public boolean equals(Object other) {
        if(other instanceof SystemVertex sl) {
            if(this.subIndex == -1 || sl.getSubIndex() == -1) 
                return this.pos.equals(sl.pos);
            return this.pos.equals(sl.pos) && this.subIndex == sl.subIndex;
        }
        return false;
    }

    public int hashCode() {
        if(subIndex < 0) return pos.hashCode();
        return pos.hashCode() + subIndex;
    }


}