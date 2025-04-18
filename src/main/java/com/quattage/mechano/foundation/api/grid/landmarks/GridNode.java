package com.quattage.mechano.foundation.api.grid.landmarks;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;


import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.PowerGridBlockEntity;
import com.quattage.mechano.foundation.api.grid.PowerGrid;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A GridNode is a functional implementation of {@link NodeIdentifier} and provides 
 * access to the Y axis of an adjacency list defined by the {@link PowerGrid}.
 * Nodes are hashed by <code>X, Y, Z, and I</code>, where <code>XYZ</code> describes the 
 * position in the world, and <code>I</code> is the index of the node at that block 
 * position. Multiple nodes may occupy the same block.
 */
public class GridNode extends NodeIdentifier<GridNode> {

    private final PowerGrid owner;
    private final PowerGridBlockEntity host;
    private final List<GridLink> links = new ObjectArrayList<>();

    public static GridNode loadFrom(PowerGrid owner, LevelReader world, CompoundTag tag) {
        if(!(tag.contains("x") && tag.contains("y") && tag.contains("z") && tag.contains("i"))) 
            throw new IllegalArgumentException("Cannot instantiate a Provisional GridNode from compound '" + tag + "' - this CompoundTag is missing the required data!");
        BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
        BlockEntity be = world.getBlockEntity(pos);
        if(be == null) throw new IllegalArgumentException("Cannot instantiate a Provisional GridNode at " + pos + " - there is no BlockEntity at this location!");
        if(!(be instanceof PowerGridBlockEntity pgbe))
        throw new IllegalArgumentException("Cannot instantiate a Provisional GridNode at " + pos + " - the BlockEntity at this location is not an instance of PowerGridBlockEntity!");
        return new GridNode(owner, pgbe, pos, tag.getByte("i"));
    }

    public GridNode(PowerGrid owner, PowerGridBlockEntity host, BlockPos pos, int index) {
        super(pos, index);
        this.owner = owner;
        this.host = host;
    }

    /**
     * Tests the integrity of the data contained within this GridNode
     * to determine its relevlence. If <code>false,</code> an error will be 
     * printed to the console. Invalid nodes indicate some kind of serialization
     * or level loading issue that should be addressed. If all is working,
     * this check is entirely unncessary, as GridNode instances should
     * never be allowed to enter a state where calls to this method return false.
     * @return <code>TRUE</code> if this GridNode is valid.
     */
    public boolean isValid() {
        if(links.isEmpty()) {
            Mechano.LOGGER.error(this + " was found to have no links and failed validity checks.");
            return false;
        }
        if(host == null) {
            Mechano.LOGGER.error(this + " was found to have a null host and failed validity checks.");
            return false;
        }
        if(!host.getBlockPos().equals(getPos())) {
            Mechano.LOGGER.error(this + " This node doesn't match its provided host at (" + host.getBlockPos() + "), validity checks failed.");
            return false;
        }
        return true;
    }

    /**
     * Creates a new {@link TrackedNode} instance from this GridNode. TrackedNodes
     * @return A new TrackedNode instance for hashing and pathfinding
     */
    @Override
    public TrackedNode makeTrackable() {
        return new TrackedNode(this);
    }

    public void forEachLink(Consumer<GridLink> cons) {
        for(int x = 0; x < links.size(); x++) {
            cons.accept(links.get(x));
        }
    }

    public GridNode getValue() {
        return this;
    }

    public void wipeLinks() {
        Iterator<GridLink> it = links.iterator();
        while(it.hasNext()) {
            GridLink thisLink = it.next();
            it.remove();
            thisLink.getConnection().onConnectionDestroyed(owner.getWorld(), null, thisLink);
        }
    }

    public String toString() {
        return "GridNode " + super.toString();
    }

    /**
     * Determines whether or not the given node is functionally identical to this one.
     * Differs from {@link NodeIdentifier#equals equals} in that this method compares
     * more than just its internally-hashed address.
     * @param other Node to compare
     * @return <code>TRUE</code> if the given node shares the same position, parent, and links as the given node.
     */
    public boolean isExactMatch(GridNode other) {
        if(other == this) return true;
        if(!this.equals(other)) return false;
        if(!this.host.equals(other.host)) return false;
        if(this.links.size() != other.links.size()) return false;
        for(int x = 0; x < links.size(); x++) {
            if(!this.links.get(x).equals(other.links.get(x)))
                return false;
        }
        return true;
    }

    @Override
    public CompoundTag writeTo(CompoundTag in) {
        super.writeTo(in);
        ListTag serializedLinks = new ListTag();
        for(GridLink link : links) {
            serializedLinks.add(link.writeTo(new CompoundTag()));
        }
        in.put("links", serializedLinks);
        return in;
    }





    /**
     * A dummy implementation of {@link NodeIdentifier} useful as
     * a stand-in replacement for {@link GridNode} instances when
     * retrieving them from the {@link com.quattage.mechano.foundation.api.grid.PowerGrid PowerGrid}.
     */
    public static class Address extends NodeIdentifier<Address> {

