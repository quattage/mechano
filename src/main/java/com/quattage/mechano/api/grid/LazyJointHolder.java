package com.quattage.mechano.api.grid;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.grid.topology.Circuit;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.grid.topology.Node;
import com.quattage.mechano.api.grid.topology.ancillary.AncillaryJack;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Stores AncillaryJack instances on the client so that
 * they don't have to be looked up every render tick
 */
public class LazyJointHolder {

    private @Nullable AncillaryJack[] exposedJoints;

    @Nullable
    @OnlyIn(Dist.CLIENT)
    public LazyJointHolder updateAncillaries(CircuitComponent component) {
        if(exposedJoints != null || component == null || !component.isSignificant()) return this;
        if(component instanceof Circuit) {
            Set<AncillaryJack> found = new ObjectOpenHashSet<>(component.size());
            component.forEachJoint(joint -> {
                if(joint == null) throw new NullPointerException("Encountered a null pointer while updating ancillaries for lazy holder");
                found.addAll(joint.getAllAncillaries());
            });
            exposedJoints = found.isEmpty() ? null : (AncillaryJack[])found.toArray();
            return this;
        }
        if(component instanceof Node n) {
            Collection<AncillaryJack> jacks = n.getAllAncillaries(); 
            exposedJoints = jacks == null || jacks.isEmpty() ? null : (AncillaryJack[])jacks.toArray();
            return this;
        }
        Mechano.LOGGER.warn("Skipped attempt update ancillaries from an irrelevent source '" + component.getClass().getSimpleName() + "'");
        return this;
    }

    @OnlyIn(Dist.CLIENT)
    public void invalidate() {
        exposedJoints = null;
    }

    @OnlyIn(Dist.CLIENT)
    public void forEach(Consumer<AncillaryJack> cons) {
        if(exposedJoints == null || exposedJoints.length <= 0) return;
        for(int x = 0; x < exposedJoints.length; x++) {
            AncillaryJack j = exposedJoints[x];
            if(j != null) cons.accept(j);
        }
    }
}
