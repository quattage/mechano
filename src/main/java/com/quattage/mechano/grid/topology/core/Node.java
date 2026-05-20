
package com.quattage.mechano.grid.topology.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.Griddable;
import com.quattage.mechano.foundation.Disposable;
import com.quattage.mechano.grid.GridTracking;
import com.quattage.mechano.grid.GridTracking.ComponentHierarchy;
import com.quattage.mechano.grid.topology.AncillaryNode;
import com.quattage.mechano.grid.topology.core.GridUUID.UUIDComposite;
import com.quattage.mechano.grid.topology.core.HierarchicalConstruct.SourceProvider;
import com.quattage.mechano.grid.topology.core.HierarchicalConstruct.TerminalProvider;

public abstract class Node implements CircuitComponent, HierarchicalConstruct, TerminalProvider, SourceProvider, Disposable {

    /**
     * Gets the node that has the lower merge priority between
     * <code>a</code> and </code>b</code>. Nodes with lower
     * merge priority should take precedence over nodes
     * with a higher merge priority.
     */
    public static Node choosePrimary(@Nullable Node a, @Nullable Node b) {
        if(a == null && b == null) return null;
        if(a != null && b == null) return a;
        if(b != null && a == null) return b;
        if(a.isGrounded() && !b.isGrounded()) return a;
        if(b.isGrounded() && !a.isGrounded()) return b;
        int amp = a.hasBeenDisposed() ? Integer.MAX_VALUE : a.getMergePriority();
        int bmp = b.hasBeenDisposed() ? Integer.MAX_VALUE : b.getMergePriority();
        if(amp < bmp) return a;
        if(bmp < amp) return b;
        return System.identityHashCode(a) > System.identityHashCode(b) ? b : a;
    }

    /**
     * Chooses the primary node between every node in the provided iterable object.
     * @param nodes Any iterable containing nodes to iterate through
     * @param maxIterations The maximum number of times to iterate
     * @return The most primary node candidate out of all nodes that were iterated
     * @see #choosePrimary(Node a, Node b)
     */
    public static @Nullable Node choosePrimary(Iterable<Node> nodes, int maxIterations) {
        Objects.requireNonNull(nodes);
        maxIterations = Math.max(1, maxIterations);
        Iterator<Node> iterator = nodes.iterator();
        Node output = null;
        int iterations = 0;
        while(iterator.hasNext() && iterations < maxIterations) {
            iterations++;
            output = Node.choosePrimary(output, iterator.next());
        }
        return output;
    }

    public abstract boolean localAttach(Terminal pin);
    public abstract boolean localAttach(@Nullable Griddable<?> source, AncillaryNode<?> jack);
    public abstract boolean localDetach(Terminal pin);
    public abstract boolean localDetach(@Nullable Griddable<?> source, AncillaryNode<?> jack);
    public void markGrounded() { markGrounded(true); }
    public abstract void markGrounded(boolean isGrounded);

    public abstract List<AncillaryNode<?>> getAncillaries();

    public boolean hasAncillaries() {
        return getAncillaries() != null && getAncillaries().size() > 0;
    }

    public boolean has(AncillaryNode<?> ancillary) {
        return indexOf(ancillary) >= 0;
    }

    public int indexOf(AncillaryNode<?> ancillary) {
        List<AncillaryNode<?>> ancillaries = getAncillaries();
        if(ancillaries == null || ancillaries.isEmpty())
            return -1;
        return ancillaries.indexOf(ancillary);
    }

    @Override
    public void saturate() {
        forEachTerminal(Terminal::saturate);
    }

    @Override
    public void reset() {
        forEachTerminal(Terminal::reset);
    }

    @Override
    public int nodeCount() {
        return 1;
    }

    @Override
    public void forEachNode(Consumer<Node> cons) {
        cons.accept(this);
    }

    @Override
    public ComponentHierarchy getHierarchyType() {
        return ComponentHierarchy.NODE;
    }


    @Override
    public int getMergePriority() {
        HierarchicalConstruct superparent = GridTracking.findSuperparent(this);
        return superparent == null || superparent instanceof Node
            ? getHierarchyType().getMergePriority()
            : superparent.getMergePriority();
    }

