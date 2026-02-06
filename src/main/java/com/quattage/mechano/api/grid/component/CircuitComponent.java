package com.quattage.mechano.api.grid.component;

import java.util.function.Consumer;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.topology.MNAIndexer;
import com.quattage.mechano.api.grid.topology.landmark.Node;
import com.quattage.mechano.foundation.numeric.Bifrucated64;

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
     * Allocates space in the {@link MNAIndexer} belonging to the
     * given {@link ServerGrid} to store this component and/or any 
     * sub-components, so long as this object or its compositional 
     * children inherit from the {@link StampingComponent}
     * interface. Implementations must make at least one call to 
     * {@link MNAIndexer#add}. If this object does not push any 
     * changes to the grid as a result of this call, a warning will 
     * be printed to the console.
     * @param grid to pull the {@link MNAIndexer indexer} from
     * @see #MNADeallocate
     */
    default void MNAAllocate(ServerGrid grid) {
        grid.warn("Attempted to allocate " + this + " but this component doesn't have any allocation implementation.");
    }

    /**
     * Removes any existing mappings (to this component and/or any 
     * sub-components) from the given {@link ServerGrid}'s 
     * {@link MNAIndexer}.
     * If this object does not push any 
     * changes to the grid as a result of this call, a warning will 
     * be printed to the console.
     * @param grid to pull the {@link MNAIndexer indexer} from
     * @see #MNAAllocate
     */
    default void MNADeallocate(ServerGrid grid) {
        grid.warn("Attempted to de-allocate " + this + " but this component doesn't have any allocation implementation.");
    }

    /**
     * Gets the charge that this component is currently storing, if applicable.
     * Otherwise, this method returns <code>BifrucatedLong.ZERO</code>
     * @return amp-hours currently contained within this energy store
     */
    default Bifrucated64 getStoredCharge() { return Bifrucated64.ZERO.mutableCopy(); }
}