package com.quattage.mechano.api.grid;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.grid.topology.Circuit;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.grid.topology.Node;
import com.quattage.mechano.api.grid.topology.ancillary.AncillaryJack;
import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.OrientationUpdatable;
import com.quattage.mechano.foundation.numeric.VectorOperations;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * An acceleration structure which stores references to {@link AncillaryJack jacks}
 * belonging to a parent {@link CircuitComponent}. This class can be thought of
 * as the summary of a {@link Griddable griddable}'s access to the outside world.
 * 
 * <p>
 * Useful for frequent operations that require access to node information, 
 * such as jack rendering.
 */
public class GriddableTerminus implements OrientationUpdatable {

    private @Nullable AncillaryJack[] exposedJoints;

    public GriddableTerminus() {}

    public GriddableTerminus(Griddable source) {
        initializeFrom(source);
    }

    public GriddableTerminus(AncillaryJack[] exposedJoints) {
        if((exposedJoints != null && exposedJoints.length > 0))
            this.exposedJoints = exposedJoints;
    }

    @OnlyIn(Dist.CLIENT)
    public GriddableTerminus initializeFrom(Griddable source) {
        Objects.requireNonNull(source);
        CircuitComponent component = source.getCircuit();
        if(component == null) throw new NullPointerException("Griddable " + source + " couldn't provide a valid CircuitComponent!");
        return initializeFrom(component);
    }

    @OnlyIn(Dist.CLIENT)
    public GriddableTerminus initializeFrom(CircuitComponent component) {
        Objects.requireNonNull(component);
        if((exposedJoints != null && exposedJoints.length > 0) || component == null || !component.isSignificant()) 
            return this;
        if(component instanceof Circuit) {
            Set<AncillaryJack> found = new ObjectOpenHashSet<>(component.size());
            component.forEachNode(joint -> {
                if(joint == null) throw new NullPointerException("Encountered a null pointer while updating ancillaries for lazy holder");
                found.addAll(joint.getAllAncillaries());
            });
            exposedJoints = found.isEmpty() ? null : found.toArray(new AncillaryJack[found.size()]);
            return this;
        }
        if(component instanceof Node n) {
            Collection<AncillaryJack> jacks = n.getAllAncillaries(); 
            exposedJoints = jacks == null || jacks.isEmpty() ? null : jacks.toArray(new AncillaryJack[jacks.size()]);
            return this;
        }
        Mechano.LOGGER.warn("Skipped attempt update ancillaries from an irrelevent source '" + component.getClass().getSimpleName() + "'");
        return this;
    }

    @OnlyIn(Dist.CLIENT)
    public void invalidate() {
        exposedJoints = null;
    }

    /**
     * Draws every {@link AncillaryJack}'s hitbox
     * to Create's outliner for debugging purposes.
     */
    public void showAll(Vector3d basis) {
        forEach(jack -> { jack.drawToOutliner(basis, VectorOperations.toColor(basis), 1f, 1f); });
    }

    /**
     * Updates the orientation of every {@link AncillaryJack}
     * which has a {@link CombinedOrientation directional orientation}
     * @see OrientationUpdatable    
     */
    @Override
    public void updateOrientation(CombinedOrientation dir) {
        forEach(jack -> {
            if(jack instanceof OrientationUpdatable ou)
                ou.updateOrientation(dir);
        });
    }

    /**
     * Iterates over all {@link AncillaryJack jacks}
     * in this holder. The input consumer won't be executed
     * at all unless this holder has been {@link #initializeFrom initialized}
     * onto a {@link CircuitComponent} with at least one {@link AncillaryJack jack}
     * @param cons Consumer to execute for each jack
     */
    @OnlyIn(Dist.CLIENT)
    public void forEach(Consumer<AncillaryJack> cons) {
        if(exposedJoints == null || exposedJoints.length <= 0) return;
        for(int x = 0; x < exposedJoints.length; x++) {
            AncillaryJack j = exposedJoints[x];
            if(j != null) cons.accept(j);
        }
    }
}
