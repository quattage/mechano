package com.quattage.mechano.grid.topology.core;

import java.util.function.Consumer;

import com.quattage.mechano.foundation.numeric.Bifrucated64;
import com.quattage.mechano.grid.topology.Circuit;

public interface CircuitComponent {

    String[] RESERVED_KEYWORDS = new String[] {
        "ground",
        "node",
        "empty",
        "disposed"
    };

    /**
     * Throws errors if the provided string is null, blank, or contains
     * keywords that shouldn't be used.
     */
    static void assertValidID(String componentID) {
        if(componentID == null || componentID.isBlank())
            throw new IllegalArgumentException("Couldn't instantiate CircuitComponent from null or empty string!");
        for(int x = 0; x < CircuitComponent.RESERVED_KEYWORDS.length; x++) {
            String kw = CircuitComponent.RESERVED_KEYWORDS[x];
            if(componentID.contains(kw)) {
                throw new IllegalArgumentException("Couldn't initialize CircuitCompoonent with id '" + componentID 
                    + "' - This CircuitComponent contains the reserved keyword '" + kw + "'!");
            }
        }
    }

    /**
     * The amount of {@link Node nodes} in this {@link CircuitComponent}.
     * This call, depending on the component's implementation, likely 
     * requires iteration to count the nodes individually. The number
     * returned by this method serves as a decent indicator of this
     * component's relative complexity. More nodes exponentially
     * increases the memory footprint of this CircuitComponent, as 
     * each node is held in multiple acceleration structures including
     * the {@link NodeUnionSet netlist}, the {@link NetlistIndexer indexer},
     * and the {@link Circuit circuit} itself.
     * @return a positive integer cooresponding to the amount of nodes 
     * being hosted by or contained within this CircuitComponent.
     */
    int nodeCount();

    void forEachNode(Consumer<Node> cons);

    /**
     * The arbitrary identifier for this component. This identifier 
     * doesn't need to be logically significant.
     * @return
     */
    default String getComponentID() {
        return getClass().getSimpleName();
    }

    /**
     * Used to determine whether or not this component connected to or directly represents
     * a reference node and as such should be treated specially when merging/simulating
     * @return <code>true</code> if this component represents or is directly attached to
     * a ground/reference source
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
}