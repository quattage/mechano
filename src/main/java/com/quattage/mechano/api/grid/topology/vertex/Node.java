package com.quattage.mechano.api.grid.topology.vertex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.grid.GridHierarchy;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.solver.NodeUnionSet;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.foundation.tracking.GridUUID;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public interface Node extends CircuitComponent {

    /**
     * Transfers the contents of <code>nodeB</code> onto
     * <code>nodeA</code> and disposes of <code>nodeB</code>
     * <p>
     * Subsequent access to a node that has been disposed of
     * will throw errors.
     * @param nodeA
     * @param nodeB
     * @return
     */
    static Node collapse(Node nodeA, Node nodeB) {
        if(nodeA == nodeB) return nodeA;
        for(Terminal term : nodeB.getTerminals()) {
            term.setConnectedTo(nodeA);
            if(term.getNode() == nodeB) continue;
            nodeA.getTerminals().add(term);
        }
        nodeB.dispose();
        return nodeA;
    }

    boolean attach(Terminal pin);
    boolean attach(@Nullable Griddable<?> source, AncillaryNode jack);
    boolean detach(Terminal pin);
    boolean detach(@Nullable Griddable<?> source, AncillaryNode jack);

    int getCircuitIndex();

    /**
     * @return The index that this node belongs to in its associated
     * {@link NodeUnionSet}. A return value <code><0</code> indicates
     * that this node is grounded.
     */
    int getNodalIndex();

    /**
     * To be called only by API elements, particularly the {@link NodeUnionSet}
     * when unioning this node for solving.
     * @param nodalIndex The index in the {@link NodeUnionSet} that this node belongs
     */
    @ApiStatus.Internal
    void setNodalIndex(int nodalIndex);

    List<AncillaryNode> getAncillaries();
    @Override List<Terminal> getTerminals();
    
    double getVoltage();
    void setVoltage(double volts);
    void dispose();

    @Override
    default String getComponentID() {
        return "Joint";
    }

    @Override 
    default boolean isSignificant() { 
        return hasTerminals() || hasAncillaries(); 
    }

    @Override default int size() { 
        Collection<Terminal> terminals = getTerminals();
        return terminals == null ? 0 : terminals.size();
    }

    default boolean hasTerminals() { 
        return size() > 0; 
    }

    default boolean hasAncillaries() {
        return getAncillaries() != null && getAncillaries().size() > 0;
    }

    default boolean has(Terminal terminal) {
        return indexOf(terminal) >= 0;
    }

    default boolean has(AncillaryNode ancillary) {
        return indexOf(ancillary) >= 0;
    }

    default int indexOf(AncillaryNode ancillary) {
        List<AncillaryNode> ancillaries = getAncillaries();
        if(ancillaries == null || ancillaries.isEmpty())
            return -1;
        return ancillaries.indexOf(ancillary);
    }

    default int indexOf(Terminal terminal) {
        List<Terminal> terminals = getTerminals();
        if(terminals == null || terminals.isEmpty())
            return -1;
        return terminals.indexOf(terminal);
    }

    @Override
    default void saturate() {
        List<Terminal> terminals = getTerminals();
        if(terminals == null || terminals.isEmpty())
            return;
        for(Terminal term : terminals)
            term.saturate();
    }

    @Override
    default void reset() {
        List<Terminal> terminals = getTerminals();
        if(terminals == null || terminals.isEmpty())
            return;
        for(Terminal term : terminals)
            term.reset();
    }

    @Override
    default boolean isGrounded() {
        return false;
    }

    @Override
    default void forEachNode(Consumer<Node> cons) {
        cons.accept(this);
    }

    @Override
    default ResourceLocation asResource() {
        return Mechano.asResource(getSerializedName());
    }

    @Override
    default String describeState() {
        return size() + " terminals, " + (getAncillaries() == null ? "0" : getAncillaries().size()) + " ancillaries, " + String.format("%.3f", getVoltage()) + " volts, " + (isGrounded() ? "grounded" : "ungrounded");
    }

    @Override
    default GridUUID bindUUID(GridUUID id) {
        return id.withBinding(getType(), getCircuitIndex());
    }
    
    @Override
    default GridHierarchy getType() {
        return GridHierarchy.EMITTER_NODE;
    }

    default String toFullString(@Nullable NodeUnionSet unionizer) {
        String out = "    ▸ " + getSerializedName() + " " + getNodalIndex() + ", " + (isGrounded() ? "Ground:" : String.format("%.3f", getVoltage()) + " volts:");
        if(hasAncillaries()) {
            Griddable<?> src = getSource();
            BlockPos bp = src.getBlockPos();
            out += "\n\t  Owned by " + src.getClass().getSimpleName() + " at [" + bp.getX() + ", " + bp.getY() + ", " + bp.getZ() + "]";
            out += "\n\t  " + getAncillaries().size() + " Ancillaries: [";
            for(AncillaryNode anc : getAncillaries())
                out += anc.getComponentID() + ", ";
            out = out.substring(0, out.length() - 2) + "]";
        } else out += "\n\t  0 Ancillaries: [Empty]";
        if(hasTerminals()) {
            out += "\n\t  " + getTerminals().size() + " Terminals: [";
            for(Terminal term : getTerminals())
                out += "\n\t\t" + term.toString();
            out += "]";
        } else out += "\n\t  0 Terminals: [Empty]";
        if(unionizer == null) return out;
        List<Node> children = unionizer.getAllChildren(this);
        if(children == null) return out = "\n\t  Non-root, no children.";
        out += "\n\t  " + children.size() + " netlist child" + (children.size() == 1 ? ":" : "ren:");
        for(Node node : children) {
            Griddable<?> source = node.getSource();
            BlockPos bp = source == null ? null : source.getBlockPos();
            out += "\n\t\t  ↪ " + node.getSerializedName() + " " + node.getNodalIndex() + " (" + (source == null ? "no source" : source.getClass().getSimpleName() + " [" + bp.getX() + ", " + bp.getY() + ", " + bp.getZ() + "]") + ")";
        }
        return out;
    }

    default @Nullable Griddable<?> getSource() {
        if(!hasAncillaries()) return null;
        for(AncillaryNode node : getAncillaries()) {
            if(node == null) continue;
            Griddable<?> source = node.getSource();
            if(source == null) continue;
            return source;
        }
        return null;
    }

    public static class Joint implements Node {

        private CircuitComponent parent;
        protected @Nullable ObjectArrayList<Terminal> terminals;
        protected @Nullable List<AncillaryNode> ancillaries = null;
        private double voltage = 0;
        private int circuitIndex, nodalIndex;

        public Joint(CircuitComponent parent) {
            this.parent = parent;
            this.circuitIndex = -1;
            this.nodalIndex = -1;
        }

        @Override
        public boolean attach(Terminal pin) {
            Objects.requireNonNull(pin);
            if(this == pin.getNode()) 
                return false;
            pin.setConnectedTo(this);
            getTerminals().add(pin);
            return true;
        }

        @Override
		public boolean attach(Griddable<?> source, AncillaryNode jack) {
			if(ancillaries == null)
                ancillaries = new ArrayList<>();
            ancillaries.add(jack);
            jack.updateOwnership(source, this, 0);
            return true;
		}

        @Override
        public boolean detach(Terminal pin) {
            Objects.requireNonNull(pin);
            if(terminals.remove(pin)) {  
                pin.setConnectedTo(null);
                return true;
            }
            return false;
        }

        @Override
		public boolean detach(Griddable<?> source, AncillaryNode jack) {
			boolean removed = ancillaries.remove(jack);
            if(ancillaries.isEmpty()) ancillaries = null;
            if(removed) jack.updateOwnership(null, null, -1);
            return removed;
		}

        @Override
        public int getCircuitIndex() {
            return circuitIndex;
        }

        @Override
        public int getNodalIndex() {
            return nodalIndex;
        }

        @Override
        public void setNodalIndex(int nodalIndex) {
            this.nodalIndex = nodalIndex;
        }

        @Override
        public List<AncillaryNode> getAncillaries() {
            if(ancillaries == null) return Collections.emptyList();
            return ancillaries;
        }

        @Override public List<Terminal> getTerminals() { 
            return terminals; 
        }

        @Override
        public @Nullable CircuitComponent getParentComponent() {
            return parent;
        }

        @Override
        public void updateOwnership(Griddable<?> source, CircuitComponent parent, int index) {
            CircuitComponent.assertValidOwnership(this, parent);
            this.parent = parent;
            this.circuitIndex = index;
            if(source == null || !hasAncillaries()) return;
            for(AncillaryNode jack : ancillaries)
                jack.updateOwnership(source, this, 0);
        }

        @Override public void setVoltage(double volts) { 
            this.voltage = volts; 
        }

        @Override
        public double getVoltage() {
            return voltage;
        }

        @Override
        public void dispose() {
            this.parent = null;
            this.circuitIndex = -1;
            this.voltage = 0;
            this.terminals = null;
        }

        @Override
        public String toString() {
            return getSerializedName() + "[" + describeState() + "]";
        }
    }

    public static class GroundedJoint implements Node {

        protected @Nullable ObjectArrayList<Terminal> terminals;

        public GroundedJoint() {}

        @Override
        public GridUUID bindUUID(GridUUID id) {
            return id.withBinding(getType(), -1);
        }

        @Override
        public boolean attach(Terminal pin) {
            Objects.requireNonNull(pin);
            if(this == pin.getNode()) 
                return false;
            pin.setConnectedTo(this);
            getTerminals().add(pin);
            return true;
        }

        @Override
		public boolean attach(Griddable<?> source, AncillaryNode jack) {
			return false;
		}

        @Override
        public boolean detach(Terminal pin) {
            Objects.requireNonNull(pin);
            if(terminals.remove(pin)) {  
                pin.setConnectedTo(null);
                return true;
            }
            return false;
        }

        @Override
		public boolean detach(Griddable<?> source, AncillaryNode jack) {
			return false;
		}

        @Override
        public int getCircuitIndex() {
            return -1;
        }
        
        @Override
        public int getNodalIndex() {
            return -1;
        }

        @Override
        public void setNodalIndex(int nodalIndex) {
            return;
        }

        @Override
        public List<AncillaryNode> getAncillaries() {
            return Collections.emptyList();
        }

        @Override public List<Terminal> getTerminals() { 
            return terminals; 
        }

        @Override
        public @Nullable CircuitComponent getParentComponent() {
            return null;
        }

        @Override
        public void updateOwnership(Griddable<?> source, CircuitComponent parent, int index) {
            return;
        }

        @Override public void setVoltage(double volts) { 
            return;
        }

        @Override
        public double getVoltage() {
            return 0;
        }

        @Override
        public void dispose() {}

        @Override
        public String toString() {
            return getSerializedName() + "[" + describeState() + "]";
        }
    }
}


