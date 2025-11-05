package com.quattage.mechano.api.griddable;

import java.util.Set;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.grid.topology.Circuit;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.grid.topology.Node;
import com.quattage.mechano.api.grid.topology.ancillary.AncillaryJack;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

/**
 * Stores AncillaryJoint instances on the client so that
 * they don't have to be looked up every render tick
 */
public class LazyJointHolder {

    private @Nullable AncillaryJack[] exposedJoints;

    @Nullable
    public LazyJointHolder updateAncillaries(CircuitComponent component) {
        if(exposedJoints != null || component == null || !component.isSignificant()) return this;
        if(component instanceof Circuit) {
            Set<AncillaryJack> found = new ObjectOpenHashSet<>(component.size());
            component.forEachJoint(joint -> {
                if(joint == null) throw new NullPointerException("Encountered a null pointer while updating ancillaries for lazy holder");
                found.addAll(joint.getAllAncillaries());
            });
            exposedJoints = (AncillaryJack[])found.toArray();
            return this;
        }
        if(component instanceof Node n) {
            exposedJoints = (AncillaryJack[])n.getAllAncillaries().toArray();
            return this;
        }
        Mechano.LOGGER.warn("Skipped attempt update ancillaries from an irrelevent source '" + component.getClass().getSimpleName() + "'");
        return this;
    }

    /**
     * Should be called whehever the joint source is updated.
     */
    public void invalidate() {
        exposedJoints = null;
    }

    public void forEach(Consumer<AncillaryJack> cons) {
        if(exposedJoints == null) return;
        for(int x = 0; x < exposedJoints.length; x++) {
            AncillaryJack j = exposedJoints[x];
            if(j != null) cons.accept(j);
        }
    }
}
