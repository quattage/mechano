package com.quattage.mechano.api.grid.component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.grid.component.ComponentTracker.ComponentHierarchy;
import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.api.grid.topology.vertex.Terminal;
import com.quattage.mechano.foundation.numeric.Bifrucated64;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public interface CircuitComponent {

    /**
     * Throws errors if the provided string is null or blank.
     */
    static void assertValidID(String componentID) {
        if(componentID == null || componentID.isBlank())
            throw new IllegalArgumentException("Couldn't instantiate CircuitComponent from null or empty string!");
    }

    /**
     * Gets all terminals associated with this component.
     * (ex. a Diode would return a list of two members: [anode, cathode])
     * If this component is compositional (like a Circuit object), calls to
     * this method will need to construct a collection of terminals, which
     * may be rather expensive.
     * @return all terminals attached to this component
     */
    Collection<Terminal> getTerminals();
    void forEachNode(Consumer<Node> cons);

    default ComponentHierarchy getHierarchyType() {
        return ComponentHierarchy.COMPOSING_CIRCUIT;
    }

    /**
     * The arbitrary identifier for this component. This identifier 
     * doesn't need to be logically significant.
     * @return
     */
    default String getComponentID() {
        return getClass().getSimpleName();
    }

    /**
     * Used to determine wether or not this component is near a grounding node and as
     * such should be treated specially when merging/simulating
     * @return <code>true</code> if this component represents or is directly attached to
     * a ground source
     */
    boolean isGrounded();

    /**
     * Fills this component with energy, if this 
     * component is capable of storing any.
     */
    void saturate();

    /**
     * Returns this component to its arbitrary
     * default state.
     */
    void reset();

    /**
     * Gets the charge that this component is currently storing, if applicable.
     * Otherwise, this method returns <code>BifrucatedLong.ZERO</code>
     * @return amp-hours currently contained within this energy store
     */
    default Bifrucated64 getStoredCharge() { return Bifrucated64.ZERO.mutableCopy(); }

    /**
     * Collects all Joints associated with this CircuitComponent that match
     * the given filter. 
     * @param filter
     * @return A list containing all relevent joints. This list will never be
     * empty, but will instead return <cogetAllNodesMatching  */
    @Nullable default List<Node> getAllJointsMatching(Predicate<Node> filter) {
        Objects.requireNonNull(filter);
        List<Node> collected = new ObjectArrayList<>();
        this.forEachNode(joint -> {
            if(!filter.test(joint)) return;
            collected.add(joint);
        });
        return collected.isEmpty() ? null : collected;
    }
}