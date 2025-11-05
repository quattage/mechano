package com.quattage.mechano.api.grid.topology;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;

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
     * the parent component of a Terminal isn't the joint its connected to, 
     * rather the CircuitComponent that created it. 
     */
    @Override
    public CircuitComponent getParentComponent() {
        return owner;
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
        return owner == null ? "No owner" : owner.getComponentID() + "'s '" + getComponentID() + " terminal";
    }

    @Override public void saturate() {}
    @Override public void reset() {}
    @Override public String getComponentID() { return "Terminal"; }

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
}
