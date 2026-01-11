package com.quattage.mechano.api.grid.topology.vertex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.component.ComponentTracker.ComponentHierarchy;
import com.quattage.mechano.api.grid.component.ComponentUUID;
import com.quattage.mechano.api.grid.component.GridConstruct;
import com.quattage.mechano.api.grid.topology.netlist.NodeUnionSet;
import com.quattage.mechano.foundation.Disposable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public interface Node extends CircuitComponent, Disposable, GridConstruct {

    /**
     * Gets the node that has the lower merge priority between
     * <code>a</code> and </code>b</code>. Nodes with lower
     * merge priority should take precedence over nodes
     * with a higher merge priority.
     */
    static Node choosePrimary(Node a, Node b) {
        if(a == null && b == null) return null;
        if(a != null && b == null) return a;
        if(b != null && a == null) return b;
        int amp = a.getMergePriority();
        int bmp = b.getMergePriority();
        if(amp < bmp) return a;
        if(bmp < amp) return b;
        return System.identityHashCode(a) > System.identityHashCode(b) ? b : a;
    }

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

    boolean localAttach(Terminal pin);
    boolean localAttach(@Nullable Griddable<?> source, AncillaryNode<?> jack);
    boolean localDetach(Terminal pin);
    boolean localDetach(@Nullable Griddable<?> source, AncillaryNode<?> jack);

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

    List<AncillaryNode<?>> getAncillaries();
    @Override List<Terminal> getTerminals();

    @Override
    void dispose();

    default boolean hasTerminals() { 
        Collection<Terminal> terminals = getTerminals();
        return terminals != null && !terminals.isEmpty();
    }

    default boolean hasAncillaries() {
        return getAncillaries() != null && getAncillaries().size() > 0;
    }

    default boolean has(Terminal terminal) {
        return indexOf(terminal) >= 0;
    }

    default boolean has(AncillaryNode<?> ancillary) {
        return indexOf(ancillary) >= 0;
    }

    default int indexOf(AncillaryNode<?> ancillary) {
        List<AncillaryNode<?>> ancillaries = getAncillaries();
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
        forEachTerminal(Terminal::saturate);
    }

    @Override
    default void reset() {
        forEachTerminal(Terminal::reset);
    }

    default void forEachTerminal(Consumer<Terminal> cons) {
        List<Terminal> terminals = getTerminals();
        if(terminals == null || terminals.isEmpty())
            return;
        for(Terminal t : terminals)
            cons.accept(t);
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
    default ComponentHierarchy getHierarchyType() {
        return ComponentHierarchy.EMITTER_NODE;
    }

    default double getVoltage(ServerGrid grid) {
        int idx = getNodalIndex();
        if(idx < 0) return 0;
        DMatrixRMaj solution = grid.getSolution();
        if(solution == null || solution.numRows <= 0) return 0;
        if(idx >= solution.numRows) return 0;
        return solution.get(idx, 0);
    }

    @Override
    default int getMergePriority() {
        GridConstruct superparent = getSuperparent();
        return superparent.getHierarchyType().getMergePriority();
    }

    public static class JointNode implements Node {

        private GridConstruct parent;
        protected @Nullable ObjectArrayList<Terminal> terminals;
        protected @Nullable List<AncillaryNode<?>> ancillaries = null;
        private int circuitIndex, nodalIndex;

        public JointNode(GridConstruct parent) {
            this.parent = parent;
            this.circuitIndex = -1;
            this.nodalIndex = -1;
        }

        @Override
        public boolean localAttach(Terminal pin) {
            Objects.requireNonNull(pin);
            if(this == pin.getNode()) 
                return false;
            pin.setConnectedTo(this);
            getTerminals().add(pin);
            return true;
        }

        @Override
		public boolean localAttach(Griddable<?> source, AncillaryNode<?> jack) {
			if(ancillaries == null)
                ancillaries = new ArrayList<>();
            ancillaries.add(jack);
            jack.updateOwnership(source, this, 0);
            return true;
		}

        @Override
        public boolean localDetach(Terminal pin) {
            Objects.requireNonNull(pin);
            if(terminals.remove(pin)) {  
                pin.setConnectedTo(null);
                return true;
            }
            return false;
        }

        @Override
		public boolean localDetach(Griddable<?> source, AncillaryNode<?> jack) {
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
            if(nodalIndex < 0 && !isGrounded()) {
                Mechano.LOGGER.warn("nodal index for " + this + " returned invalid value (" 
                    + nodalIndex + ") - Perhaps the matrix hasn't been initialized?");
            }
            return nodalIndex;
        }

        @Override
        public void setNodalIndex(int nodalIndex) {
            this.nodalIndex = nodalIndex;
        }

        @Override
        public List<AncillaryNode<?>> getAncillaries() {
            if(ancillaries == null) return Collections.emptyList();
            return ancillaries;
        }

        @Override public List<Terminal> getTerminals() { 
            return terminals; 
        }

        @Override
        public @Nullable GridConstruct getParentConstruct() {
            return parent;
        }

        @Override
        public void dispose() {
            this.parent = null;
            this.circuitIndex = -1;
            this.terminals = null;
        }

        @Override
        public String getComponentID() {
            return "JointNode";
        }

        @Override
        public String toString() {
            return getComponentID();
        }

        @Override
        public void updateOwnership(@Nullable Griddable<?> source, GridConstruct parent, int index) {
            GridConstruct.assertValidOwnership(this, parent);
            this.parent = parent;
            this.circuitIndex = index;
            if(source == null || !hasAncillaries()) return;
            for(AncillaryNode<?> jack : ancillaries)
                jack.updateOwnership(source, this, 0);
        }

        @Override
        public <T extends ComponentUUID<T>> T bindUUID(T id) {
            throw new UnsupportedOperationException("Unimplemented method 'bindUUID'");
        }

        @Override
        public @Nullable CircuitComponent findSubComponent(ComponentUUID<?> id) {
            throw new UnsupportedOperationException("Unimplemented method 'findComponent'");
        }
    }

    public static class GroundNode implements Node {

        protected @Nullable ObjectArrayList<Terminal> terminals;

        public GroundNode() {}

        @Override
        public boolean localAttach(Terminal pin) {
            Objects.requireNonNull(pin);
            if(this == pin.getNode()) 
                return false;
            pin.setConnectedTo(this);
            getTerminals().add(pin);
            return true;
        }

        @Override
		public boolean localAttach(Griddable<?> source, AncillaryNode<?> jack) {
			return false;
		}

        @Override
        public boolean localDetach(Terminal pin) {
            Objects.requireNonNull(pin);
            if(terminals.remove(pin)) {  
                pin.setConnectedTo(null);
                return true;
            }
            return false;
        }

        @Override
		public boolean localDetach(Griddable<?> source, AncillaryNode<?> jack) {
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
        public List<AncillaryNode<?>> getAncillaries() {
            return Collections.emptyList();
        }

        @Override public List<Terminal> getTerminals() { 
            return terminals; 
        }

        @Override
        public @Nullable GridConstruct getParentConstruct() {
            return null;
        }

        @Override
        public void updateOwnership(Griddable<?> source, GridConstruct parent, int index) {
            return;
        }

        @Override
        public void dispose() {}

        @Override
        public String getComponentID() {
            return "GroundedNode";
        }

        @Override
        public String toString() {
            return getComponentID();
        }

        @Override
        public <T extends ComponentUUID<T>> T bindUUID(T id) {
            return id;
        }

        @Override
        public @Nullable CircuitComponent findSubComponent(ComponentUUID<?> id) {
            return null;
        }
    }
}