package com.quattage.mechano.api.grid.topology.vertex;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.component.ComponentTracker.ComponentHierarchy;
import com.quattage.mechano.api.grid.component.ComponentUUID;
import com.quattage.mechano.api.grid.component.DiscreteComponent;
import com.quattage.mechano.api.grid.component.GridConstruct;

public class Terminal implements CircuitComponent, GridConstruct {
    
    private final DiscreteComponent instantiator;
    private @Nullable Node connected;
    private String id;

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

    public final void setConnectedTo(@Nullable Node trace) {
        this.connected = trace;
    }

    public @Nullable Node getNode() {
        return connected;
    }

    public boolean isAttached() {
        return connected != null;
    }

    @Override
    public Collection<Terminal> getTerminals() {
        return instantiator.getTerminals();
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
        return id; 
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
    public void updateOwnership(@Nullable Griddable<?> source, GridConstruct parent, int index) {
        return;
    }

    @Override
    public <T extends ComponentUUID<T>> T bindUUID(T id) {
        throw new UnsupportedOperationException("Unimplemented method 'bindUUID'");
    }

    @Override
    public @Nullable CircuitComponent findSubComponent(ComponentUUID<?> id) {
        return connected;
    }

    @Override
    public @Nullable GridConstruct getParentConstruct() {
        return instantiator;
    }

    @Override
    public ComponentHierarchy getHierarchyType() {
        return ComponentHierarchy.TERMINAL;
    }
}
