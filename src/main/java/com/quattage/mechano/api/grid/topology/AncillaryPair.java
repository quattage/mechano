package com.quattage.mechano.api.grid.topology;

import java.util.Objects;

import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.grid.GridReferent;
import com.quattage.mechano.api.grid.topology.vertex.AncillaryNode;
import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.foundation.tracking.GridUUID;

public class AncillaryPair {

    protected GridUUID startID, endID;
    protected AncillaryNode startNode, endNode;

    public AncillaryPair(AncillaryNode startNode, AncillaryNode endNode) {
        assignStart(startNode);
        assignEnd(endNode);
    }

    public AncillaryPair(GridUUID startID, AncillaryNode startNode, GridUUID endID, AncillaryNode endNode) {
        assignStart(startID, startNode);
        assignEnd(endID, endNode);
    }

    public AncillaryPair flippedCopy() {
        return new AncillaryPair(endNode, startNode);
    }

    public AncillaryPair assignStart(AncillaryNode startNode) {
        Objects.requireNonNull(startNode);
        this.startNode = startNode;
        this.startID = startNode.bindUUID(startNode.getSource().getUUID());
        return this;
    }

    public AncillaryPair assignEnd(AncillaryNode endNode) {
        Objects.requireNonNull(endNode);
        this.endNode = endNode;
        this.endID = endNode.bindUUID(endNode.getSource().getUUID());
        return this;
    }

    public AncillaryPair assignStart(GridUUID startID, AncillaryNode startNode) {
        Objects.requireNonNull(startID);
        GridReferent.throwIfMismatch(startID, GridReferent.ANCILLARY_NODE);
        if(!startNode.isSignificant()) {
            throw new IllegalArgumentException("Failed while assigning ComponentLink starting point with " + startID 
                + " - The provided AncillaryNode is insignificant!");
        }
        Objects.requireNonNull(startNode);
        this.startID = startID;
        this.startNode = startNode;
        return this;
    }

    public AncillaryPair assignEnd(GridUUID endID, AncillaryNode endNode) {
        Objects.requireNonNull(endID);
        GridReferent.throwIfMismatch(endID, GridReferent.ANCILLARY_NODE);
        Objects.requireNonNull(endNode);
        if(!endNode.isSignificant()) {
            throw new IllegalArgumentException("Failed while assigning ComponentLink starting point with " + endID 
                + " - The provided AncillaryNode is insignificant!");
        }
        this.endID = endID;
        this.endNode = endNode;
        return this;
    }

    public void onAddedToGrid(Grid grid) {
        
    }

    public void onRemovedFromGrid(Grid grid) {
        
    }

    public GridUUID getStartID() {
        return startID;
    }
    
    public AncillaryNode getStartAncillary() {
        return startNode;
    }

    public Node getStartNode() {
        return (Node)getStartAncillary().getParentComponent();
    }

    public GridUUID getEndID() {
        return endID;
    }

    public AncillaryNode getEndAncillary() {
        return endNode;
    }

    public Node getEndNode() {
        return (Node)getEndAncillary().getParentComponent();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.startNode, this.endNode);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof AncillaryPair that)) return false;
        return this.startNode == that.startNode && this.endNode == that.endNode;
    }

    @Override
    public String toString() {
        return "[" + getStartID() + " -> " + getEndID() + "]";
    }

    public AncillaryPair validateSelf() {
        assertHasIDs();
        assertHasAncillaries();
        assertHasSignificance();
        assertHasSources();
        assertIDsMatch();
        assertNonConflict();
        return this;
    }

    private void assertHasIDs() {
        if(startID == null) {
            throw new NullPointerException("An operation failed on ComponentLink " + this 
                + " - The starting jack's UUID is null! (It was never assigned using assignStart())");
        }
        if(endID == null) {
            throw new NullPointerException("An operation failed on ComponentLink " + this 
                + " - The ending jack's UUID is null! (It was never assigned using assignEnd())");
        }
    }

    private void assertHasAncillaries() {
        if(startNode == null) {
            throw new NullPointerException("An operation failed on ComponentLink " + this 
                + " - The starting jack is null! (It was never assigned using assignStart())");
        }
        if(endNode == null) {
            throw new NullPointerException("An operation failed on ComponentLink " + this 
                + " - The ending jack is null! (It was never assigned using assignEnd())");
        }
    }

    private void assertHasSignificance() {
        if(!startNode.isSignificant()) {
            throw new IllegalStateException("An operation failed on ComponentLink " + this 
                + " - The starting jack is insignificant (This instance potentially leaked)");
        }
        if(!endNode.isSignificant()) {
            throw new IllegalStateException("An operation failed on ComponentLink " + this 
                + " - The ending jack is insignificant (This instance potentially leaked)");
        }
    }

    private void assertHasSources() {
        if(startNode.getSource() == null) {
            throw new IllegalStateException("An operation failed on ComponentLink " + this 
                + " - The starting jack has no source! (This instance potentially leaked)");
        }
        if(endNode.getSource() == null) {
            throw new IllegalStateException("An operation failed on ComponentLink " + this 
                + " - The ending jack has no source! (This instance potentially leaked)");
        }
    }

    private void assertIDsMatch() {
        GridUUID startIDRetrieved = startNode.bindUUID(startNode.getSource().getUUID());
        if(!startIDRetrieved.equals(startID)) {
            throw new IllegalStateException("An operation failed on ComponentLink " + this 
                + " - The starting jack returned a UUID that doesn't match! (got " + startIDRetrieved + ")");
        }
        GridUUID endIDRetrieved = endNode.bindUUID(endNode.getSource().getUUID());
        if(!endIDRetrieved.equals(endID)) {
            throw new IllegalStateException("An operation failed on ComponentLink " + this 
                + " - The ending jack returned a UUID that doesn't match! (got " + endIDRetrieved + ")");
        }
    }

    private void assertNonConflict() {
        if(startID.equals(endID)) {
            throw new IllegalStateException("An operation failed on ComponentLink " + this 
                + " - The starting and ending UUIDs are identical!");
        }
        if(startNode == endNode) {
            throw new IllegalStateException("An operation failed on ComponentLink " + this 
                + " - The starting and ending AncillaryNode instances are identical!");
        }
    }
}
