package com.quattage.mechano.api.grid.topology;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.GridReferent;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.topology.vertex.AncillaryNode;
import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.api.grid.topology.vertex.Terminal;
import com.quattage.mechano.api.transmitter.TransmitterType;
import com.quattage.mechano.foundation.tracking.GridUUID;

/**
 * A link that connects two {@link AncillaryNode ancillaries}
 * together, like a wire.
 */
public class ComponentLink<T extends CircuitComponent> extends AncillaryPair implements CircuitComponent {

    private final TransmitterType trns;
    private boolean isInstantiated = false;
    private CircuitComponent component;

    public ComponentLink(TransmitterType trns, AncillaryNode startNode, AncillaryNode endNode) {
        super(startNode, endNode);
        Objects.requireNonNull(trns);
        this.trns = trns;
    }

    public ComponentLink(TransmitterType trns, GridUUID startID, AncillaryNode startNode, GridUUID endID, AncillaryNode endNode) {
        super(startID, startNode, endID, endNode);
        Objects.requireNonNull(trns);
        this.trns = trns;
    }

    private ComponentLink(boolean isInstantiated, CircuitComponent component, TransmitterType trns, GridUUID startID, AncillaryNode startNode, GridUUID endID, AncillaryNode endNode) {
        this(trns, startID, startNode, endID, endNode);
        this.isInstantiated = isInstantiated;
        this.component = component;
    }

    @Override
    public ComponentLink<T> flippedCopy() {
        return new ComponentLink<T>(isInstantiated, component, trns, endID, endNode, startID, startNode);
    }

    public @Nullable CircuitComponent get() {
        return component;
    }

    public @Nullable CircuitComponent apply(ServerGrid grid) {
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
    public Collection<Terminal> getTerminals() {
        Collection<Terminal> tA = (startNode == null || !startNode.isSignificant()) ? Collections.emptyList() : startNode.getParentComponent().getTerminals();
        Collection<Terminal> tB = (endNode == null || !endNode.isSignificant()) ? Collections.emptyList() : endNode.getParentComponent().getTerminals();
        // i avoid using addAll() here because we cannot guarantee that the collections above are returned as
        // shallow-copies by API users (in fact, its inadvisable to do so) - instead, the collections 
        // are concatenated using primitive arrays
        int tal = tA.size();
        int tbl = tB.size();
        Terminal[] tm = new Terminal[tal + tbl];
        System.arraycopy(tA.toArray(), 0, tm, 0, tal);
        System.arraycopy(tB.toArray(), 0, tm, tal, tbl);
        return Arrays.asList(tm);
    }

    @Override
    public void forEachNode(Consumer<Node> cons) {
        if(startNode != null) startNode.forEachNode(cons);
        if(endNode != null) endNode.forEachNode(cons);
    }

    @Override
    public boolean isSignificant() {
        return (startNode != null && startNode.isSignificant()) || (endNode != null && endNode.isSignificant());
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
    public int size() {
        return 2;
    }

    @Override
    public GridUUID bindUUID(GridUUID id) {
        throw new UnsupportedOperationException("Unimplemented method 'bindUUID'");
    }

    @Override
    public GridReferent getReferentType() {
        return GridReferent.COMPONENT_LINK;
    }

    @Override
    public void updateOwnership(@Nullable Griddable<?> source, CircuitComponent parent, int index) {
        return;
    }

    @Override
    public @Nullable CircuitComponent getParentComponent() {
        return startNode;
    }

    @Override
    public String getComponentID() {
        return "link_" + trns.getName();
    }

    @Override
    public String toString() {
        return getComponentID();
    }
}

