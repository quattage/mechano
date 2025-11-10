package com.quattage.mechano.api.grid.topology;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.grid.Griddable;

import net.minecraft.resources.ResourceLocation;

public class Terminal implements CircuitComponent {
    
    private CircuitComponent owner;
    private @Nullable Node connected;
    private String id;

    public static Terminal[] pair(CircuitComponent instantiator) {
        return new Terminal[] { new Terminal(instantiator, "pinA"), new Terminal(instantiator, "pinB") };
    }

    public static Terminal[] polarPair(CircuitComponent instantiator) {
        return new Terminal[] { new Terminal(instantiator, "positive"), new Terminal(instantiator, "negative") };
    }

    public static Terminal[] functionalPair(CircuitComponent instantiator) {
        return new Terminal[] { new Terminal(instantiator, "anode"), new Terminal(instantiator, "cathode") };
    }

    public Terminal(CircuitComponent owner, String id) {
        Objects.requireNonNull(owner);
        CircuitComponent.checkID(id);
        this.owner = owner;
    }

    public final void setConnectedTo(@Nullable Node trace) {
        this.connected = trace;
        this.owner = trace == null ? null : trace.getParentComponent();
    }

    public @Nullable Node getJoint() {
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
        return owner;
    }

    /**
     * @return The Circuit that this Terminal's 
     * {@link #getParentComponent() parent component} belongs to
     * @see #getParentComponent()
     */
    public @Nullable Circuit getControllingCircuit() {
        CircuitComponent parent = getParentComponent();
        if(parent == null) return null;
        CircuitComponent superparent = parent.getParentComponent();
        return superparent instanceof Circuit c ? c : null;
    }

    @Override
    public void updateOwnership(@Nullable Griddable source, CircuitComponent parent, int index) {
        CircuitComponent component = getParentComponent();
        if(component == null) return;
        component.updateOwnership(source, parent, index);
    }

    @Override
    public Collection<Terminal> getTerminals() {
        Mechano.LOGGER.warn(this + " attempted to query itself.");
        return owner.getTerminals();
    }

    @Override
    public void forEachJoint(Consumer<Node> cons) {
        if(connected != null) cons.accept(connected);
    }

    @Override
    public String describeState() {
        return "from '" + owner.getComponentID() + "'";
    }

    public String describeSelf() {
        return owner == null ? "No owner" : owner.getComponentID() + "'s " + getComponentID();
    }

    @Override public void saturate() {}
    @Override public void reset() {}
    @Override public String getComponentID() { return id; }

    @Override
    public ResourceLocation asResource() {
        return Mechano.asResource(getSerializedName());
    }

    @Override
    public boolean isSignificant() {
        return connected != null && owner != null;
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
    public CircuitComponent.Type getType() {
        return CircuitComponent.Type.TERMINAL;
    }
}
