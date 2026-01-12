package com.quattage.mechano.api.grid.topology.vertex;

import java.util.ArrayList;
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
import com.quattage.mechano.api.grid.component.ComponentUUID.ComponentBinding;
import com.quattage.mechano.api.grid.component.GridConstruct;
import com.quattage.mechano.api.grid.component.GridConstruct.TerminalProvider;
import com.quattage.mechano.api.grid.topology.netlist.NodeUnionSet;
import com.quattage.mechano.foundation.Disposable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public interface Node extends CircuitComponent, Disposable, GridConstruct, TerminalProvider {

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

    boolean localAttach(Terminal pin);
    boolean localAttach(@Nullable Griddable<?> source, AncillaryNode<?> jack);
    boolean localDetach(Terminal pin);
    boolean localDetach(@Nullable Griddable<?> source, AncillaryNode<?> jack);

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

    @Override
    void dispose();

    default boolean hasAncillaries() {
        return getAncillaries() != null && getAncillaries().size() > 0;
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

    @Override
    default void saturate() {
        forEachTerminal(Terminal::saturate);
    }

    @Override
    default void reset() {
        forEachTerminal(Terminal::reset);
    }

    @Override
    default boolean isGrounded() {
        return hasGroundedTerminal();
    }

    @Override
    default void forEachNode(Consumer<Node> cons) {
        cons.accept(this);
    }

    @Override
    default ComponentHierarchy getHierarchyType() {
        return ComponentHierarchy.NODE;
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

    @Override
    default int indexOfChild(GridConstruct child) {
        if(child.getHierarchyType() == ComponentHierarchy.ANCILLARY_NODE && hasAncillaries())
            return getAncillaries().indexOf(child);
        if(child.getHierarchyType() == ComponentHierarchy.TERMINAL && hasTerminals())
            return indexOfTerminal((Terminal) child);
        return -1;
    }

    public static class JointNode implements Node {

        private GridConstruct parent;
        protected @Nullable ObjectArrayList<Terminal> terminals;
        protected @Nullable List<AncillaryNode<?>> ancillaries = null;
        private int nodalIndex;

        public JointNode(GridConstruct parent) {
            this.parent = parent;
            this.nodalIndex = -1;
        }

        @Override
        public boolean localAttach(Terminal pin) {
            Objects.requireNonNull(pin);
            if(this == pin.getAttachedNode()) 
                return false;
            pin.setConnectedTo(this);
            terminals.add(pin);
            return true;
        }

        @Override
		public boolean localAttach(Griddable<?> source, AncillaryNode<?> jack) {
			if(ancillaries == null)
                ancillaries = new ArrayList<>();
            ancillaries.add(jack);
            jack.updateOwnership(source, this);
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
            if(removed) jack.dispose();
            return removed;
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

        @Override
        public Terminal[] getTerminals() {
            return terminals.toArray(new Terminal[terminals.size()]);
        }

        @Override
        public @Nullable GridConstruct getParentConstruct() {
            return parent;
        }

        @Override
        public void dispose() {
            this.parent = null;
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
        public void updateOwnership(@Nullable Griddable<?> source, GridConstruct parent) {
            GridConstruct.assertValidOwnership(this, parent);
            this.parent = parent;
            if(source == null || !hasAncillaries()) return;
            for(AncillaryNode<?> jack : ancillaries)
                jack.updateOwnership(source, this);
        }

        @Override
        public @Nullable CircuitComponent getComponent(ComponentBinding binding) {
            if(binding.getHierarchyType() == ComponentHierarchy.ANCILLARY_NODE) {
                if(!hasAncillaries()) return null;
                return ancillaries.get(binding.get());
            }
            return terminals.get(binding.get());
        }
    }

    public static class GroundNode implements Node {

        protected @Nullable ObjectArrayList<Terminal> terminals;

        public GroundNode() {}

        @Override
        public boolean localAttach(Terminal pin) {
            Objects.requireNonNull(pin);
            if(this == pin.getAttachedNode()) 
                return false;
            pin.setConnectedTo(this);
            terminals.add(pin);
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

        @Override
        public Terminal[] getTerminals() {
            return terminals.toArray(new Terminal[terminals.size()]);
        }

        @Override
        public @Nullable GridConstruct getParentConstruct() {
            return null;
        }

        @Override
        public ComponentHierarchy getHierarchyType() {
            return ComponentHierarchy.NONE;
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
        public @Nullable CircuitComponent getComponent(ComponentBinding binding) {
            return terminals.get(binding.get());
        }
    }
}