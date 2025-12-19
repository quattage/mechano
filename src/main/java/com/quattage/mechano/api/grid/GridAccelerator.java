package com.quattage.mechano.api.grid;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.grid.GridHierarchy.SourceIdentifier;
import com.quattage.mechano.api.grid.topology.Circuit;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.grid.topology.vertex.AncillaryNode;
import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.OrientationUpdatable;
import com.quattage.mechano.foundation.numeric.VectorOperations;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

/**
 * An acceleration structure which stores references to {@link AncillaryNode ancillaries}
 * belonging to a parent {@link CircuitComponent}. 
 * <p>
 * This class is especially useful in contexts (e.g. rendering) that need frequent access to 
 * node and link information. 
 */
public class GridAccelerator implements OrientationUpdatable, SourceIdentifier {

    private @Nullable AncillaryNode[] exposedJoints;

    public GridAccelerator() {}

    public GridAccelerator(Griddable<?> source) {
        initializeFrom(source);
    }

    public GridAccelerator(AncillaryNode[] exposedJoints) {
        if((exposedJoints != null && exposedJoints.length > 0))
            this.exposedJoints = exposedJoints;
    }

    public GridAccelerator initializeFrom(Griddable<?> source) {
        Objects.requireNonNull(source);
        CircuitComponent component = source.getCircuit();
        if(component == null) throw new NullPointerException("Griddable " + source + " couldn't provide a valid CircuitComponent!");
        return initializeFrom(component);
    }

    public GridAccelerator initializeFrom(CircuitComponent component) {
        Objects.requireNonNull(component);
        if((exposedJoints != null && exposedJoints.length > 0) || component == null || !component.isSignificant()) 
            return this;
        if(component instanceof Circuit) {
            Set<AncillaryNode> found = new ObjectOpenHashSet<>(component.size());
            component.forEachNode(joint -> {
                if(joint == null) throw new NullPointerException("Encountered a null pointer while updating ancillaries for lazy holder");
                found.addAll(joint.getAncillaries());
            });
            exposedJoints = found.isEmpty() ? null : found.toArray(new AncillaryNode[found.size()]);
            return this;
        }
        if(component instanceof Node n) {
            Collection<AncillaryNode> jacks = n.getAncillaries(); 
            exposedJoints = jacks == null || jacks.isEmpty() ? null : jacks.toArray(new AncillaryNode[jacks.size()]);
            return this;
        }
        Mechano.LOGGER.warn("Skipped attempt update ancillaries from an irrelevent source '" + component.getClass().getSimpleName() + "'");
        return this;
    }

    public void invalidate() {
        exposedJoints = null;
    }

    /**
     * Draws every {@link AncillaryNode}'s hitbox
     * to Create's outliner for debugging purposes.
     */
    public void showAll(Vector3d basis) {
        forEach(jack -> { jack.drawToOutliner(basis, VectorOperations.toColor(basis), 1f, 1f); });
    }

    /**
     * Updates the orientation of every {@link AncillaryNode}
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
     * Iterates over all {@link AncillaryNode jacks}
     * in this holder. The input consumer won't be executed
     * at all unless this holder has been {@link #initializeFrom initialized}
     * onto a {@link CircuitComponent} with at least one {@link AncillaryNode jack}
     * @param cons Consumer to execute for each jack
     */
    public void forEach(Consumer<AncillaryNode> cons) {
        if(isEmpty()) return;
        for(int x = 0; x < exposedJoints.length; x++) {
            AncillaryNode j = exposedJoints[x];
            if(j != null) cons.accept(j);
        }
    }

    public boolean isEmpty() {
        return exposedJoints == null || exposedJoints.length <= 0;
    }

    @Override
    public @NotNull Griddable<?> getSource() {
        if(isEmpty())
            throw new IllegalStateException("Failed while getting source griddable for a accelerator which hasn't been loaded!");
        return exposedJoints[0].getSource();
    }
}
