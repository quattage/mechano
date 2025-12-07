package com.quattage.mechano.api.grid.topology;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.grid.GridHierarchy;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.topology.ancillary.AncillaryNode;
import com.quattage.mechano.api.grid.topology.ancillary.WireJack;
import com.quattage.mechano.api.transmitter.TransmitterType;
import com.quattage.mechano.foundation.tracking.GridUUID;

import net.minecraft.resources.ResourceLocation;

/**
 * A link that connects two {@link AncillaryNode ancillaries}
 * together, like a wire.
 */
public class ComponentLink<T extends CircuitComponent> implements CircuitComponent {

    private final TransmitterType<T> trns;
    private @Nullable GridUUID startID, endID;
    private AncillaryNode startNode, endNode;
    private CircuitComponent component;

    public ComponentLink(TransmitterType<T> trns) {
        Objects.requireNonNull(trns);
        this.trns = trns;
    }

    public ComponentLink<T> assignStart(AncillaryNode startNode) {
        Objects.requireNonNull(startID);
        if(startID.getReferentType() != GridHierarchy.ANCILLARY_NODE) {
            throw new IllegalArgumentException("Failed while assigning ComponentLink starting point with " + startID 
                + " - The provided UUID doesn't conform to the proper referent type! (ComponentLinks may only refer to Ancillaries, got '" + startID.getReferentType() + "'!)");
        }
        Objects.requireNonNull(startNode);
        this.startNode = startNode;
        this.startID = startNode.bindUUID(startNode.getSource().getUUID());
        return this;
    }

    public ComponentLink<T> assignStart(GridUUID startID, AncillaryNode startNode) {
        Objects.requireNonNull(startID);
        if(startID.getReferentType() != GridHierarchy.ANCILLARY_NODE) {
            throw new IllegalArgumentException("Failed while assigning ComponentLink starting point with " + startID 
                + " - The provided UUID doesn't conform to the proper referent type! (ComponentLinks may only refer to Ancillaries, got '" + startID.getReferentType() + "'!)");
        }
        Objects.requireNonNull(startNode);
        this.startID = startID;
        this.startNode = startNode;
        return this;
    }

    public ComponentLink<T> assignEnd(AncillaryNode endNode) {
        Objects.requireNonNull(endID);
        if(endID.getReferentType() != GridHierarchy.ANCILLARY_NODE) {
            throw new IllegalArgumentException("Failed while assigning ComponentLink starting point with " + endID 
                + " - The provided UUID doesn't conform to the proper referent type! (ComponentLinks may only refer to Ancillaries, got '" + endID.getReferentType() + "'!)");
        }
        Objects.requireNonNull(endNode);
        if(!endNode.isSignificant()) {
            throw new IllegalArgumentException("Failed while assigning ComponentLink starting point with " + endID 
                + " - The provided AncillaryNode is insignificant!");
        }        
        this.endNode = endNode;
        this.endID = endNode.bindUUID(endNode.getSource().getUUID());
        return this;
    }

    public ComponentLink<T> assignEnd(GridUUID endID, AncillaryNode endNode) {
        Objects.requireNonNull(endID);
        if(endID.getReferentType() != GridHierarchy.ANCILLARY_NODE) {
            throw new IllegalArgumentException("Failed while assigning ComponentLink starting point with " + endID 
                + " - The provided UUID doesn't conform to the proper referent type! (ComponentLinks may only refer to Ancillaries, got '" + endID.getReferentType() + "'!)");
        }
        Objects.requireNonNull(endNode);
        if(!endNode.isSignificant()) {
            throw new IllegalArgumentException("Failed while assigning ComponentLink starting point with " + endID 
                + " - The provided AncillaryNode is insignificant!");
        }
        this.endID = endID;
        this.endNode = endNode;
        return this;
    }

    public ComponentLink<T> checkValidity() {
        assertHasIDs();
        assertHasAncillaries();
        assertHasSignificance();
        assertHasSources();
        assertIDsMatch();
        assertNonConflict();
        return this;
    }

    public CircuitComponent getOrCreateInternalComponent() {
        if(component != null) return component;
        if(!(startNode instanceof WireJack startJack))
            throw new IllegalStateException("Failed to create component for " + this + " - The starting node's type is unsupported!");
        if(!(endNode instanceof WireJack endJack))
            throw new IllegalStateException("Failed to create component for " + this + " - The starting node's type is unsupported!");
        component = trns.instantiate(startJack, endJack);
        component.updateOwnership(this, -1);
        return component;
    }

    public ComponentLink<T> flippedCopy() {
        return new ComponentLink<T>(trns)
            .assignStart(endID, endNode)
            .assignEnd(startID, startNode);
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
        getOrCreateInternalComponent().reset();
    }

    @Override
    public void reset() {
        getOrCreateInternalComponent().reset();
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
    public GridHierarchy getType() {
        return GridHierarchy.COMPONENT_LINK;
    }

    @Override
    public void updateOwnership(@Nullable Griddable<?>source, CircuitComponent parent, int index) {
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
    public String describeState() {
        return "'" + getComponentID() + "', " + startID + " -> " + endID + ", init? " + (component != null);
    }

    @Override
    public ResourceLocation asResource() {
        return Mechano.asResource(getComponentID());
    }

    public GridUUID getStart() {
        return startID;
    }

    public GridUUID getEnd() {
        return endID;
    }

    public AncillaryNode getStartAncillary() {
        return startNode;
    }

    public AncillaryNode getEndAncillary() {
        return endNode;
    }

    public boolean hasUUIDs() {
        return startID != null && endID != null;
    }

    public boolean hasJacks() {
        return startNode != null && endNode != null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.startNode, this.endNode);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ComponentLink<?> that)) return false;
        return this.startNode == that.startNode && this.endNode == that.endNode;
    }


    private void assertHasTransmitter() {
        if(trns == null) {
            throw new NullPointerException("An operation failed on ComponentLink " + this 
                + " - This ComponentLink has no transmitter!");
        }
        if(trns.getFactory() == null) {
            throw new NullPointerException("An operation failed on ComponentLink " + this 
                + " - The supplied transmitter " + trns + " returned a null CircuitComponent factory!");
        }
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
                + " - The starting jack returned a UUID that doesn't match!");
        }
        GridUUID endIDRetrieved = endNode.bindUUID(endNode.getSource().getUUID());
        if(!endIDRetrieved.equals(endID)) {
            throw new IllegalStateException("An operation failed on ComponentLink " + this 
                + " - The ending jack returned a UUID that doesn't match!");
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

