package com.quattage.mechano.foundation.electricity.system;

import java.util.LinkedList;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import static com.quattage.mechano.foundation.electricity.system.GlobalTransferNetwork.NETWORK;

/***
 * A SystemVertex is an approximation of a NodeBank. 
 * The lication of this SystemVertex is stored as a SystemVertex, 
 * and links to other SystemVertices are stored in a list.
 */
public class SystemVertex {

    /***
     * Member status dictates whether this vertex interfaces with blocks outside of the TransferSystem.
     * If this boolean is false, this vertex will be skipped during iteration when signals are propagated
     */
    private boolean isMember = false; //TODO INIT SEQUENCE DOESN'T UPDATE THIS STATUS YET

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
        NETWORK.refreshVertex(this);
    }

    public SystemVertex(SystemVertex original, int subIndex) {
        this.pos = original.pos;
        this.subIndex = subIndex;
    }

    public SystemVertex(BlockPos pos, int subIndex, boolean isMember) {
        this.pos = pos;
        this.subIndex = subIndex;
        this.isMember = isMember;
        NETWORK.refreshVertex(this);
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
        NETWORK.refreshVertex(this);
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
            linkTo(new SystemVertex(list.getCompound(x)));
    }

    /**
     * Gets the real-world position of this SystemVertex
     * @return BlockPos
     */
    public BlockPos getPos() {
        return pos;
    }

    /***
     * Gets the sub index of this SystemVertex
     * @return Int representing the index of this SystemVertex's ElectricNode representation
     */
    public int getSubIndex() {
        return subIndex;
    }

    /***
     * Generate a unique identifier for this SystemVertex for storing in datasets
     * that require hashing.
     * @return a new SVID object representing this SystemVertex's BlockPos and SubIndex summarized as a String.
     */
    public SVID toSVID() {
        return new SVID(this);
    }

    public LinkedList<SystemVertex> connections() {
        return links;
    }

    /***
     * Returns a copy of this SystemVertex at a given location
     * @param newPos Position that the copied node will hold
     * @return A copy of this SystemVertex with its position set to the provided BlockPos
     */
    public SystemVertex getMovedCopy(BlockPos newPos) {
        return new SystemVertex(newPos, this.subIndex, this.isMember);
    }

    /***
     * Adds a link from the given SystemVertex to this SystemVertex.
     * @param other Other SystemVertex within the given TransferSystem to add to this SystemVertex
     * @return True if the list of connections within this SystemVertex was changed.
     */
    protected boolean linkTo(SystemVertex other) {
        if(links.contains(other)) return false;

        return links.add(other);
    }

    /***
     * Adds a link from the given SystemVertex to this SystemVertex.
     * Does not perform any sanity checks to ensure that there is a valid
     * SystemVertex at the givem BlockPos.
     * @param other Other SystemVertex within the given TransferSystem to add to this SystemVertex
     * @return True if the list of connections within this SystemVertex was changed.
     */
    protected boolean linkTo(BlockPos otherPos, int subIndex) {
        SystemVertex newLink = new SystemVertex(otherPos, subIndex);
        if(links.contains(newLink)) return false;
        return links.add(newLink);
    }

    /***
     * Removes the link from this SystemVertex to the given SystemVertex.
     * @param other
     * @returns True if this SystemVertex was modified as a result of this call
     */
    public boolean unlinkFrom(SystemVertex other) {
        return links.remove(other);
    }

    /***
     * Removes the link from this SystemVertex to all nodes at the given SVID safely
     * @param other
     * @returns True if this SystemVertex was modified as a result of this call
     */
    public boolean unlinkFromContextually(SVID other, TransferSystem sys) {
        boolean changed = false;
        for(SystemVertex vert : links) {
            if(vert.getPos().equals(other.getPos()))
                if(this.unlinkFrom(vert)) changed = true;
        }
        return changed;
    }

    /***
     * Checks whether this SystemVertex is linked to the given SystemVertex.
     * @param other SystemVertex to check for 
     * @return True if this SystemVertex contains a link to the given SystemVertex
     */
    public boolean isLinkedTo(SystemVertex other) {
        return this.links.contains(other);
    }

    /***
     * Checks whether this SystemVertex is linked to the given BlockPos.
     * Does not perform any sanity checks to ensure that there is a valid
     * SystemVertex at the givem BlockPos.
     * @param matrix TransferSystem to check
     * @param other SystemVertex to get from the links within this SystemVertex
     * @return True if this SystemVertex contains a link to the given SystemVertex
     */
    public boolean isLinkedTo(BlockPos otherPos, int subIndex) {
        return this.links.contains(new SystemVertex(otherPos, subIndex));
    }

    protected void unlinkAll() {
        links.clear();
    }

    /***
     * Checks whether this SystemVertex has any links attached to it
     * @return True if this SystemVertex doesn't contain any links
     */
    public boolean isEmpty() {
        if(links == null) return true;
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

    /***
     * Equivalence comparison by position. Does not compare links to other nodes.
     */
    public boolean equals(Object other) {
        if(other instanceof SystemVertex sl) {
            if(this.subIndex == -1 || sl.getSubIndex() == -1)
                return this.pos.equals(sl.pos);
            return this.pos.equals(sl.pos) && this.subIndex == sl.subIndex;
        }
        return false;
    }

    public int hashCode() {
        return pos.hashCode();
    }

    public boolean isMember() {
        return isMember;
        // return !isEmpty() && isMember;
    }

    public void setIsMember(boolean isMember) {
        this.isMember = isMember;
    }

    public void setIsMember() {
        setIsMember(true);
    }
}