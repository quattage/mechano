package com.quattage.mechano.api.grid.topology;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.component.Resistor;
import com.quattage.mechano.api.grid.solver.NodeUnionSet;
import com.quattage.mechano.api.grid.topology.Node.GroundedJoint;
import com.quattage.mechano.api.grid.topology.Node.Joint;
import com.quattage.mechano.api.grid.topology.ancillary.AncillaryJack;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.resources.ResourceLocation;

/**
 * A graph which, in and of itself, is a CircuitComponent, but this graph
 * 
 */
public class Circuit implements CircuitComponent {
    
    public static final double DELTA = 0.05d;
    public static final double DELTA_AH = Circuit.DELTA / 3600d;
    public static final double EPSILON = 0.01d;

    protected ObjectArrayList<CircuitComponent> components;
    protected ObjectArrayList<Node> nodes;

    public static Circuit ofSingleResistor(float ohms) {
        Circuit out = new Circuit();
        Resistor res = new Resistor(ohms);
        out.addComponent(res);
        Node jA = new Joint(out);
        Node jB = new Joint(out);
        out.addJoint(jA);
        out.addJoint(jB);
        jA.attach(res.pinA());
        jB.attach(res.pinB());
        return out;
    }

    public Circuit() {
        this.components = new ObjectArrayList<>();
        this.nodes = new ObjectArrayList<>(3);
        this.nodes.add(new GroundedJoint(this));
    }

    public Circuit(Griddable source, @Nullable GroundedJoint ground, ObjectArrayList<CircuitComponent> components, Set<Node> preload) {
        this.components = components;
        this.nodes = new ObjectArrayList<>(preload.size() + 3);
        if(ground != null && ground.isSignificant()) {
            ground.updateOwnership(this, -1);
            this.nodes.add(ground);
        }
        for(CircuitComponent c : components)
            c.updateOwnership(this, 0);
        for(Node n : preload) { 
            this.nodes.add(n);
            n.updateOwnership(source, n, this.nodes.size() - 1);
        }
        trim(true);
    }

    public CircuitComponent getComponent(int index) {
        return !isSignificant() ? null : components.get(index);
    }

    public void addComponent(CircuitComponent component) {
        component.assertCanBeOwnedBy(this);        
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
        Node nodeB = termA.getJoint(), nodeA = termB.getJoint();
        if(nodeB == null && nodeA == null) {
            Joint newJoint = new Joint(this, nodes.size());
            nodes.add(newJoint);
            newJoint.attach(termA);
            newJoint.attach(termB);
            return newJoint;
        }
        if(nodeB != null && nodeA == null) {
            nodeB.attach(termB);
            return nodeB;
        }
        if(nodeB == null && nodeA != null) {
            nodeA.attach(termA);
            return nodeA;
        }
        if(nodeB.involves(termB)) return nodeB;
        if(nodeA.involves(termA)) return nodeA;
        // prioritize merging onto the grounded joint
        if(nodeA.isGrounded()) {
            removeJoint(nodeB);
            return NodeUnionSet.collapse(nodeA, nodeB);
        }
        removeJoint(nodeA);
        return NodeUnionSet.collapse(nodeB, nodeA);
    }

    public Node attachTerminalToGround(Terminal termA) {
        Node ground = getOrCreateCommonGround();
        ground.attach(termA);
        return ground;
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
        int i = joint.getIndex();
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

    public Node getOrCreateCommonGround() {
        Node fn = nodes.getFirst();
        if(fn != null && fn.isGrounded()) return fn;
        fn = new GroundedJoint(this);
        addJoint(fn);
        return fn;
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
            Iterator<Node> ni = nodes.iterator();
            while(ni.hasNext()) {
                Node n = ni.next();
                if(!n.isSignificant()) {
                    if(log) Mechano.LOGGER.warn(n + " was trimmed from circuit");
                    n.dispose();
                    ni.remove();
                }
            }
            nodes.trim();
        }
        if(components != null && !components.isEmpty()) {
            Iterator<CircuitComponent> ci = components.iterator();
            while(ci.hasNext()) {
                CircuitComponent c = ci.next();
                if(!c.isSignificant()) {
                    if(log) Mechano.LOGGER.warn(c + " was trimmed from circuit");
                    ci.remove();
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
            final Node joint = term.getJoint();
            if(!joint.detach(term)) continue;
            modified = true;
            if(!joint.hasConnections() && !joint.isGrounded()) {
                removeJoint(joint);
                joint.dispose();
            }
        }
        return modified;
    }

    public boolean arePinsConnected(Terminal a, Terminal b) {
        return a.hasJoint() && b.hasJoint() && a.getJoint() == b.getJoint();
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

    @Override
    public boolean isSignificant() {
        return nodes != null && !nodes.isEmpty() && components != null && !components.isEmpty();
    }

    @Override // circuits cannot own other circuits
    public @Nullable CircuitComponent getParentComponent() {
        return null;
    }

    @Override
    public String describeState() {
        String out = "";
        for(Node node : nodes) {
            out += "\n\tJoint " + node.getIndex() + " (" + node.hashCode() + ") - " + node.describeState();
            if(node.hasAncillaries()) {
                out += "\n\t\t- Ancillaries:";
                for(AncillaryJack jack : node.getAllAncillaries())
                    out += "\n\t\t\t" + jack;
            } else out += "\n\t\t- No ancillaries";
            if(node.hasConnections()) {
                out += "\n\t\t- Terminals:";
                for(Terminal t : node.getTerminals())
                    out += "\n\t\t\t" + t.describeSelf();
            } else out += "\n\t\t- No terminals";
        }
        return out + "\n";
    }

    @Override
    public String toString() {
        return getComponentID() + "@" + hashCode() + " {" + describeState() + "}";
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
    public ResourceLocation asResource() {
        return Mechano.asResource(getComponentID());
    }

    @Override
    public boolean isGrounded() {
        Node fn = this.nodes.getFirst();
        return fn != null && fn.isGrounded();
    }

    @Override
    public void updateOwnership(@Nullable Griddable source, CircuitComponent parent, int index) {
        Mechano.LOGGER.warn("Skipped invalid attempt to update ownership of top-level circuit");
        return;
    }

    @Override
    public CircuitComponent.Type getType() {
        return CircuitComponent.Type.COMPOSING_CIRCUIT;
    }
}
