package com.quattage.mechano.foundation.api.landmark.uuid.impl;

import com.quattage.mechano.foundation.api.landmark.GridLink;
import com.quattage.mechano.foundation.api.landmark.uuid.GridUUID;

public class HeuristicUUID implements Comparable<HeuristicUUID> {

    private final GridUUID addr;

    private float f = 0;
    private float heur = 0;
    private float cum = Float.MAX_VALUE; // we stay mature here at quatworks ltd
    private boolean visited = false;

    public float getF() {
        return f;
    }

    public HeuristicUUID(GridUUID addr) {
        this.addr = addr;
    }

    /**
     * Investigates the given <code>neighbor</code> across the given <code>link</code>
     * Returns a boolean representing whether the path described by the link and neighbor should be
     * addressed. 
     * @param link The link across which the investigation is occuring
     * @param neighbor The neighboring node to investigate. Should have the same address as the link's target
     * @return <code>true</code> if this address is worth investigating while pathfinding
     */
    public boolean investigateAcross(GridLink link, HeuristicUUID neighbor) {
        if(neighbor.visited) return false;
        if(!link.startsWith(this.addr)) return false;
        if(!link.endsWith(neighbor.addr)) return false;
        float tentative = this.updateWeightedHeuristic(link) + this.cum;
        if(tentative < neighbor.cum) {
            neighbor.cum = tentative;
            neighbor.updateWeightedHeuristic(link);
            return true;
        }
        return false;
    }

    /**
     * Primes this TrackedNode for pathfinding by estimating
     * a heuristic cost to the target address
     * @param target 
     * @return This TrackedNode with modified guidance values
     */
    public HeuristicUUID estimateCostTo(GridUUID target) {
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
    public float updateHeuristic(GridUUID target) {
        this.heur = GridLink.getEuclideanDistance(this.addr, target);
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

    public void markVisited() {
        this.visited = true;
    }

    public GridUUID getAddress() {
        return addr;
    }

    /**
     * Compares the heuristic cost of this TrakcedNode
     * to the given TrackedNode
     */
    @Override
    public int compareTo(HeuristicUUID that) {
        if(this.f > that.f) return 1;
        if(this.f < that.f) return -1;
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return addr.equals(obj);
    }

    @Override
    public int hashCode() {
        return addr.hashCode();
    }
}