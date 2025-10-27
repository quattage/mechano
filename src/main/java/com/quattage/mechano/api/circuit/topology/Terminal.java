package com.quattage.mechano.api.circuit.topology;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.griddable.Griddable;

public class Terminal extends CircuitComponent {

    private CircuitComponent owner;
    private @Nullable Joint connected;

    public static Terminal[] pair(CircuitComponent instantiator) {
        return new Terminal[] { new Terminal(instantiator, "pinA"), new Terminal(instantiator, "pinB") };
    }

    public static Terminal[] polarPair(CircuitComponent instantiator) {
        return new Terminal[] { new Terminal(instantiator, "positive"), new Terminal(instantiator, "negative") };
    }

    public static Terminal[] functionalPair(CircuitComponent instantiator) {
        return new Terminal[] { new Terminal(instantiator, "anode"), new Terminal(instantiator, "cathode") };
    }

    public Terminal(CircuitComponent owner, String name) {
        super(owner == null ? "NO_OWNER" : owner.getSerializedName() + ".pin." + name);
        Objects.requireNonNull(owner);
        if(name == null || name.isBlank()) name = "NO_NAME";
        this.owner = owner;
    }

    protected final void setConnectedTo(@Nullable Joint trace) {
        this.connected = trace;
        this.owner = trace == null ? null : trace.getParentComponent();
    }

    public @Nullable Joint getJoint() {
        return connected;
    }

    public boolean hasJoint() {
        return connected != null;
    }

    public CircuitComponent getParentComponent() {
        return owner;
    }

    @Override
    public Collection<Terminal> getTerminals() {
        Mechano.LOGGER.warn(this + " attempted to query itself.");
        return Collections.singleton(this);
    }

    @Override
    public String describeState() {
        return "Terminal[" + owner == null ? ("NO_OWNER's " + componentID) : (owner + "'s " + componentID) + "]";
    }

    @Override
    public void tick(Griddable<?> host) {

    }
}
