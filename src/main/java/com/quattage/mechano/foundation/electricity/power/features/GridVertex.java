package com.quattage.mechano.foundation.electricity.power.features;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.power.LocalTransferGrid;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/***
 * A GridVertex is a logical representation of a connection point within a LocalTransferGrid.
 * Where GridEdges represent the wires themselves, a GridVertex can be thought of as a connector.
 */
public class GridVertex {

    private boolean isMember = false;
    
    private static final VertexComparator comparator = new VertexComparator();
    private final LocalTransferGrid parent;
    private final WireAnchorBlockEntity host;
    private final BlockPos pos;
    private final int subIndex;

    private float f = 0;
    private float heuristic = 0;
    private float cumulative = Float.MAX_VALUE;

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

    public GridVertex(Level world, LocalTransferGrid parent, GID id) {
        this.parent = parent;
        this.pos = id.getPos();
        this.subIndex = id.getSubIndex();
        this.isMember = false;

        BlockEntity be = world.getBlockEntity(pos);
        if(be instanceof WireAnchorBlockEntity wbe) this.host = wbe;
        else this.host = null;
    }

    public GridVertex(LocalTransferGrid parent, CompoundTag in, Level world) {
        this.parent = parent;
        this.pos = new BlockPos(
            in.getInt("x"),
            in.getInt("y"),
            in.getInt("z"));
        this.subIndex = in.getInt("i");
        // TODO VERTS DONT SET THEIR MEMBER STATUS CORRECTLY IF THEY AREN'T FACING STRAIGHT UP
        readLinks(in.getList("l", Tag.TAG_COMPOUND), world);
        BlockEntity be = world.getBlockEntity(pos);
        if(be instanceof WireAnchorBlockEntity wbe) this.host = wbe;
        else this.host = null;
    }

    public CompoundTag writeTo(CompoundTag in) {
        in.putInt("x", pos.getX());
        in.putInt("y", pos.getY());
        in.putInt("z", pos.getZ());
        in.putInt("i", subIndex);
        in.putBoolean("m", this.isMember);
        in.put("l", writeLinks());
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

    public void readLinks(ListTag list, Level world) {
        for(int x = 0; x < list.size(); x++) {
            GID id = GID.of(list.getCompound(x));
            GridVertex target = parent.getOrCreateOnLoad(world, id);
            linkTo(target);
        }
    }

    public static VertexComparator getComparator() {
        return GridVertex.comparator;
    }

    public void syncToHostBE() {
        if(host.isConnectedExternally() && !isEmpty()) {

            Mechano.log("Member Flagged");

            for(AnchorPoint anchor : host.getAnchorBank().getAll()) {
                if(anchor.getID().equals(this.getGID())) {
                    anchor.setParticipant(this);
                }
            }

            if(!isMember()) {
                setIsMember(true);
                // do other things? maybe?
                // TODO this edge case may need special treatment, especially for moving contraptions
            }

        } else {

            for(AnchorPoint anchor : host.getAnchorBank().getAll()) {
                if(anchor.getID().equals(this.getGID())) {
                    anchor.nullifyParticipant();
                }
            }

            if(isMember()) {
                setIsMember(false);
                parent.removePathsEndingIn(this);
            }
        }
    }

    public void syncOnLoad() {
        if(isMember()) {
            for(AnchorPoint anchor : host.getAnchorBank().getAll()) {
                if(anchor.getID().equals(this.getGID())) 
                    anchor.setParticipant(this);
            }
        }
    }

    /**
     * Calculate a heuristic value for A* guidance and store it in this GridVertex.
     * @param other GridVertex to calculate heuristic with
     * @return The Euclidian distance between this vertex and the given vertex
     */
    public float getAndStoreHeuristic(GridVertex other) {
        BlockPos a = this.getPos();
        BlockPos b = other.getPos();
        this.heuristic = (float)Math.sqrt(Math.pow(a.getX() - b.getX(), 2f) + Math.pow(a.getY() - b.getY(), 2f) + Math.pow(a.getZ() - b.getZ(), 2f));
        this.f = this.cumulative + this.heuristic;
        return this.heuristic;
    }

    /**
     * Calculate a heuristic value for A* guidance and store it in this GridVertex
     * using the pre-computed heuristic value found within a pre-existing edge.
     * @param other GridEdge to grab the heuristic from
     * @return The Euclidian distance (length) of the given edge.
     */
    public float getAndStoreHeuristic(GridEdge edge) {
        this.heuristic = edge.getDistance();
        this.f = cumulative + heuristic;
        return this.heuristic;
    }

    public float getCumulative() {
        return this.cumulative;
    }

    public void setCumulative(float g) {
        this.cumulative = g;
    }

    public GridVertex reset() {
        this.f = 0;
        this.heuristic = 0;
        this.cumulative = Float.MAX_VALUE;
        return this;
    }

    public float getF() {
        return this.f;
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
     * @return a new GID object representing this GridVertex's BlockPos and SubIndex summarized as a String.
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
     * Removes the link from this GridVertex to the given GridVertex.
     * @param other
     * @returns True if this GridVertex was modified as a result of this call
     */
    public boolean unlinkFrom(GridVertex other) {
        return links.remove(other);
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
        String content = "at " + posAsString() + ", ---> ";
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
        if(other instanceof GridVertex ov) {
            return new GID(this.pos, this.subIndex).equals(new GID(ov.pos, ov.subIndex));
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

    public LocalTransferGrid getParent() {
        return parent;
    }

    public void setIsMember(boolean isMember) {
        this.isMember = isMember;
    }

    public void setIsMember() {
        setIsMember(true);
    }

    private static class VertexComparator implements Comparator<GridVertex> {

        @Override
        public int compare(GridVertex vertA, GridVertex vertB) {
            if(vertA.getF() > vertB.getF()) return 1;
            if(vertA.getF() < vertB.getF()) return -1;

            BlockPos a = vertA.getPos();
            BlockPos b = vertB.getPos();
            if(a.getX() > b.getX()) return 1;
            if(a.getX() < b.getX()) return -1;
            if(a.getY() > b.getY()) return 1;
            if(a.getY() < b.getY()) return -1;
            if(a.getZ() > b.getZ()) return 1;
            if(a.getZ() < b.getZ()) return -1;
            return 0;
        }
        
    }
}