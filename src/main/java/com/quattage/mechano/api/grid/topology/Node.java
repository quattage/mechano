package com.quattage.mechano.api.grid.topology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.grid.topology.ancillary.AncillaryJack;
import com.quattage.mechano.api.griddable.Griddable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;

public interface Node extends CircuitComponent {

    double getVoltage();
    void setVoltage(double volts);
    Collection<Terminal> getAllConnections();
    Collection<AncillaryJack> getAllAncillaries();
    
    boolean attach(Terminal pin);
    boolean detach(Terminal pin);
    boolean attach(@Nullable Griddable<?> source, AncillaryJack jack);
    boolean detach(@Nullable Griddable<?> source, AncillaryJack jack);

    boolean involves(Terminal pin);
    void dispose();
    int getIndex();
    
    default void updateOwnership(CircuitComponent parent, int index) {
        updateOwnership(null, parent, index);
    }
    void updateOwnership(@Nullable Griddable<?> source, CircuitComponent parent, int index);

    default boolean hasConnections() { return size() > 0; }
    boolean hasAncillaries();
    @Override default boolean isSignificant() { return hasConnections() || hasAncillaries(); }





    public static class Joint implements Node {

        private CircuitComponent parent;
        protected ObjectArrayList<Terminal> attachedPins;
        protected @Nullable List<AncillaryJack> ancillaries = null;
        private double voltagePotential = 0;
        private int index;

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
            if(a == b) return a;
            a.attachedPins.ensureCapacity(a.attachedPins.size() + b.attachedPins.size());
            for(Terminal t : b.getTerminals()) {
                t.setConnectedTo(a);
                if(t.getJoint() == a) continue;
                a.attachedPins.add(t);
            }
            b.dispose();
            return a;
        }

        public static Joint combine(Node a, Node b) {
            if(a instanceof Joint aj && b instanceof Joint bj)
                return Joint.combine(aj, bj);
            throw new IllegalArgumentException("shut up");
        }

        public Joint(CircuitComponent ownerCircuit, int index) {
            this.parent = ownerCircuit;
            attachedPins = new ObjectArrayList<>(3);
            this.index = index;
        }

        public Joint(CircuitComponent ownerCircuit) {
            this.parent = ownerCircuit;
            attachedPins = new ObjectArrayList<>(3);
            this.index = -1;
        }

        @Override
        public boolean attach(Terminal pin) {
            Objects.requireNonNull(pin);
            if(this == pin.getJoint()) 
                return false;
            pin.setConnectedTo(this);
            attachedPins.add(pin);
            return true;
        }

        @Override
        public boolean detach(Terminal pin) {
            Objects.requireNonNull(pin);
            if(attachedPins.remove(pin)) {  
                pin.setConnectedTo(null);
                return true;
            }
            return false;
        }

        @Override
        public boolean hasAncillaries() {
            return ancillaries != null && ancillaries.size() > 0;
        }

		@Override
		public boolean attach(Griddable<?> source, AncillaryJack jack) {
			if(ancillaries == null)
                ancillaries = new ArrayList<>();
            ancillaries.add(jack);
            jack.attachTo(source, this);
            return true;
		}

		@Override
		public boolean detach(Griddable<?> source, AncillaryJack jack) {
			boolean removed = ancillaries.remove(jack);
            if(ancillaries.isEmpty()) ancillaries = null;
            if(removed) jack.attachTo(null, null);
            return removed;
		}


        @Override public String getComponentID() { return "Joint"; }
        @Override public ResourceLocation asResource() { return Mechano.asResource(getSerializedName()); }
        @Override public void saturate() {}
        @Override public void reset() {}
        @Override public double getVoltage() { return voltagePotential; }
        @Override public void setVoltage(double volts) { this.voltagePotential = volts; }
        @Override public Collection<Terminal> getAllConnections() { return attachedPins; }
        @Override public boolean isGrounded() { return false; }
        @Override public String describeState() { return attachedPins.size() + " pins, " + ancillaries.size() + " ancillaries, " + String.format("%.3f", voltagePotential) + " volts (ungrounded)"; }
        @Override public boolean involves(Terminal pin) { return pin != null && pin.getJoint() == this; }
        @Override public boolean hasConnections() { return attachedPins != null && !attachedPins.isEmpty(); }
        @Override public @Nullable CircuitComponent getParentComponent() { return parent; }
        @Override public Collection<Terminal> getTerminals() { return attachedPins; }
        @Override public void forEachJoint(Consumer<Node> cons) { cons.accept(this); }
        @Override public int size() { return attachedPins.size(); }

