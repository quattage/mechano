package com.quattage.mechano.api.circuit.topology;

import java.util.Collection;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.griddable.Griddable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class Joint extends CircuitComponent {

    /**
     * Merges all of the circuit data from <code>b</code>
     * into <code>a</code> and disposes of <code>b</code>.
     * Any subsequent access to the disposed joint will
     * throw errors.
     * @param a Joint destination
     * @param b Joint source
     * @return <code>a</code>, with the additional data from <code>b</code>
     */
    public static Joint combine(Joint a, Joint b) {
        a.attachedPins.addAll(b.attachedPins);
        a.voltagePotential = Math.max(a.voltagePotential, b.voltagePotential);
        for(Terminal terminal : b.getTerminals()) {
            terminal.setConnectedTo(a);
            b.dispose();
        }
        return a;
    }

    private CircuitComponent parent;
    private ObjectArrayList<Terminal> attachedPins;
    private double voltagePotential = 0;

    public Joint(CircuitComponent ownerCircuit) {
        super("Joint");
        this.parent = ownerCircuit;
        attachedPins = new ObjectArrayList<>(3);
    }

    public void attach(Terminal pin) {
        Objects.requireNonNull(pin);
        // TODO compare the joint already belonging to the terminal instead of searching the array
        if(!attachedPins.contains(pin))
            attachedPins.add(pin);
        pin.setConnectedTo(this);
    }

    public boolean detach(Terminal pin) {
        Objects.requireNonNull(pin);
        if(attachedPins.remove(pin)) {  
            pin.setConnectedTo(null);
            return true;
        }
        return false;
    }

    public boolean involves(Terminal pin) {
        for(Terminal otherPin : attachedPins) {
            if(otherPin == pin) return true;
        }
        return false;
    }

    public Collection<Terminal> getAllConnections() {
        return attachedPins;
    }

    public boolean hasConnections() {
        return attachedPins != null && !attachedPins.isEmpty();
    }

    public int getConnectionCount() {
        return attachedPins == null ? 0 : attachedPins.size();
    }

    public boolean isStaticGround() { return false; }
    @Override public double getVoltage() { return voltagePotential; }
    @Override public void setVoltage(double voltage) { this.voltagePotential = voltage; }
    @Override public @Nullable CircuitComponent getParentComponent() { return parent; }

    @Override
    public Collection<Terminal> getTerminals() {
        return attachedPins;
    }

    @Override
    public void tick(Griddable<?> host) {
        
    }

    protected void dispose() {
        this.parent = null;
        this.attachedPins = null;
        this.voltagePotential = 0;
    }

    @Override
    public String describeState() {
        return attachedPins.size() + " pin(s), grounded: " + isStaticGround();
    }

    public static class GroundedJoint extends Joint {
        public GroundedJoint(CircuitComponent ownerCircuit) {
            super(ownerCircuit);
        }
        @Override public boolean isStaticGround() { return true; }
        @Override public double getVoltage() { return 0; }
    }
}


