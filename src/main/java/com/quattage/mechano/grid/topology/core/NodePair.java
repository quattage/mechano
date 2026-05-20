package com.quattage.mechano.grid.topology.core;

import com.quattage.mechano.api.Griddable;
import com.quattage.mechano.grid.GridTracking;
import com.quattage.mechano.grid.topology.core.HierarchicalConstruct.GridReferent;
import com.quattage.mechano.grid.topology.core.HierarchicalConstruct.SourceProvider;

public class NodePair implements SourceProvider {
    
    protected Node a;
    protected Node b;

    public NodePair() {}

    public NodePair(Node a, Node b) {
        this.a = a;
        this.b = b;
    }

    public NodePair inverse() {
        assertHasNodes();
        return new NodePair(b, a);
    }

    public Node getNodeA() {
        return a;
    }

    public Node getNodeB() {
        return b;
    }

    public Griddable<?> getSourceA() {
        return GridTracking.getReferentOrThrow(a);
    }

    public Griddable<?> getSourceB() {
        return GridTracking.getReferentOrThrow(b);
    }

    @Override
    public GridReferent<?> getReferent() {
        return getSourceA();
    }

    @Override
    public final boolean equals(Object obj) {
        if(this == obj) return true;
        if(!(obj instanceof NodePair that)) return false;
        return (this.a == that.a && this.b == that.b) || (this.a == that.b && this.b == that.a);
    }

    @Override
    public final int hashCode() {
        return a.hashCode() * b.hashCode();
    }

    protected void assertHasSources() {
        if(a.getReferent() == null) {
            throw new IllegalStateException("An operation failed on " + this 
                + " - The starting ancillary has no source! (This instance potentially leaked)");
        }
        if(b.getReferent() == null) {
            throw new IllegalStateException("An operation failed on " + this 
                + " - The ending ancillary has no source! (This instance potentially leaked)");
        }
    }

    protected void assertNonConflict() {
        if(a == b) {
            throw new IllegalStateException("An operation failed on " + this 
                + " - The starting and ending AncillaryNode<?> instances are identical!");
        }
    }

    protected void assertHasNodes() {
        if(a == null) {
            throw new NullPointerException("An operation failed on ComponentLink " + this 
                + " - The starting ancillary is null! (It was never assigned using assignStart())");
        }
        if(b == null) {
            throw new NullPointerException("An operation failed on ComponentLink " + this 
                + " - The ending ancillary is null! (It was never assigned using assignEnd())");
        }
    }

    @Override
    public String toString() {
        return "NodePair[ " + a + "  ->  " + b + " ]";
    }
}
