package com.quattage.mechano.api.grid.component;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.GridTracking.ComponentHierarchy;
import com.quattage.mechano.api.grid.GridUUID.UUIDComposite;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.HierarchicalConstruct;
import com.quattage.mechano.api.grid.topology.landmark.Node;
import com.quattage.mechano.foundation.Disposable;

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
            this.node = node;
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
        public void MNAAllocate(ServerGrid grid) {
            
        }

        @Override
        public void MNADeallocate(ServerGrid grid) {
            
        }

        @Override
        public void updateOwnership(@Nullable Griddable<?> source, HierarchicalConstruct parent) {
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
            if(this == obj) return true;
            if(obj instanceof Node tn) return this.node == tn;
            if(!(obj instanceof NodeStub that)) return false;
            return this.node == that.node;
        }

        @Override
        public int hashCode() {
            return node.hashCode();
        }
    }
}
