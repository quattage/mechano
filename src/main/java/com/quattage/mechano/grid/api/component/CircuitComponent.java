package com.quattage.mechano.grid.api.component;

import java.util.function.Consumer;

import com.quattage.mechano.api.GridDomain;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.foundation.numeric.Bifrucated64;
import com.quattage.mechano.grid.topology.NetlistIndexer;
import com.quattage.mechano.grid.topology.Node;
import com.quattage.mechano.grid.topology.NodeUnionSet;

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

    int getDomainIndex();

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
     * Allocates space in the {@link NetlistIndexer} belonging to the
     * given {@link ServerGrid} to store this component and/or any 
     * sub-components, so long as this object or its compositional 
     * children inherit from the {@link StampingComponent}
     * interface. Implementations must make at least one call to 
     * {@link NetlistIndexer#add}. If this object does not push any 
     * changes to the grid as a result of this call, a warning will 
     * be printed to the console.
     * @param domain to pull the {@link NetlistIndexer indexer} from
     * @see #MNADeallocate
     */
    default void MNAAllocate(GridDomain domain) {
        domain.getHostGrid().warn("Attempted to allocate '" + this + "' (" + this.getClass().getSimpleName() + ") but this component doesn't have any allocation implementation.");
    }

    /**
     * Removes any existing mappings (to this component and/or any 
     * sub-components) from the given {@link ServerGrid}'s 
     * {@link NetlistIndexer}.
     * If this object does not push any 
     * changes to the grid as a result of this call, a warning will 
     * be printed to the console.
     * @param domain to pull the {@link NetlistIndexer indexer} from
     * @see #MNAAllocate
     */
    default void MNADeallocate(GridDomain domain) {
        domain.getHostGrid().warn("Attempted to de-allocate '" + this + "' (" + this.getClass().getSimpleName() + ") but this component doesn't have any allocation implementation.");
    }
}