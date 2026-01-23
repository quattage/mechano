package com.quattage.mechano.api.grid.topology.vertex;

import java.util.Objects;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.quattage.mechano.api.grid.GridComponentTracker.ComponentHierarchy;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.component.ComponentUUID.UUIDComposite;
import com.quattage.mechano.api.grid.component.DiscreteComponent;
import com.quattage.mechano.api.grid.component.GridConstruct;
import com.quattage.mechano.api.grid.component.GridConstruct.TerminalProvider;
import com.quattage.mechano.foundation.Disposable;

public class Terminal implements CircuitComponent, GridConstruct, TerminalProvider, Disposable {
    
    private DiscreteComponent instantiator;
    private Node connected;
    private String componentID;

    public static Terminal[] pair(DiscreteComponent instantiator) {
        return new Terminal[] { new Terminal(instantiator, "pinA"), new Terminal(instantiator, "pinB") };
    }

    public static Terminal[] polarPair(DiscreteComponent instantiator) {
        return new Terminal[] { new Terminal(instantiator, "positive"), new Terminal(instantiator, "negative") };
    }

    public static Terminal[] functionalPair(DiscreteComponent instantiator) {
        return new Terminal[] { new Terminal(instantiator, "anode"), new Terminal(instantiator, "cathode") };
    }

    public Terminal(DiscreteComponent instantiator, String id) {
        Objects.requireNonNull(instantiator);
        CircuitComponent.assertValidID(id);
        this.instantiator = instantiator;
    }

    public @Nullable Node getAttachedNode() {
        return connected;
    }

    public boolean isAttached() {
        return connected != null;
    }

    @Override
    public void forEachNode(Consumer<Node> cons) {
        if(connected != null) cons.accept(connected);
    }

    @Override 
    public void saturate() {
        instantiator.saturate();
    }

    @Override 
    public void reset() {
        instantiator.reset();
    }

    @Override 
    public String getComponentID() { 
        return componentID; 
    }

    @Override
    public boolean isGrounded() {
        return connected == null ? false : connected.isGrounded();
    }

    @Override
    public String toString() {
        return instantiator == null ? "No owner" : instantiator.getComponentID() + "'s '" + getComponentID() + "'";
    }

    @Override
    public void updateOwnership(@Nullable Griddable<?> source, GridConstruct parent) {
        GridConstruct.assertValidOwnership(this, parent);
        if(parent instanceof Node node) this.connected = node;
    }

    @Override
    public @Nullable CircuitComponent getComponent(UUIDComposite binding) {
        return binding.getHierarchyType() == ComponentHierarchy.DISCRETE 
            || binding.getHierarchyType() == ComponentHierarchy.STRANGER ? instantiator : connected;
    }

    @Override
    public @Nullable GridConstruct getParentConstruct() {
        return instantiator;
    }

    @Override
    public ComponentHierarchy getHierarchyType() {
        return ComponentHierarchy.TERMINAL;
    }

    @Override
    public Terminal[] getTerminals() {
        return new Terminal[] { this };
    }

    @Override
    public void dispose() {
        if(connected == null) return;
        componentID += " (disposed)";
        connected.localDetach(this);
        connected = null;
        instantiator = null;
    }

    @Override
    public boolean hasBeenDisposed() {
        return componentID.endsWith("(disposed)");
    }
}
