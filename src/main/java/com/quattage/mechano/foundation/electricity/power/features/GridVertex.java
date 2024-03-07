package com.quattage.mechano.foundation.electricity.power.features;

import java.util.LinkedList;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.power.LocalTransferGrid;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;

public class GridVertex {

    private boolean isMember = false;
    
    private final LocalTransferGrid parent;
    private final WireAnchorBlockEntity host;
    private final BlockPos pos;
    private final int subIndex;
    public LinkedList<GridVertex> links = new LinkedList<GridVertex>();


    public GridVertex(WireAnchorBlockEntity wbe, LocalTransferGrid parent, BlockPos pos, int subIndex) {
        this.parent = parent;
        this.pos = pos;
        this.subIndex = subIndex;
        this.host = wbe;
    }

    public GridVertex(WireAnchorBlockEntity wbe, LocalTransferGrid parent, GridVertex original, int subIndex) {
        this.parent = parent;
        this.pos = original.pos;
        this.subIndex = subIndex;
        this.host = wbe;
    }

    public GridVertex(WireAnchorBlockEntity wbe, LocalTransferGrid parent, BlockPos pos, int subIndex, boolean isMember) {
        this.parent = parent;
        this.pos = pos;
        this.subIndex = subIndex;
        this.isMember = isMember;
        this.host = wbe;
    }

    public GridVertex(LocalTransferGrid parent, CompoundTag in) {
        this.parent = parent;
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
        BlockEntity be = parent.getParent().getWorld().getBlockEntity(pos);
        if(be instanceof WireAnchorBlockEntity wbe) this.host = wbe;
        else this.host = null;
    }

    public void syncToHostBE() {
        if(host.isConnectedExternally() && !isEmpty()) {

            if(!isMember()) {
                setIsMember(true);
                parent.validatePathsFrom(this);
            }

            for(AnchorPoint anchor : host.getAnchorBank().getAll()) {
                if(anchor.getID().equals(this.getGID())) {
                    anchor.setParticipant(this);
                }
            }

        } else {

            if(isMember()) {
                setIsMember(false);
                // TODO clean paths
            }

            for(AnchorPoint anchor : host.getAnchorBank().getAll()) {
                if(anchor.getID().equals(this.getGID())) {
                    anchor.nullifyParticipant();
                }
            }
        }
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
        for(GridVertex v : links) {
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
            linkTo(new GridVertex(parent, list.getCompound(x)));
    }

    /**
     * Gets the real-world position of this GridVertex
     * @return BlockPos
     */
    public BlockPos getPos() {
        return pos;
    }

    /***
     * Gets the sub index of this GridVertex
     * @return Int representing the index of this GridVertex's ElectricNode representation
     */
    public int getSubIndex() {
        return subIndex;
    }

    /***
     * Generate a unique identifier for this GridVertex for storing in datasets
     * that require hashing.
     * @return a new SVID object representing this GridVertex's BlockPos and SubIndex summarized as a String.
     */
    public GID getGID() {
        return new GID(this);
    }

    public LinkedList<GridVertex> connections() {
        return links;
    }

    /***
     * Adds a link from the given GridVertex to this GridVertex.
     * @param other Other GridVertex within the given LocalTransferGrid to add to this GridVertex
     * @return True if the list of connections within this GridVertex was changed.
     */
    public boolean linkTo(GridVertex other) {
        if(links.contains(other)) return false;

        return links.add(other);
    }

    /***
     * Adds a link from the given GridVertex to this GridVertex.
     * Does not perform any sanity checks to ensure that there is a valid
     * GridVertex at the givem BlockPos.
     * @param other Other GridVertex within the given LocalTransferGrid to add to this GridVertex
     * @return True if the list of connections within this GridVertex was changed.
     */
    public boolean linkTo(BlockPos otherPos, int subIndex) {

        BlockEntity be = parent.getParent().getWorld().getBlockEntity(otherPos);
        if(!(be instanceof WireAnchorBlockEntity wbe)) return false;

        GridVertex newLink = new GridVertex(wbe, this.parent, otherPos, subIndex);
        if(links.contains(newLink)) return false;
        return links.add(newLink);
    }

    /***
     * Removes the link from this GridVertex to the given GridVertex.
     * @param other
     * @returns True if this GridVertex was modified as a result of this call
     */
    public boolean unlinkFrom(GridVertex other) {
        return links.remove(other);
    }

    /***
     * Removes the link from this GridVertex to all nodes at the given SVID safely
     * @param other
     * @returns True if this GridVertex was modified as a result of this call
     */
    public boolean unlinkEdgesToThisVertex(GID other, LocalTransferGrid sys) {
        boolean changed = false;
        for(GridVertex vert : links) {
            if(vert.getPos().equals(other.getPos())) {
                if(this.unlinkFrom(vert)) 
                    changed = true;
            }
        }
        return changed;
    }

    /***
     * Checks whether this GridVertex is linked to the given GridVertex.
     * @param other GridVertex to check for 
     * @return True if this GridVertex contains a link to the given GridVertex
     */
    public boolean isLinkedTo(GridVertex other) {
        return this.links.contains(other);
    }

    protected void unlinkAll() {
        links.clear();
    }

    /***
     * Checks whether this GridVertex has any links attached to it
     * @return True if this GridVertex doesn't contain any links
     */
    public boolean isEmpty() {
        if(links == null) return true;
        return links.isEmpty();
    }

    public String toString() {
        String sig = isMember ? "MEMBER" : "actor";
        String content = "at " + posAsString() + ", ->";
        for(int x = 0; x < links.size(); x++) {
            GridVertex connectionTarget = links.get(x);
            content += connectionTarget.posAsString();
            if(x < links.size() - 1)
                content += ", ";
        }
        return sig + " " + content;
    }

    public String posAsString() {
        return "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ", i" + subIndex + "]";
    }

    /***
     * Equivalence comparison by position. Does not compare links to other nodes.
     */
    public boolean equals(Object other) {
        if(other instanceof GridVertex sl) {
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

    public void sync(WireAnchorBlockEntity wbe) {
        
    }

    public LocalTransferGrid getParent() {
        return parent;
    }

    public void setIsMember(boolean isMember) {
        this.isMember = isMember;
    }

    public void setIsMember() {
        setIsMember(true);
    }
}