package com.quattage.mechano.api.grid.topology;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.grid.GridHierarchy;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.topology.ancillary.AncillaryNode;
import com.quattage.mechano.api.grid.topology.ancillary.WireJack;
import com.quattage.mechano.api.transmitter.TransmitterEntry;
import com.quattage.mechano.foundation.tracking.GridUUID;

import net.minecraft.resources.ResourceLocation;

/**
 * A link that connects two {@link AncillaryNode ancillaries}
 * together, like a wire.
 */
public class ComponentLink<T extends CircuitComponent> implements CircuitComponent {

    private final TransmitterEntry<T> type;
    private CircuitComponent component;
    private @Nullable GridUUID startID, endID;
    private @Nullable AncillaryNode startNode, endNode;

    public ComponentLink(TransmitterEntry<T> type) {
        Objects.requireNonNull(type);
        this.type = type;
    }
    
    public ComponentLink<T> assignStart(GridUUID startID) {
        Objects.requireNonNull(startID);
        if(startID.getReferentType() != GridHierarchy.ANCILLARY_NODE) {
            throw new IllegalArgumentException("Failed while assigning ComponentLink starting point with " + startID 
                + " - The provided UUID doesn't conform to the proper referent type! (ComponentLinks may only refer to Ancillaries, got '" + startID.getReferentType() + "'!)");
        }
        this.startID = startID;
        return this;
    }

    public ComponentLink<T> assignEnd(GridUUID endID) {
        Objects.requireNonNull(endID);
        if(endID.getReferentType() != GridHierarchy.ANCILLARY_NODE) {
            throw new IllegalArgumentException("Failed while assigning ComponentLink starting point with " + endID 
                + " - The provided UUID doesn't conform to the proper referent type! (ComponentLinks may only refer to Ancillaries, got '" + endID.getReferentType() + "'!)");
        }
        this.endID = endID;
        return this;
    }

    public ComponentLink<T> findJacks(Grid grid) {
        assertAssigned();
        CircuitComponent startC = grid.findComponent(startID);
        CircuitComponent endC = grid.findComponent(endID);
        if(!(startC instanceof AncillaryNode startA)) {
            throw new IllegalArgumentException("Failed to find WireJack for " + this 
                + " - The starting address located a non-ancillary type " + startC.getClass().getSimpleName());
        }
        if(!(endC instanceof AncillaryNode endA)) {
            throw new IllegalArgumentException("Failed to find WireJack for " + this 
                + " - The ending address located a non-ancillary type " + endC.getClass().getSimpleName());
        }
        this.startNode = startA; 
        this.endNode = endA;
        return this;
    }

    public boolean findJacksSafe(Grid grid) {
        return findJacksSafe(grid, true);
    }

    public boolean findJacksSafe(Grid grid, boolean log) {
        try{ findJacks(grid); } catch(RuntimeException e) {
            if(log) {
                grid.warn("Couldn't find jacks for " + this + " - See error for more details:");
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }

    public CircuitComponent getOrCreateInternalComponent() {
        if(component != null) return component;
        assertAssigned();
        assertHasJacks();
        if(!(startNode instanceof WireJack startJack))
            throw new IllegalStateException("Failed to create component for " + this + " - The starting node's type is unsupported!");
        if(!(endNode instanceof WireJack endJack))
            throw new IllegalStateException("Failed to create component for " + this + " - The starting node's type is unsupported!");
        component = type.get().create(startJack, endJack);
        component.updateOwnership(this, -1);
        return component;
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
        assertHasJacks();
        return (startNode != null && startNode.isSignificant()) || (endNode != null && endNode.isSignificant());
    }

    @Override
    public boolean isGrounded() {
        assertHasJacks();
        return (startNode != null && startNode.isGrounded()) || (endNode != null && endNode.isGrounded());
    }

    @Override
    public void saturate() {
        assertHasJacks();
        getOrCreateInternalComponent().reset();
    }

    @Override
    public void reset() {
        assertHasJacks();
        getOrCreateInternalComponent().reset();
    }

    @Override
    public int size() {
        return 2;
    }

    @Override
    public GridUUID bindUUID(GridUUID id) {
        // TODO Auto-generated method stub
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
        return "link_" + type.getRegisteredName();
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

    private void assertAssigned() {
        if(type == null) {
            throw new NullPointerException("An operation failed on ComponentLink " + this
                + " - The TransmitterType was not assigned! (This instance probably leaked or was never initialized properly!");
        }
        if(startID == null) {
            throw new NullPointerException("An operation failed on ComponentLink " + this 
                + " - The starting ID is not assigned! (This instance probably leaked or was never initialized properly!");
        }
        if(endID == null) {
            throw new NullPointerException("An operation failed on ComponentLink " + this 
                + " - The ending ID is not assigned! (This instance probably leaked or was never initialized properly!");
        }
    }

    public boolean hasUUIDs() {
        return startID != null && endID != null;
    }

    public boolean hasJacks() {
        return startNode != null && endNode != null;
    }

    @Override
    public boolean equals(Object obj) {
        assertAssigned();
        assertHasJacks();
        if(!(obj instanceof ComponentLink<?> that)) return false;
        return this.startNode == that.startNode && this.endNode == that.endNode;
    }

    private void assertHasJacks() {
        if(startNode == null) {
            throw new NullPointerException("An operation failed on ComponentLink " + this 
                + " - The starting jack hasn't been initialized! (At least one call to findJacks() must be made!");
        }
        if(endNode == null) {
            throw new NullPointerException("An operation failed on ComponentLink " + this 
                + " - The ending jack hasn't been initialized! (At least one call to findJacks() must be made!");
        }
    }
}

