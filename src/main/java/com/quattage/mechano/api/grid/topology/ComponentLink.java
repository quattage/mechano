package com.quattage.mechano.api.grid.topology;

import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.component.ComponentTracker.ComponentHierarchy;
import com.quattage.mechano.api.grid.component.ComponentUUID;
import com.quattage.mechano.api.grid.component.ComponentUUID.ComponentBinding;
import com.quattage.mechano.api.grid.component.GridConstruct;
import com.quattage.mechano.api.grid.topology.vertex.AncillaryNode;
import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.api.transmitter.TransmitterType;
import com.quattage.mechano.api.transmitter.TransmitterType.UnionFactory;

/**
 * A link that connects two {@link AncillaryNode ancillaries}
 * together, like a wire.
 */
public class ComponentLink<T extends CircuitComponent> extends AncillaryPair implements CircuitComponent, GridConstruct {

    private final TransmitterType trns;
    private boolean isInstantiated = false;
    private CircuitComponent component;

    public ComponentLink(TransmitterType trns, AncillaryNode<?> startNode, AncillaryNode<?> endNode) {
        super(startNode, endNode);
        Objects.requireNonNull(trns);
        this.trns = trns;
    }

    public ComponentLink(TransmitterType trns, ComponentUUID<?> startID, AncillaryNode<?> startNode, ComponentUUID<?> endID, AncillaryNode<?> endNode) {
        super(startID, startNode, endID, endNode);
        Objects.requireNonNull(trns);
        this.trns = trns;
    }

    private ComponentLink(boolean isInstantiated, CircuitComponent component, TransmitterType trns, ComponentUUID<?> startID, AncillaryNode<?> startNode, ComponentUUID<?> endID, AncillaryNode<?> endNode) {
        this(trns, startID, startNode, endID, endNode);
        this.isInstantiated = isInstantiated;
        this.component = component;
    }

    @Override
    public ComponentLink<T> flippedCopy() {
        return new ComponentLink<T>(isInstantiated, component, trns, endID, endNode, startID, startNode);
    }

    /**
     * Returns the {@link CircuitComponent} backed by this ComponentLink.
     * <code>null</code> values returned here indicate either that this ComponentLink's
     * hasn't been instantiated yet or that the result of a previous call to {@link #apply}
     * returned no component.
     * @return The CircuitComponent controlled and instantiated by this ComponentLink's {@link UnionFactory}
     * @see #apply
     */
    public CircuitComponent unsafeGet() {
        return component;
    }

    public @Nullable CircuitComponent get(ServerGrid grid) {
        if(isInstantiated) return component;
        this.component = TransmitterType.applyUnion(grid, trns.getFactory(), this, getStartAncillary(), getEndAncillary());
        isInstantiated = true;
        return this.component;
    }

    public void invalidate() {
        this.isInstantiated = false;
        this.component.reset();
        this.component = null;
    }

    public TransmitterType getTransmitter() {
        return trns;
    }

    @Override
    public void forEachNode(Consumer<Node> cons) {
        if(startNode != null) startNode.forEachNode(cons);
        if(endNode != null) endNode.forEachNode(cons);
    }

    @Override
    public boolean isGrounded() {
        return (startNode != null && startNode.isGrounded()) || (endNode != null && endNode.isGrounded());
    }

    @Override
    public void saturate() {
        if(!isInstantiated) return;
        if(component == null) {
            throw new NullPointerException("An operation on " + this 
                + " required a valid component, but the component was null!");
        }
        component.saturate();
    }

    @Override
    public void reset() {
        if(!isInstantiated) return;
        if(component == null) {
            throw new NullPointerException("An operation on " + this 
                + " required a valid component, but the component was null!");
        }
        component.reset();
    }

    @Override
    public String getComponentID() {
        return "link_" + trns.getName();
    }

    @Override
    public String toString() {
        return getComponentID();
    }

    @Override
    public ComponentHierarchy getHierarchyType() {
        return ComponentHierarchy.COMPONENT_LINK;
    }

    @Override
    public @Nullable CircuitComponent getComponent(ComponentBinding binding) {
        if(binding.getHierarchyType() == ComponentHierarchy.ANCILLARY_NODE)
            return binding.get() == 0 ? startNode : endNode;
        return component;
    }

    @Override
    public @Nullable GridConstruct getParentConstruct() {
        return null;
    }
}

