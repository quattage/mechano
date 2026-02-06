package com.quattage.mechano.api.grid.topology.landmark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.grid.GridTracking.ComponentHierarchy;
import com.quattage.mechano.api.grid.GridUUID.UUIDComposite;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.HierarchicalConstruct;
import com.quattage.mechano.api.grid.HierarchicalConstruct.SourceProvider;
import com.quattage.mechano.api.grid.HierarchicalConstruct.TerminalProvider;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.foundation.Disposable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public interface Node extends CircuitComponent, Disposable, HierarchicalConstruct, TerminalProvider, SourceProvider {

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

    @Override
    default int getMergePriority() {
        HierarchicalConstruct superparent = getSuperparent();
        return superparent.getMergePriority();
    }

    @Override
    default int indexOfChild(HierarchicalConstruct child) {
        if(child.getHierarchyType() == ComponentHierarchy.ANCILLARY && hasAncillaries())
            return getAncillaries().indexOf(child);
        if(child.getHierarchyType() == ComponentHierarchy.TERMINAL && hasTerminals())
            return indexOfTerminal((Terminal) child);
        return -1;
    }

    public static class JointNode implements Node {

        private String componentID;
        private HierarchicalConstruct parent;
        protected @Nullable ObjectArrayList<Terminal> terminals;
        protected @Nullable List<AncillaryNode<?>> ancillaries;

        public JointNode(HierarchicalConstruct parent) {
            this.parent = parent;
            this.componentID = "node";
        }

        public JointNode(HierarchicalConstruct parent, String componentID) {
            this.parent = parent;
            this.componentID = componentID;
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
        public @Nullable HierarchicalConstruct getParentConstruct() {
            return parent;
        }

        @Override
        public GridReferent<?> getProviderSource() {
            if(hasAncillaries()) {
                AncillaryNode<?> first = ancillaries.getFirst();
                if(first != null) return first.getProviderSource();
            }
            HierarchicalConstruct superparent = getSuperparent();
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
        public void updateOwnership(@Nullable Griddable<?> source, HierarchicalConstruct parent) {
            HierarchicalConstruct.assertValidOwnership(this, parent);
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
        public List<AncillaryNode<?>> getAncillaries() {
            return Collections.emptyList();
        }

        @Override
        public Terminal[] getTerminals() {
            if(terminals == null) return new Terminal[0];
            return terminals.toArray(new Terminal[terminals.size()]);
        }

        @Override
        public @Nullable HierarchicalConstruct getParentConstruct() {
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