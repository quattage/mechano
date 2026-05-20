package com.quattage.mechano.grid.topology.core;

import com.quattage.mechano.grid.topology.AncillaryPair;
import com.quattage.mechano.switchboard.action.ActionTask;

/**
 * A dumb wrapper object whose entire purpose is to just contain
 * either a {@link Node}, {@link NodePair}, {@link AncillaryPair}, 
 * or {@link CircuitComponent}. For use by the 
 * {@link ActionTask#executeDeferred deferred execution} method of 
 * {@link ActionTask tasks} that support topological modifications.
 */
public class MutableComponentReference {

    public static MutableComponentReference of(Node node) { return new MutableComponentReference(node); }
    public static MutableComponentReference of(NodePair union) { return new MutableComponentReference(union); }
    public static MutableComponentReference of(AncillaryPair pair) { return new MutableComponentReference(pair); }
    public static MutableComponentReference of(CircuitComponent component) { return new MutableComponentReference(component); }

    private final Object obj;
    private MutableComponentReference(Object obj) { this.obj = obj; }

    public boolean isNode() { return obj instanceof Node; }
    public boolean isNodePair() { return obj instanceof NodePair; }
    public boolean isAncillaryPair() { return obj instanceof AncillaryPair; }
    public boolean isCircuitComponent() { return obj instanceof CircuitComponent; }

    public Node asNode() {
        if(!isNode()) throwBadType(Node.class);
        return (Node)obj;
    }

    public NodePair asNodePair() {
        if(!isNodePair()) throwBadType(NodePair.class);
        return (NodePair)obj;
    }

    public AncillaryPair asAncillaryPair() {
        if(!isAncillaryPair()) throwBadType(AncillaryPair.class);
        return (AncillaryPair)obj;
    }

    public CircuitComponent asComponent() {
        if(!isCircuitComponent()) throwBadType(CircuitComponent.class);
        return (CircuitComponent)obj;
    }

    private void throwBadType(Class<?> c) {
        throw new NullPointerException("MutableComponentReference of type '" 
            + obj.getClass().getTypeName() + "' can't provide an instance of '" + c.getTypeName() + "'");
    }

    @Override
    public String toString() {
        return "TopologyAudit[" + obj + "]";
    }
}
