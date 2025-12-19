package com.quattage.mechano.api.grid.topology.vertex;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.grid.GridHierarchy;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.topology.Circuit;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.foundation.tracking.GridUUID;

import net.minecraft.resources.ResourceLocation;

public class Terminal implements CircuitComponent {
    
    private final FunctionalComponent instantiator;
    private @Nullable Node connected;
    private String id;

    public static Terminal[] pair(FunctionalComponent instantiator) {
        return new Terminal[] { new Terminal(instantiator, "pinA"), new Terminal(instantiator, "pinB") };
    }

    public static Terminal[] polarPair(FunctionalComponent instantiator) {
        return new Terminal[] { new Terminal(instantiator, "positive"), new Terminal(instantiator, "negative") };
    }

    public static Terminal[] functionalPair(FunctionalComponent instantiator) {
        return new Terminal[] { new Terminal(instantiator, "anode"), new Terminal(instantiator, "cathode") };
    }

    public Terminal(FunctionalComponent instantiator, String id) {
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

    public boolean hasJoint() {
        return connected != null;
    }

    /**
     * @return The CircuitComponent that created this Terminal.
     * For example, if this Terminal represents a capacitor's positive pin,
     * this method will return the capacitor itself.
     * @see #getControllingCircuit()
     */
    @Override
    public CircuitComponent getParentComponent() {
        return instantiator;
    }

    /**
     * @return The Circuit that this Terminal's 
     * {@link #getParentComponent() parent component} belongs to
     * @see #getParentComponent()
     */
    public @Nullable Circuit getControllingCircuit() {
        CircuitComponent superparent = traverseUpwards();
        return superparent instanceof Circuit c ? c : null;
    }

    @Override
    public void updateOwnership(@Nullable Griddable<?> source, CircuitComponent parent, int index) {
        CircuitComponent.assertValidOwnership(this, parent);
        CircuitComponent component = getParentComponent();
        if(component == null) return;
        component.updateOwnership(source, parent, index);
    }

    @Override
    public Collection<Terminal> getTerminals() {
        Mechano.LOGGER.warn(this + " attempted to query itself.");
        return instantiator.getTerminals();
    }

    @Override
    public void forEachNode(Consumer<Node> cons) {
        if(connected != null) cons.accept(connected);
    }

    @Override
    public String describeState() {
        return "from '" + instantiator.getComponentID() + "'";
    }

    public String describeSelf() {
        return instantiator == null ? "No owner" : instantiator.getComponentID() + "'s " + getComponentID();
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
    public ResourceLocation asResource() {
        return Mechano.asResource(getSerializedName());
    }

    @Override
    public boolean isSignificant() {
        return connected != null && instantiator != null;
    }

    @Override
    public boolean isGrounded() {
        return connected == null ? false : connected.isGrounded();
    }

    @Override
    public int size() { return isSignificant() ? 1 : 0; }


    @Override
    public String toString() {
        return describeSelf();
    }

    @Override
    public GridHierarchy getType() {
        return GridHierarchy.TERMINAL;
    }

    @Override
    public GridUUID bindUUID(GridUUID id) {
        Mechano.LOGGER.warn("Attempted to bind " + id + " to a Terminal object (" 
            + this + "), which is unsupported. The unmodified ID was returned directly.");
        return id;
    }
}