        @Override
        public Collection<AncillaryJack> getAllAncillaries() {
            if(ancillaries == null) return Collections.singleton(null);
            return ancillaries;
        }

        @Override
        public void dispose() {
            this.parent = null;
            this.index = -1;
            this.voltagePotential = 0;
            this.attachedPins = null;
        }

        @Override
        public int getIndex() {
            if(index < 0 && !isGrounded()) throw new IllegalStateException("Joint returned invalid index " + index);
            return index;
        }

        @Override
        public void updateOwnership(Griddable<?> source, CircuitComponent parent, int index) {
            this.parent = parent;
            this.index = index;
            if(source == null || !hasAncillaries()) return;
            for(AncillaryJack jack : ancillaries)
                jack.attachTo(source, this);
        }
        
        @Override
        public String toString() {
            return "Node[" + describeState() + "]";
        }
    }


    /**
     * A Joint whose voltage potential is always zero
     * and whose index is always -1. This joint
     * is added automatically to Circuit instances
     * during creation.
     */
    public static class GroundedJoint implements Node {

        private CircuitComponent parent;
        protected ObjectArrayList<Terminal> attachedPins;
        protected @Nullable List<AncillaryJack> ancillaries = null;

        public GroundedJoint(CircuitComponent parent) {
            this.parent = parent;
            this.attachedPins = new ObjectArrayList<>(2);
        }

        @Override
        public boolean attach(Terminal pin) {
            Objects.requireNonNull(pin);
            if(this == pin.getJoint()) 
                return false;
            pin.setConnectedTo(this);
            attachedPins.add(pin);
            return true;
        }

        @Override
        public boolean detach(Terminal pin) {
            Objects.requireNonNull(pin);
            if(attachedPins.remove(pin)) {  
                pin.setConnectedTo(null);
                return true;
            }
            return false;
        }

        @Override
        public boolean hasAncillaries() {
            return ancillaries != null && ancillaries.size() > 0;
        }

        @Override
		public boolean attach(Griddable<?> source, AncillaryJack jack) {
			if(ancillaries == null)
                ancillaries = new ArrayList<>();
            ancillaries.add(jack);
            jack.attachTo(source, this);
            return true;
		}

		@Override
		public boolean detach(Griddable<?> source, AncillaryJack jack) {
			boolean removed = ancillaries.remove(jack);
            if(ancillaries.isEmpty()) ancillaries = null;
            if(removed) jack.attachTo(null, null);
            return removed;
		}

        @Override public Collection<Terminal> getTerminals() { return attachedPins; }
        @Override public void forEachJoint(Consumer<Node> cons) { cons.accept(this); }
        @Override public String getComponentID() { return "GroundedJoint"; }
        @Override public String describeState() { return attachedPins.size() + ancillaries.size() + " ancillaries, " + " pin(s), 0.000 volts (grounded)"; }
        @Override public ResourceLocation asResource() { return Mechano.asResource(getSerializedName()); }
        @Override public Collection<Terminal> getAllConnections() { return attachedPins; }
        @Override public void setVoltage(double volts) { return; }
        @Override public double getVoltage() { return 0; }
        @Override public @Nullable CircuitComponent getParentComponent() { return parent; }
        @Override public boolean involves(Terminal pin) { return attachedPins.contains(pin); }
        @Override public boolean isGrounded() { return true; }
        @Override public void saturate() {}
        @Override public void reset() {}
        @Override public void dispose() {}
        @Override public int getIndex() { return -1; }
        @Override public int size() { return attachedPins.size(); }
        
        @Override
        public Collection<AncillaryJack> getAllAncillaries() {
            if(ancillaries == null) return Collections.singleton(null);
            return ancillaries;
        }
        
        @Override public void updateOwnership(Griddable<?> source, CircuitComponent parent, int index) { 
            this.parent = parent; 
            if(source == null || !hasAncillaries()) return;
            for(AncillaryJack jack : ancillaries)
                jack.attachTo(source, this);
        }
        

        @Override
        public String toString() {
            return "Node[" + describeState() + "]";
        }
    }
}