    @Override
    public int indexOfChild(HierarchicalConstruct child) {
        if(child.getHierarchyType() == ComponentHierarchy.ANCILLARY && hasAncillaries())
            return getAncillaries().indexOf(child);
        if(child.getHierarchyType() == ComponentHierarchy.TERMINAL && hasTerminals())
            return indexOfTerminal((Terminal) child);
        return -1;
    }

    public static class JointNode extends Node {

        private String componentID;
        private HierarchicalConstruct parent;
        protected @Nullable Terminal[] terminals = new Terminal[0];
        protected @Nullable List<AncillaryNode<?>> ancillaries;
        private boolean isGrounded = false;

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
            assertNotDisposed();
            if(this == pin.getAttachedNode()) 
                return false;
            pin.updateOwnership(this);
            terminals = Arrays.copyOf(terminals, terminals.length + 1);
            terminals[terminals.length - 1] = pin;
            return true;
        }

        @Override
		public boolean localAttach(Griddable<?> source, AncillaryNode<?> jack) {
            Objects.requireNonNull(jack);
            assertNotDisposed();
			if(ancillaries == null)
                ancillaries = new ArrayList<>();
            ancillaries.add(jack);
            jack.updateOwnership(source, this);
            return true;
		}

        @Override
        public boolean localDetach(Terminal pin) {
            Objects.requireNonNull(pin);
            assertNotDisposed();
            terminals = ArrayUtils.removeElement(terminals, pin);
            pin.updateOwnership(null);
            return true;
        }

        @Override
		public boolean localDetach(Griddable<?> source, AncillaryNode<?> jack) {
            Objects.requireNonNull(jack);
            assertNotDisposed();
            if(ancillaries == null) return false;
            if(ancillaries.remove(jack)) {  
                jack.updateOwnership(null);
                return true;
            }
            if(ancillaries.isEmpty()) ancillaries = null;
            return false;
		}

        @Override
        public boolean isGrounded() {
            return isGrounded;
        }

        @Override
        public void markGrounded(boolean isGrounded) {
            assertNotDisposed();
            this.isGrounded = isGrounded;
        }

        @Override
        public List<AncillaryNode<?>> getAncillaries() {
            assertNotDisposed();
            if(ancillaries == null) return Collections.emptyList();
            return ancillaries;
        }

        @Override
        public Terminal[] getTerminals() {
            assertNotDisposed();
            return terminals;
        }

        @Override
        public @Nullable HierarchicalConstruct getParentConstruct() {
            assertNotDisposed();
            return parent;
        }

        @Override
        public GridReferent<?> getReferent() {
            assertNotDisposed();
            if(!hasAncillaries()) return null;
            AncillaryNode<?> first = ancillaries.getFirst();
            return first == null ? null : first.getReferent();
        }

        @Override
        public void dispose() {
            assertNotDisposed();
            for(AncillaryNode<?> node : ancillaries) node.dispose();
            this.ancillaries = null;
            this.parent = null;
            this.terminals = null;
            this.componentID += " (disposed)";
        }

        @Override
        public boolean hasBeenDisposed() {
            return terminals == null;
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
            assertNotDisposed();
            HierarchicalConstruct.assertValidOwnership(this, parent);
            this.parent = parent;
            if(source == null || !hasAncillaries()) return;
            for(AncillaryNode<?> jack : ancillaries)
                jack.updateOwnership(source, this);
        }

        @Override
        public @Nullable CircuitComponent getComponent(UUIDComposite binding) {
            assertNotDisposed();
            if(binding.getHierarchyType() == ComponentHierarchy.ANCILLARY) {
                if(!hasAncillaries()) return null;
                return ancillaries.get(binding.get());
            }
            int idx = binding.get();
            if(idx <= 0 || idx > terminals.length) throw new IndexOutOfBoundsException("Can't get terminal for " + binding + " - The terminal index is invalid for a target node with " + terminals.length + " terminal(s)");
            return terminals[idx];
        }
    }
}