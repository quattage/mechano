package com.quattage.mechano.foundation.api.landmarks;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.ApiStatus;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.PowerGrid;
import com.quattage.mechano.foundation.api.PowerGridBlockEntity;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;




/**
 * A GridNode is a functional implementation of {@link NodeIdentifier} and provides 
 * access to the Y axis of an adjacency list defined by the {@link PowerGrid}.
 * Nodes are hashed by <code>X, Y, Z, and I</code>, where <code>XYZ</code> describes the 
 * position in the world, and <code>I</code> is the index of the node at that block 
 * position. Multiple nodes may occupy the same block.
 */
public class GridNode extends NodeIdentifier<GridNode> {

    public final PowerGrid owner;
    public final PowerGridBlockEntity host;

    /**
     * A list of links to other nodes.
     * This should never be modified directly.
     * Make any changes you need through the 
     * {@link com.quattage.mechano.foundation.api.GlobalServerGrid GlobalServerGrid}
     */
    @ApiStatus.Internal
    public final List<GridLink> links = new ObjectArrayList<>();


    public GridNode(PowerGrid owner, PowerGridBlockEntity host, int index) {
        super(host.getBlockPos(), index);
        Objects.requireNonNull(owner);
        Objects.requireNonNull(host);
        this.owner = owner;
        this.host = host;
        owner.nodes.add(this);
    }


    /**
     * Tests the integrity of the data contained within this GridNode
     * to determine its relevlence. If <code>false,</code> an error will be 
     * printed to the console. Invalid nodes indicate some kind of serialization
     * or level loading issue that should be addressed. If all is working,
     * this check is entirely unncessary, as GridNode instances should
     * never be allowed to enter a state where calls to this method return false.
     * @return <code>true</code> if this GridNode is valid.
     */
    public boolean isValid() {
        if(links.isEmpty()) {
            Mechano.LOGGER.warn("GridNode at " + this + " was found to have no links and failed validity checks.");
            return false;
        }
        if(host == null) {
            Mechano.LOGGER.warn("GridNode at " + this + " doesn't have a host and failed validity checks.");
            return false;
        }
        if(!host.getBlockPos().equals(getPos())) {
            Mechano.LOGGER.warn("GridNode at " + this + " node doesn't match its provided host at (" + host.getBlockPos() + "), validity checks failed.");
            return false;
        }
        return true;
    }

    /**
     * Creates a new {@link Tracker} instance from this GridNode. TrackedNodes
     * @return A new TrackedNode instance for hashing and pathfinding
     */
    @Override
    public Tracker makeTrackable() {
        return new Tracker(this);
    }

    public void forEachLink(Consumer<GridLink> cons) {
        for(int x = 0; x < links.size(); x++) {
            cons.accept(links.get(x));
        }
    }

    public GridNode getValue() {
        return this;
    }

    public void wipeLinks(boolean notify) {
        Iterator<GridLink> it = links.iterator();
        while(it.hasNext()) {
            GridLink thisLink = it.next();
            it.remove();
            if(!notify) continue;
            thisLink.transmitter.onConnectionDestroyed(owner.getWorld(), null, thisLink);
            host.onConnectionBroken(owner.getWorld(), thisLink);
        }
    }

    public boolean hasLinks() {
        return links.size() > 0;
    }

    /**
     * Determines whether or not the given node is functionally identical to this one.
     * Differs from {@link NodeIdentifier#equals equals} in that this method compares
     * more than just its internally-hashed address.
     * @param other Node to compare
     * @return <code>true</code> if the given node shares the same position, parent, and links as the given node.
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
     * An object that wraps {@link GridNode} instances
     * while providing additional capabilities that
     * GridNodes only need intermittently.
     * 
     * A {@link PowerGrid} uses instances of this class
     * to maintain visitation history for pathing guidance.
     * This ensures safe access while eliminating multiple transient
     * variables from having to be declared in the GridNode itself.
     */
    public static class Tracker implements NodeIdentifiable<GridNode>, Comparable<Tracker> {

        private float f = 0;
        private float heur = 0;
        private float cum = Float.MAX_VALUE;
        private boolean visited = false;
        public final GridNode node;

        public float getF() {
            return f;
        }

        protected Tracker(GridNode node) {
            this.node = node;
        }

        /**
         * Primes this TrackedNode for pathfinding by estimating
         * a heuristic cost to the target address
         * @param target 
         * @return This TrackedNode with modified guidance values
         */
        public Tracker estimateCostTo(NodeIdentifiable<?> target) {
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
         * @return <code>true</code> if this address is worth investigating while pathfinding
         */
        public boolean investigateAcross(GridLink link, Tracker neighbor) {
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
        public int compareTo(Tracker that) {
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
        public Tracker makeTrackable() {
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
            Mechano.LOGGER.warn("Potential bad access - " + this + " was serialized to NBT. (This instanec has probably leaked!)");
            return node.writeTo(in);
        }

        @Override
        public CompoundTag writeOnlyAddress(CompoundTag in) {
            Mechano.LOGGER.warn("Potential bad access - " + this + " was serialized to NBT. (This instanec has probably leaked!)");
            return node.writeOnlyAddress(in);
        }
    }
}
