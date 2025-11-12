package com.quattage.mechano.api.grid.topology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.topology.ancillary.AncillaryJack;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;

public interface Node extends CircuitComponent {

    double getVoltage();
    void setVoltage(double volts);
    default boolean hasConnections() { return size() > 0; }
    Collection<AncillaryJack> getAllAncillaries();
    boolean hasAncillaries();
    int getIndex();
    boolean attach(Terminal pin);
    boolean detach(Terminal pin);
    boolean attach(@Nullable Griddable source, AncillaryJack jack);
    boolean detach(@Nullable Griddable source, AncillaryJack jack);
    boolean involves(Terminal pin);
    @Override default boolean isSignificant() { return hasConnections() || hasAncillaries(); }
    void dispose();
    
    @Override
    default CircuitComponent.Type getType() {
        return CircuitComponent.Type.EMITTER_NODE;
    }

    public static class Joint implements Node {

        private CircuitComponent parent;
        protected ObjectArrayList<Terminal> attachedPins;
        protected @Nullable List<AncillaryJack> ancillaries = null;
        private double voltagePotential = 0;
        private int index;

        public Joint(CircuitComponent parent, int index) {
            this.parent = parent;
            attachedPins = new ObjectArrayList<>(3);
            this.index = index;
        }

        public Joint(CircuitComponent parent) {
            this.parent = parent;
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
		public boolean attach(Griddable source, AncillaryJack jack) {
			if(ancillaries == null)
                ancillaries = new ArrayList<>();
            ancillaries.add(jack);
            jack.attachTo(source, this);
            return true;
		}

		@Override
		public boolean detach(Griddable source, AncillaryJack jack) {
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
        @Override public boolean isGrounded() { return false; }
        @Override public String describeState() { return attachedPins.size() + " pins, " + ancillaries.size() + " ancillaries, " + String.format("%.3f", voltagePotential) + " volts (ungrounded)"; }
        @Override public boolean involves(Terminal pin) { return pin != null && pin.getJoint() == this; }
        @Override public boolean hasConnections() { return attachedPins != null && !attachedPins.isEmpty(); }
        @Override public @Nullable CircuitComponent getParentComponent() { return parent; }
        @Override public void forEachNode(Consumer<Node> cons) { cons.accept(this); }
        @Override public int size() { return attachedPins.size(); }

        @Override
        public Collection<AncillaryJack> getAllAncillaries() {
            if(ancillaries == null) return Collections.emptyList();
            return ancillaries;
        }

        @Override public Collection<Terminal> getTerminals() { 
            return attachedPins; 
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
        public void updateOwnership(Griddable source, CircuitComponent parent, int index) {
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
		public boolean attach(Griddable source, AncillaryJack jack) {
			if(ancillaries == null)
                ancillaries = new ArrayList<>();
            ancillaries.add(jack);
            jack.attachTo(source, this);
            return true;
		}

		@Override
		public boolean detach(Griddable source, AncillaryJack jack) {
			boolean removed = ancillaries.remove(jack);
            if(ancillaries.isEmpty()) ancillaries = null;
            if(removed) jack.attachTo(null, null);
            return removed;
		}

        @Override public void forEachNode(Consumer<Node> cons) { cons.accept(this); }
        @Override public String getComponentID() { return "GroundedJoint"; }
        @Override public String describeState() { return attachedPins.size() + ancillaries.size() + " ancillaries, " + " pin(s), 0.000 volts (grounded)"; }
        @Override public ResourceLocation asResource() { return Mechano.asResource(getSerializedName()); }
        @Override public void setVoltage(double volts) { return; }
        @Override public double getVoltage() { return 0; }
        @Override public @Nullable CircuitComponent getParentComponent() { return parent; }
        @Override public boolean involves(Terminal pin) { return attachedPins.contains(pin); }
        @Override public boolean isGrounded() { return true; }
        @Override public void saturate() {}
        @Override public void reset() {}
        @Override public void dispose() {}
        @Override public int getIndex() { return 0; }
        @Override public int size() { return attachedPins.size(); }
        
        @Override
        public Collection<AncillaryJack> getAllAncillaries() {
            if(ancillaries == null) return Collections.emptyList();
            return ancillaries;
        }

        @Override public Collection<Terminal> getTerminals() { 
            return attachedPins; 
        }

        @Override public void updateOwnership(Griddable source, CircuitComponent parent, int index) { 
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


