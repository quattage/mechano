package com.quattage.mechano.api.circuit.topology;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.circuit.solver.NodalSnapshot;
import com.quattage.mechano.api.circuit.solver.NodalSnapshot.Stamper;
import com.quattage.mechano.api.circuit.topology.Joint.GroundedJoint;
import com.quattage.mechano.api.griddable.Griddable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class Circuit extends CircuitComponent {
    
    public static final double DELTA = 0.05d;
    public static final double DELTA_AH = DELTA / 3600d;
    public static final double EPSILON = 0.01d;

    protected ObjectArrayList<CircuitComponent> components;
    protected ObjectArrayList<Joint> joints;
    private @Nullable NodalSnapshot snapshot;

    public Circuit() {
        super("Circuit");
        this.components = new ObjectArrayList<>();
        this.joints = new ObjectArrayList<>();
        this.joints.add(new GroundedJoint(this));
    }

    public void addComponent(CircuitComponent component) {
        component.assertCanBeOwnedBy(this);
        this.components.add(component);
    }

    public boolean removeComponent(CircuitComponent component) {
        if(component.getParentComponent() != this) return false;
        detachTerminals(component.getTerminals());
        component.resetState();
        return true;
    }

    public Joint attachTerminals(Terminal termA, Terminal termB) {
        if(termA.getParentComponent() != this || termB.getParentComponent() != this) {
            throw new IllegalArgumentException("Failed while attempting to link terminals " 
                + termA + ", " + termB + " - These terminals don't belong to this circuit!");
        }
        Joint jointA = termA.getJoint(), jointB = termB.getJoint();
        if(jointA == null && jointB == null) {
            Joint newJoint = new Joint(this);
            joints.add(newJoint);
            newJoint.attach(termA);
            newJoint.attach(termB);
            return newJoint;
        }
        if(jointA != null && jointB == null) {
            jointA.attach(termB);
            return jointA;
        }
        if(jointA == null && jointB != null) {
            jointB.attach(termA);
            return jointB;
        }
        if(jointA.involves(termB)) return jointA;
        if(jointB.involves(termA)) return jointB;
        if(jointB.isStaticGround()) {
            joints.remove(jointA);
            return Joint.combine(jointB, jointA);
        }
        joints.remove(jointB);
        return Joint.combine(jointA, jointB);
    }
    
    public Joint attachTerminalToGround(Terminal term) {
        Joint ground = getCommonGround();
        ground.attach(term);
        return ground;
    }


    public Joint getCommonGround() {
        return joints.get(-1);
    }

    public boolean detachTerminals(Collection<Terminal> terminals) {
        if(terminals == null || terminals.size() <= 0) return false;
        boolean modified = false;
        for(Terminal term : terminals) {
            if(term == null || !term.hasJoint()) continue;
            final Joint joint = term.getJoint();
            if(!joint.detach(term)) continue;
            modified = true;
            if(!joint.hasConnections() && !joint.isStaticGround()) {
                this.joints.remove(joint);
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
        for(CircuitComponent component : components)
            output.addAll(component.getTerminals());
        output.trim();
        return output;
    }

    @Override
    public void tick(Griddable<?> host) {
        beginSolverStep();

    }

    public void beginSolverStep() {
        if(this.snapshot == null) this.snapshot = new NodalSnapshot();
        snapshot.loadOnto(this, joints.size(), 0);
        for(CircuitComponent component : components) {
            if(!(component instanceof Stamper stamper)) continue;
            stamper.stamp(this, this.snapshot);
        }
    }

    public List<Joint> getJoints() {
        return joints;
    }

    @Override // circuits cannot own other circuits
    public @Nullable CircuitComponent getParentComponent() {
        return null;
    }
}
