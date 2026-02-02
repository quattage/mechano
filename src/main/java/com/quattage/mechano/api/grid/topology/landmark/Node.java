package com.quattage.mechano.api.grid.topology.landmark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.GridConstruct;
import com.quattage.mechano.api.grid.GridConstruct.SourceProvider;
import com.quattage.mechano.api.grid.GridConstruct.TerminalProvider;
import com.quattage.mechano.api.grid.GridTracking.ComponentHierarchy;
import com.quattage.mechano.api.grid.GridUUID.UUIDComposite;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.topology.NodeUnionSet;
import com.quattage.mechano.foundation.Disposable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public interface Node extends CircuitComponent, Disposable, GridConstruct, TerminalProvider, SourceProvider {

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
        return superparent.getMergePriority();
    }

    @Override
    default int indexOfChild(GridConstruct child) {
        if(child.getHierarchyType() == ComponentHierarchy.ANCILLARY && hasAncillaries())
            return getAncillaries().indexOf(child);
        if(child.getHierarchyType() == ComponentHierarchy.TERMINAL && hasTerminals())
            return indexOfTerminal((Terminal) child);
        return -1;
    }

    public static class JointNode implements Node {

        private String componentID;
        private GridConstruct parent;
        protected @Nullable ObjectArrayList<Terminal> terminals;
        protected @Nullable List<AncillaryNode<?>> ancillaries;
        private int nodalIndex;

        public JointNode(GridConstruct parent) {
            this.parent = parent;
            this.componentID = "node";
            this.nodalIndex = -1;
        }

        public JointNode(GridConstruct parent, String componentID) {
            this.parent = parent;
            this.componentID = componentID;
            this.nodalIndex = -1;
        }


        @Override
        public boolean localAttach(Terminal pin) {
            Objects.requireNonNull(pin);
            if(this == pin.getAttachedNode()) 
                return false;
            pin.updateOwnership(this);
            if(terminals == null) terminals = new ObjectArrayList<>();
            terminals.add(pin);
            return true;
        }

        @Override
		public boolean localAttach(Griddable<?> source, AncillaryNode<?> jack) {
            Objects.requireNonNull(jack);
			if(ancillaries == null)
                ancillaries = new ArrayList<>();
            ancillaries.add(jack);
            jack.updateOwnership(source, this);
            return true;
		}

        @Override
        public boolean localDetach(Terminal pin) {
            Objects.requireNonNull(pin);
            if(terminals == null) return false;
            if(terminals.remove(pin)) {  
                pin.updateOwnership(null);
                return true;
            }
            if(terminals.isEmpty()) terminals = null;
            return false;
        }

        @Override
		public boolean localDetach(Griddable<?> source, AncillaryNode<?> jack) {
            Objects.requireNonNull(jack);
            if(ancillaries == null) return false;
            if(ancillaries.remove(jack)) {  
                jack.updateOwnership(null);
                return true;
            }
            if(ancillaries.isEmpty()) ancillaries = null;
            return false;
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
        public List<AncillaryNode<?>> getAncillaries() {
            if(ancillaries == null) return Collections.emptyList();
            return ancillaries;
        }

        @Override
        public Terminal[] getTerminals() {
            if(terminals == null) return new Terminal[0];
            return terminals.toArray(new Terminal[terminals.size()]);
        }

        @Override
        public @Nullable GridConstruct getParentConstruct() {
            return parent;
        }

        @Override
        public GridReferent<?> getProviderSource() {
            if(hasAncillaries()) {
                AncillaryNode<?> first = ancillaries.getFirst();
                if(first != null) return first.getProviderSource();
            }
            GridConstruct superparent = getSuperparent();
            return superparent instanceof GridReferent<?> gr ? gr : null;
        }

        @Override
        public void dispose() {
            this.parent = null;
            this.terminals = null;
            this.componentID += " (disposed)";
        }

        @Override
        public boolean hasBeenDisposed() {
            return parent == null && terminals == null;
        }

        @Override
        public String getComponentID() {
            return componentID;
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
        public @Nullable CircuitComponent getComponent(UUIDComposite binding) {
            if(binding.getHierarchyType() == ComponentHierarchy.ANCILLARY) {
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
            pin.updateOwnership(this);
            if(terminals == null) terminals = new ObjectArrayList<>();
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
            if(terminals == null) return false;
            if(terminals.remove(pin)) {  
                pin.updateOwnership(null);
                return true;
            }
            if(terminals.isEmpty()) terminals = null;
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
            if(terminals == null) return new Terminal[0];
            return terminals.toArray(new Terminal[terminals.size()]);
        }

        @Override
        public @Nullable GridConstruct getParentConstruct() {
            return null;
        }

        @Override
        public GridReferent<?> getProviderSource() {
            return null;
        }

        @Override
        public ComponentHierarchy getHierarchyType() {
            return ComponentHierarchy.STRANGER;
        }

        @Override
        public void dispose() {}

        @Override
        public boolean hasBeenDisposed() { return false; }

        @Override
        public String getComponentID() {
            return "ground";
        }

        @Override
        public String toString() {
            return getComponentID();
        }

        @Override
        public @Nullable CircuitComponent getComponent(UUIDComposite binding) {
            return terminals.get(binding.get());
        }
    }
}