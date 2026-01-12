package com.quattage.mechano.api.grid.component;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.component.ComponentTracker.ComponentHierarchy;
import com.quattage.mechano.api.grid.component.ComponentUUID.ComponentBinding;

/**
 * A {@link CircuitComponent} with a singular function that can be
 * parented to another CircuitComponent.
 */
public abstract class DiscreteComponent implements CircuitComponent, GridConstruct {

    private final String componentID;
    private GridConstruct parent;

    public DiscreteComponent(String componentID) {
        CircuitComponent.assertValidID(componentID);
        this.componentID = componentID;
    }

    @Override public final String getComponentID() { return componentID; }
    @Override public boolean isGrounded() { return false; }
    @Override public void saturate() {}
    @Override public void reset() {}

    @Override
    public String toString() {
        return componentID;
    }

    @Override
    public ComponentHierarchy getHierarchyType() {
        return ComponentHierarchy.DISCRETE_COMPONENT;
    }

    @Override
    public void updateOwnership(@Nullable Griddable<?> source, GridConstruct parent) {
        this.parent = parent;
    }

    @Override
    public @Nullable CircuitComponent getComponent(ComponentBinding id) {
        return this;
    }

    @Override
    public @Nullable GridConstruct getParentConstruct() {
        return parent;
    }
}
