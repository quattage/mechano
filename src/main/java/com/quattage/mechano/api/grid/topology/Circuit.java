package com.quattage.mechano.api.grid.topology;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.CircuitFactory;
import com.quattage.mechano.api.grid.GridReferent;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.functional.Resistor;
import com.quattage.mechano.api.grid.topology.netlist.NodeUnionSet;
import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.api.grid.topology.vertex.Node.JointNode;
import com.quattage.mechano.api.grid.topology.vertex.Terminal;
import com.quattage.mechano.foundation.tracking.GridUUID;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.util.Mth;

/**
 * A localized graph which represents a collection of 
 * {@link FunctionalComponent functional components} connected together by 
 * nodes. This class can be instantiated by Griddables to define a single 
 * electric circuit with a discrete function. For example, an electric 
 * furnace may define a circuit containing a diode and a heating element.
 * The heating element may have a callback attached to it which allows
 * the BlockEntity to respond to electrical changes and smelt items.
 * <h3>A node on graph access:</h3>
 * The Circuit cannot provide direct access to the adjacency
 * status of itself or its constituents. The data contained within
 * this class is not assembled in any sort of legible graph
 * system. (that's what the {@link ServerGrid} is for) When connections 
 * are made, The {@link Node nodes} belonging to this circuit are flushed 
 * into the {@link NodeUnionSet} belonging to the active {@link ServerGrid}. 
 * This data is collected and captured as a snapshot by the grid, which is 
 * processed and solved off-thread. <strong>You cannot modify the voltage, 
 * current, or charge of any circuit elements from this class.</strong>
 */
public class Circuit implements CircuitComponent {
    
    public static final double DELTA = 0.05d;
    public static final double DELTA_AH = Circuit.DELTA / 3600d;
    public static final double EPSILON = 0.01d;

    protected ObjectArrayList<FunctionalComponent> components;
    protected ObjectArrayList<Node> nodes;

    public static Circuit ofSingleResistor(Griddable<?> source, float ohms) {
        Circuit out = new Circuit(source);
        Resistor res = new Resistor(ohms);
        out.addComponent(res);
        Node jA = new JointNode(out);
        Node jB = new JointNode(out);
        out.addJoint(jA);
        out.addJoint(jB);
        jA.localAttach(res.pinA());
        jB.localAttach(res.pinB());
        return out;
    }

    public Circuit(Griddable<?> source) {
        this.components = new ObjectArrayList<>();
        this.nodes = new ObjectArrayList<>(3);
    }

    /**
     * A constructor designed to be used by the {@link CircuitFactory}
     * @param source The instantiator {@link Griddable}
     * @param ground An initial ground object, if one is necessary
     * @param components A list of all {@link FunctionalComponents} that make up the circuit
     * @param preload A list of all {@link Node} objects that connect components together
     */
    public Circuit(Griddable<?> source, ObjectArrayList<FunctionalComponent> components, Set<Node> preload) {
        this.components = components;
        this.nodes = new ObjectArrayList<>(preload.size() + 3);
        for(CircuitComponent c : components)
            c.updateOwnership(this, 0);
        for(Node n : preload) { 
            this.nodes.add(n);
            n.updateOwnership(source, this, this.nodes.size() - 1);
        }
        trim(true);
    }

    public FunctionalComponent getComponent(int index) {
        return !isSignificant() ? null : components.get(Mth.clamp(index, 0, components.size() - 1));
    }

    public Node getNode(int index) {
        return !isSignificant() ? null : nodes.get(Mth.clamp(index, 0, nodes.size() - 1));
    }

    public void addComponent(FunctionalComponent component) {
        this.components.add(component);
        component.updateOwnership(this, 0);
    }

    public boolean removeComponent(CircuitComponent component) {
        if(component.getParentComponent() != this) return false;
        detachTerminals(component.getTerminals());
        component.reset();
        return true;
    }

    /**
     * Attach any two terminals together. 
     * @param termA
     * @param termB
     * @return
     */
    public Node attachTerminals(Terminal termA, Terminal termB) {
        if(termA.getParentComponent() != this || termB.getParentComponent() != this) {
            throw new IllegalArgumentException("Failed while attempting to link terminals " 
                + termA + ", " + termB + " - These terminals don't belong to this circuit!");
        }
        Node nodeA = termA.getNode(), nodeB = termB.getNode();
        Node primary = Node.choosePrimary(nodeA, nodeB);
        Node secondary = primary == nodeA ? nodeB : nodeA;
        if(primary == null) {
            JointNode newJoint = new JointNode(this);
            nodes.add(newJoint);
            newJoint.localAttach(termA);
            newJoint.localAttach(termB);
            return newJoint;
        }
        removeJoint(secondary);
        return Node.collapse(primary, secondary);
    }

