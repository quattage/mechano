package com.quattage.mechano.api.grid;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.topology.Circuit;
import com.quattage.mechano.api.grid.topology.vertex.AncillaryNode;
import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.api.grid.topology.vertex.WireJack;
import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.OrientationUpdatable;
import com.quattage.mechano.foundation.numeric.VectorOperations;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

/**
 * An acceleration structure which stores references to {@link AncillaryNode ancillaries}
 * belonging to a parent {@link Griddable}. 
 * <p>
 * This class is especially useful in contexts (e.g. rendering) that need frequent access to 
 * node and link information. 
 */
public class GriddableTerminus implements OrientationUpdatable {

    private @Nullable AncillaryNode<?>[] exposedJoints;

    public GriddableTerminus() {}

    public GriddableTerminus(Griddable<?> source) {
        initializeFrom(source);
    }

    public GriddableTerminus(AncillaryNode<?>[] exposedJoints) {
        if((exposedJoints != null && exposedJoints.length > 0))
            this.exposedJoints = exposedJoints;
    }

    public GriddableTerminus initializeFrom(Griddable<?> source) {
        Objects.requireNonNull(source);
        CircuitComponent component = source.getComponent();
        if(component == null) throw new NullPointerException("Griddable " + source + " couldn't provide a valid CircuitComponent!");
        return initializeFrom(component);
    }

    public GriddableTerminus initializeFrom(CircuitComponent component) {
        Objects.requireNonNull(component);
        if((exposedJoints != null && exposedJoints.length > 0) || component == null) 
            return this;
        if(component instanceof Circuit) {
            Set<AncillaryNode<?>> found = new ObjectOpenHashSet<>();
            component.forEachNode(joint -> {
                if(joint == null) throw new NullPointerException("Encountered a null ancillary while initializing terminus!");
                found.addAll(joint.getAncillaries());
            });
            exposedJoints = found.isEmpty() ? null : found.toArray(new AncillaryNode[found.size()]);
            return this;
        }
        if(component instanceof Node n) {
            Collection<AncillaryNode<?>> jacks = n.getAncillaries(); 
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
    public void forEach(Consumer<AncillaryNode<?>> cons) {
        if(isEmpty()) return;
        for(int x = 0; x < exposedJoints.length; x++) {
            AncillaryNode<?> j = exposedJoints[x];
            if(j != null) cons.accept(j);
        }
    }

    /**
     * @return The first reachable {@link AncillaryNode} in this
     * terminus's internal array. If this terminus has not
     * yet been {@link #initializeFrom() initialized}, this method
     * will always return <code>null</code>
     */
    public @Nullable AncillaryNode<?> getFirstAncillary() {
        if(isEmpty()) return null;
        for(int x = 0; x < exposedJoints.length; x++) {
            AncillaryNode<?> node = exposedJoints[x];
            if(node instanceof WireJack) return node;
        }
        return exposedJoints[0];
    }

    /**
     * @return The {@link AncillaryNode} at the given index
     * in this terminus's internal array. If this terminus has not
     * yet been {@link #initializeFrom() initialized}, this method
     * will always return <code>null</code>
     */
    public @Nullable AncillaryNode<?> getAncillary(int index) {
        if(isEmpty()) return null;
        if(index >= exposedJoints.length || index < 0) return null;
        return exposedJoints[index];
    }

    public AncillaryNode<?>[] getAncillaries() {
        return exposedJoints;
    }

    public boolean isEmpty() {
        return size() <= 0;
    }

    public int size() {
        return exposedJoints == null ? 0 : exposedJoints.length;
    }

    public @NotNull Griddable<?> getProviderSource() {
        if(isEmpty())
            throw new IllegalStateException("Failed while getting source griddable for a terminus which hasn't been loaded!");
        for(int x = 0; x < exposedJoints.length; x++) {
            AncillaryNode<?> ancillary = exposedJoints[x];
            if(ancillary == null) continue;
            Griddable<?> source = ancillary.getProviderSource();
            if(source != null) return source;
        }
        throw new IllegalStateException("Failed while getting source griddable for terminus - This terminus couldn't provide a Griddable source from any of its ancillaries!");
    }
}
