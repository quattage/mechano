package com.quattage.mechano.grid.api.component;

import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.GridDomain;
import com.quattage.mechano.foundation.Disposable;
import com.quattage.mechano.grid.GridTracking.ComponentHierarchy;
import com.quattage.mechano.grid.GridUUID.UUIDComposite;
import com.quattage.mechano.grid.Griddable;
import com.quattage.mechano.grid.HierarchicalConstruct;
import com.quattage.mechano.grid.topology.Node;

/**
 * A {@link CircuitComponent} with a singular function that can be
 * parented to another CircuitComponent.
 */
public abstract class DiscreteComponent implements CircuitComponent, HierarchicalConstruct, Disposable {

    private String componentID;
    private HierarchicalConstruct parent;

    public DiscreteComponent(String componentID) {
        CircuitComponent.assertValidID(componentID);
        this.componentID = componentID;
    }

    @Override public final String getComponentID() { return componentID; }
    @Override public boolean isGrounded() { return false; }
    @Override public void saturate() {}
    @Override public void reset() {}

    @Override
    public String toString() {
        return componentID;
    }

    @Override
    public ComponentHierarchy getHierarchyType() {
        return ComponentHierarchy.DISCRETE;
    }

    @Override
    public void updateOwnership(@Nullable Griddable<?> source, HierarchicalConstruct parent) {
        this.parent = parent;
    }

    @Override
    public @Nullable CircuitComponent getComponent(UUIDComposite id) {
        return this;
    }

    @Override
    public @Nullable HierarchicalConstruct getParentConstruct() {
        return parent;
    }

    @Override
    public void dispose() {
        componentID += " (disposed";
        parent = null;
    }

    public static class NodeStub extends DiscreteComponent {

        private Node node;

        public NodeStub(Node node) {
            super("NodeStub");
            Objects.requireNonNull(node);
            this.node = node;
        }

        @Override
        public int nodeCount() {
            assertNotDisposed();
            return node.nodeCount();
        }

        @Override
        public void forEachNode(Consumer<Node> cons) {
            cons.accept(node);
        }

        @Override
        public String toString() {
            return "NodeStub (" + node.toString() + ")";
        }

        @Override
        public int indexOfChild(HierarchicalConstruct child) {
            return child == node ? 0 : -1;
        }

        @Override
        public @Nullable CircuitComponent getComponent(UUIDComposite id) {
            return (id.getHierarchyType() == ComponentHierarchy.NODE && id.get() == 0) ? node : null;
        }

        @Override
        public ComponentHierarchy getHierarchyType() {
            return ComponentHierarchy.STUB;
        }

        @Override
        public void MNAAllocate(GridDomain domain) {
            // ignored safely
        }

        @Override
        public void MNADeallocate(GridDomain domain) {
            // ignored safely
        }

        @Override
        public int getDomainIndex() {
            assertNotDisposed();
            return node.getDomainIndex();
        }

        @Override
        public void updateOwnership(@Nullable Griddable<?> source, HierarchicalConstruct parent) {
            assertNotDisposed();
            super.updateOwnership(source, parent);
            node.updateOwnership(source, this);
        }

        @Override
        public void dispose() {
            this.node = null;
            super.dispose();
        }

        @Override
        public boolean hasBeenDisposed() {
            return this.node == null;
        }

        @Override
        public boolean equals(Object obj) {
            assertNotDisposed();
            if(this == obj) return true;
            if(obj instanceof Node tn) return this.node == tn;
            if(!(obj instanceof NodeStub that)) return false;
            that.assertNotDisposed();
            return this.node == that.node;
        }

        @Override
        public int hashCode() {
            assertNotDisposed();
            return node.hashCode();
        }
    }
}