    public void removeJoint(Node joint) {
        if(joint.getParentComponent() != this) {
            Mechano.LOGGER.warn("Skipped attempt to remove Joint instance from a non-owning circuit.");
            return;
        }
        if(joint.isGrounded()) {
            nodes.remove(0);
            for(int x = 0; x < nodes.size(); x++)
                nodes.get(x).updateOwnership(this, x);
            return;
        } 
        int i = joint.getCircuitIndex();
        nodes.remove(i); 
        // update the indices of all the shifted instnaces
        for(int x = i; x < nodes.size(); x++)
            nodes.get(x).updateOwnership(this, x);
    }

    public void addJoint(Node joint) {
        if(joint.isGrounded()) {
            if(this.isGrounded())
                throw new IllegalArgumentException("Cannot add a GroundedJoint to a Circuit that already contains one!");
            joint.updateOwnership(this, 0);
            this.nodes.addFirst(joint);
            for(int x = 1; x < nodes.size(); x++)
                nodes.get(x).updateOwnership(this, x);
            return;
        }
        joint.updateOwnership(this, nodes.size() - 1);
        this.nodes.add(joint);
    }

    /**
     * Selectively removes all redundant or empty features from this Circuit.
     * Internal collections will be resized to fit this circuit's current
     * contents.
     * @param log (Optional, defaults to <code>false</code>)
     * If <code>true</code> this Circuit will log any elements that are trimmed
     * for debugging purposes.
     */
    public void trim() { trim(false); }

    /**
     * Selectively removes all redundant or empty features from this Circuit.
     * Internal collections will be resized to fit this circuit's current
     * contents.
     * @param log (Optional, defaults to <code>false</code>)
     * If <code>true</code> this Circuit will log any elements that are trimmed
     * for debugging purposes.
     */
    public void trim(boolean log) {
        if(nodes != null && !nodes.isEmpty()) {
            Iterator<Node> nodeIter = nodes.iterator();
            while(nodeIter.hasNext()) {
                Node node = nodeIter.next();
                if(!node.isSignificant()) {
                    if(log) Mechano.LOGGER.warn(node + " was trimmed from circuit");
                    node.dispose();
                    nodeIter.remove();
                }
            }
            nodes.trim();
        }
        if(components != null && !components.isEmpty()) {
            Iterator<FunctionalComponent> componentIter = components.iterator();
            while(componentIter.hasNext()) {
                CircuitComponent component = componentIter.next();
                if(!component.isSignificant()) {
                    if(log) Mechano.LOGGER.warn(component + " was trimmed from circuit");
                    componentIter.remove();
                }
            }
            components.trim();
        }
    }

    public boolean detachTerminals(Collection<Terminal> terminals) {
        if(terminals == null || terminals.size() <= 0) return false;
        boolean modified = false;
        for(Terminal term : terminals) {
            if(term == null || !term.hasJoint()) continue;
            final Node joint = term.getNode();
            if(!joint.localDetach(term)) continue;
            modified = true;
            if(!joint.hasTerminals() && !joint.isGrounded()) {
                removeJoint(joint);
                joint.dispose();
            }
        }
        return modified;
    }

    public boolean arePinsConnected(Terminal a, Terminal b) {
        return a.hasJoint() && b.hasJoint() && a.getNode() == b.getNode();
    }

    @Override
    public Collection<Terminal> getTerminals() {
        ObjectOpenHashSet<Terminal> output = new ObjectOpenHashSet<>(components.size() * 3);
        for(CircuitComponent component : components) {
            if(!component.isSignificant()) continue;
            output.addAll(component.getTerminals());
        }
        output.trim();
        return output;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    @Override
    public void forEachNode(Consumer<Node> cons) {
        for(Node j : nodes) {
            if(!j.isSignificant()) continue;
            cons.accept(j);
        }
    }

    public void forEachComponent(Consumer<CircuitComponent> cons) {
        for(CircuitComponent comp : components) {
            if(!comp.isSignificant()) continue;
            cons.accept(comp);
        }
    }

    @Override
    public boolean isSignificant() {
        return nodes != null && !nodes.isEmpty();
    }

    @Override // circuits cannot own other circuits
    public @Nullable CircuitComponent getParentComponent() {
        return null;
    }

    @Override
    public String toString() {
        return getComponentID();
    }

    @Override
    public void saturate() {
        for(CircuitComponent c : components) c.saturate();
    }

    @Override
    public void reset() {
        for(CircuitComponent c : components) c.reset();
    }

    @Override
    public int size() {
        return nodes.size();
    }

    @Override
    public String getComponentID() {
        return "Circuit";
    }

    @Override
    public boolean isGrounded() {
        Node fn = this.nodes.getFirst();
        return fn != null && fn.isGrounded();
    }

    @Override
	public GridUUID bindUUID(GridUUID id) {
		return id.withBinding(getReferentType(), -1);
	}

    @Override
    public void updateOwnership(@Nullable Griddable<?> source, CircuitComponent parent, int index) {
        Mechano.LOGGER.warn("Skipped invalid attempt to update ownership of top-level circuit");
        return;
    }

    @Override
    public GridReferent getReferentType() {
        return GridReferent.COMPOSING_CIRCUIT;
    }
}