        public static Address loadFrom(CompoundTag tag) {
            if(tag == null) throw new NullPointerException("Cannot instantiate a Provisional GridNode from a null CompoundTag!");
            if(!(tag.contains("x") && tag.contains("y") && tag.contains("z") && tag.contains("i"))) 
                throw new IllegalArgumentException("Cannot instantiate a Provisional GridNode from compound '" + tag + "' - this CompoundTag is missing the required data!");
            BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
            return new Address(pos, tag.getByte("i"));
        }

        public Address(BlockPos pos, int index) {
            super(pos, index);
        }
        @Override
        public Address getValue() {
            return this;
        }
		@Override
		public TrackedNode makeTrackable() {
			throw new UnsupportedOperationException("Dummy addresses aren't trackable!");
		}

        public String toString() {
            return "Node (" + getX() + "," + getY() + "," + getZ() + "," + getIndex() + ")";
        }
    }

























    /**
     * An object that wraps {@link GridNode} instances.
     * while providing additional capabilities that
     * GridNodes only need intermittently.
     * 
     * A {@link PowerGrid} uses instances of this class
     * to maintain visitation history for pathing guidance.
     * This ensures safe access, and elimites multiple transient
     * variables from having to be declared in the GridNode itself.
     */
    public static class TrackedNode implements NodeIdentifiable<GridNode>, Comparable<TrackedNode> {

        private float f = 0;
        private float heur = 0;
        private float cum = Float.MAX_VALUE;
        private boolean visited = false;
        public final GridNode node;

        public float getF() {
            return f;
        }

        protected TrackedNode(GridNode node) {
            this.node = node;
        }

        /**
         * Primes this TrackedNode for pathfinding by estimating
         * a heuristic cost to the target address
         * @param target 
         * @return This TrackedNode with modified guidance values
         */
        public TrackedNode prime(NodeIdentifiable<?> target) {
            updateHeuristic(target);
            this.visited = true;
            this.cum = 0;
            return this;
        }

        /**
         * Update the heuristic to this address based
         * directly upon the in-world position of the
         * target address.
         * @param target 
         * @return The updated heuristic value
         */
        public float updateHeuristic(NodeIdentifiable<?> target) {
            this.heur = GridLink.getEuclideanDistance(this, target);
            this.f = cum + heur;
            return heur;
        }

        /**
         * Updates the heuristic to this address based on the 
         * traversal cost of the given {@link GridLink}
         * Modifies this address in-place with the new heuristic and f values.
         * @param link GridLink describing the connection to be traversed.
         * @return The new heuristic value stored by this address after modification.
         */
        public float updateWeightedHeuristic(GridLink link) {
            this.heur = link.calculateTraversalCost();
            this.f = cum + heur;
            return heur;
        }

        /**
         * Investigates the given <code>neighbor</code> across the given <code>link</code>
         * Returns a boolean representing whether the path described by the link and neighbor should be
         * addressed. 
         * @param link The link across which the investigation is occuring
         * @param neighbor The neighboring node to investigate. Should have the same address as the link's target
         * @return <code>TRUE</code> if this address is worth investigating while pathfinding
         */
        public boolean investigateAcross(GridLink link, TrackedNode neighbor) {
            if(neighbor.visited) return false;
            if(!link.startsWith(this)) return false;
            if(!link.endsWith(neighbor)) return false;
            float tentative = this.updateWeightedHeuristic(link) + this.cum;
            if(tentative < neighbor.cum) {
                neighbor.cum = tentative;
                neighbor.updateWeightedHeuristic(link);
                return true;
            }
            return false;
        }

        /**
         * Resets the heuristic values in this TrackedNode to defaults.
         * Not necessary for most circumstances, but can be useful for 
         * reusing instances.
         */
        public void reset() {
            this.f = 0;
            this.heur = 0;
            this.cum = Float.MAX_VALUE;
            this.visited = false;
        }

        /**
         * Gets the {@link GridNode} instance wrapped
         * by this TrackedNode
         */
        @Override
        public GridNode getValue() {
            return node;
        }

        /**
         * Compares the heuristic cost of this TrakcedNode
         * to the given TrackedNode
         */
        @Override
        public int compareTo(TrackedNode that) {
            if(this.f > that.f) return 1;
            if(this.f < that.f) return -1;
            return 0;
        }

        @Override
        public BlockPos getPos() {
            return node.getPos();
        }

        @Override
        public int getIndex() {
            return node.getIndex();
        }

        @Override
        public TrackedNode makeTrackable() {
            Mechano.LOGGER.warn("The node " + this + " was already a TrackedNode instance, but a call was made to makeTrackable()");
            return this;
        }

        public void markVisited() {
            this.visited = true;
        }

        public String toString() {
            return "TrackedNode (" + getX() + "," + getY() + "," + getZ() + "," + getIndex() + ")";
        }

        @Override
        public CompoundTag writeTo(CompoundTag in) {
            Mechano.LOGGER.error(this + " was serialized to NBT. This indicates bad access and a potential memory leak.");
            return node.writeTo(in);
        }

        @Override
        public CompoundTag writeOnlyAddress(CompoundTag in) {
            Mechano.LOGGER.error(this + " was serialized to NBT. This indicates bad access and a potential memory leak.");
            return node.writeOnlyAddress(in);
        }
    }
}
