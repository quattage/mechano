package com.quattage.mechano.api.grid.component;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.GridTracking.ComponentHierarchy;
import com.quattage.mechano.api.grid.GridUUID.UUIDComposite;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.HierarchicalConstruct;
import com.quattage.mechano.api.grid.topology.GridDomain;
import com.quattage.mechano.api.grid.topology.NodeUnionSet;
import com.quattage.mechano.api.grid.topology.landmark.Node;
import com.quattage.mechano.foundation.Disposable;
import com.quattage.mechano.infrastructure.BreakoutException;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * A localized graph which represents a collection of 
 * {@link DiscreteComponent functional components} connected together by 
 * nodes. This class can be instantiated by Griddables to define a single 
 * electric circuit with a discrete function. For example, an electric 
 * furnace may define a circuit containing a diode and a heating element.
 * The heating element may have a callback attached to it which allows
 * the BlockEntity to respond to electrical changes and smelt items.
 * <p> Circuits are constructed fluently using the {@link CircuitBuilder}.
 * It is not reccomended to invoke this class's constructor manually
 * unless you intend to populate it yourself.
 * <h3>A note on graph access:</h3>
 * The Circuit cannot provide direct access to the adjacency
 * status of itself or its constituents. The data contained within
 * this class is not assembled in any sort of legible graph
 * system. (that's what the {@link GridDomain} is for) When connections 
 * are made, The {@link Node nodes} belonging to this circuit are flushed 
 * into the {@link NodeUnionSet} belonging to the active {@link GridDomain}. 
 * This data is collected and captured as a snapshot by the grid, which is 
 * processed and solved off-thread. <strong>You cannot modify the voltage, 
 * current, or charge of any circuit elements from this class.</strong>
 */
public class Circuit implements CircuitComponent, HierarchicalConstruct, Disposable {

    protected Griddable<?> owner;
    protected ObjectArrayList<DiscreteComponent> components;
    private boolean disposed;

    protected Circuit() {}

    @Override
    public void updateOwnership(@Nullable Griddable<?> source, HierarchicalConstruct parent) {
        this.owner = source;
        forEachComponent(component -> {
            if(component instanceof HierarchicalConstruct gc)
                gc.updateOwnership(source, parent); 
        });
    }

    public DiscreteComponent getComponent(int index) {
        return components.get(index);
    }

    public void forEachComponent(Consumer<CircuitComponent> cons) {
        if(hasBeenDisposed()) return;
        for(CircuitComponent comp : components)
            cons.accept(comp);
    }

    @Override
    public void forEachNode(Consumer<Node> cons) {
        if(hasBeenDisposed()) return;
        forEachComponent(component -> component.forEachNode(cons));
    }

    @Override
    public int getDomainIndex() {
        assertNotDisposed();
        return components.getFirst().getDomainIndex();
    }

    @Override
    public boolean isGrounded() {
        try { forEachComponent(component -> {  // lol
            if(component.isGrounded()) throw new BreakoutException(); 
        }); } catch (BreakoutException e) { return true; };
        return false;
    }

    @Override 
    public void saturate() { 
        forEachComponent(CircuitComponent::saturate); 
    }

    @Override 
    public void reset() { 
        forEachComponent(CircuitComponent::reset); 
    }

    @Override
    public void MNAAllocate(ServerGrid grid, GridDomain domain) {
        forEachComponent(component -> component.MNAAllocate(grid, domain));
    }

    @Override
    public void MNADeallocate(ServerGrid grid, GridDomain domain) {
        forEachComponent(component -> component.MNADeallocate(grid, domain));
    }

    @Override
    public @Nullable CircuitComponent getComponent(UUIDComposite binding) {
        return components.get(binding.get());
    }

    @Override
    public @Nullable HierarchicalConstruct getParentConstruct() {
        return owner;
    }

    @Override
    public int indexOfChild(HierarchicalConstruct child) {
        if(!child.getHierarchyType().canBeOwnedBy(getHierarchyType())) return -1;
        return components.indexOf(child);
    }

    @Override
    public ComponentHierarchy getHierarchyType() {
        return ComponentHierarchy.CIRCUIT;
    }

    @Override
    public void dispose() {
        if(hasBeenDisposed()) return;
        for(DiscreteComponent component : components)
            component.dispose();
        disposed = true;
        components = null;
    }

    @Override
    public int getMergePriority() {
        return -components.size();
    }

    @Override
    public boolean hasBeenDisposed() {
        return disposed;
    }

    @Override
    public String getComponentID() {
        return disposed ? "Circuit (disposed)" : "Circuit";
    }

    @Override
    public String toString() {
        return getComponentID();
    }
}
