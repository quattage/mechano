package com.quattage.mechano.api.grid.topology;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.CircuitFactory;
import com.quattage.mechano.api.grid.GridReferent;
import com.quattage.mechano.api.grid.GridReferent.ComponentHierarchyInvalidException;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.solver.MNAIndexer;
import com.quattage.mechano.api.grid.topology.vertex.AncillaryNode;
import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.api.grid.topology.vertex.Terminal;
import com.quattage.mechano.api.transmitter.TransmitterType.UnionFactory;
import com.quattage.mechano.foundation.numeric.Bifrucated64;
import com.quattage.mechano.foundation.tracking.GridUUID;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public interface CircuitComponent {

    /**
     * A shorthanded {@link UnionFactory} substitute for unions that represent
     * perfect conductors. (e.g. a wire with no resistence.)
     * @param grid Grid to operate within
     * @param startAncillary {@link AncillaryNode} starting point
     * @param endAncillary {@link AncillaryNode} ending point
     * @return <code>null,</code> since a perfect union doesn't have a component associated with it.
     */
    static CircuitComponent perfectConductor(ServerGrid grid, AncillaryNode startAncillary, AncillaryNode endAncillary) {
        grid.getNetlist().union(startAncillary.getParentNode(), endAncillary.getParentNode());
        return null;
    }

    /**
     * Throws errors if the provided string is null or blank.
     */
    static void assertValidID(String componentID) {
        if(componentID == null || componentID.isBlank())
            throw new IllegalArgumentException("Couldn't instantiate CircuitComponent from null or empty string!");
    }

    /**
     * Throws exceptions if component <code>child</code> cannot be parented to <code>parent</code>
     */
    static void assertValidOwnership(CircuitComponent child, CircuitComponent parent) {
        if(child == null) throw new NullPointerException("child component is null!");
        if(parent == null) throw new NullPointerException("parent component is null!");
        if(child == parent) throw new ComponentHierarchyInvalidException(child);
        if(child.getReferentType() == null) throw new NullPointerException("child component returned a null type!");
        if(parent.getReferentType() == null) throw new NullPointerException("parent component returned a null type!");
        if(!child.getReferentType().canBeOwnedBy(parent.getReferentType())) throw new ComponentHierarchyInvalidException(child, parent);
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

    /**
     * The arbitrary identifier for this component. This identifier 
     * doesn't need to be logically significant.
     * @return
     */
    String getComponentID();

    /**
     * Used to determine whether or not this component is participating in
     * whatever circuit/instantiator it is a part of, if applicable.
     * This method returns <code>false</code> in cases where this component 
     * hasn't been properly initialized or isn't attached to anything. 
     * The {@link Circuit} automatically {@link Circuit#trim trims} 
     * components and joints that are insignificant. Insignificant
     * components being trimmed out of fresh {@link Griddable<?>griddables}
     * indicates misuse of the {@link CircuitFactory} during initialization.
     */
    boolean isSignificant();

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
     * @return The effective "size" of this component
     * is a loose approximation of how computationally
     * expensive it is to process.
     */
    int size();

    /**
     * Binds the given {@link GridUUID} to this particular component,
     * so that its sub-address information points towards this component.
     * Implementations use {@link GridUUID#withBinding} at some point in their
     * logic.
     * @param id GridUUID to modify
     * @return The modified GridUUID
     */
    GridUUID bindUUID(GridUUID id);

    GridReferent getReferentType();

    default void updateOwnership(CircuitComponent parent, int index) { updateOwnership(null, parent, index); }
    void updateOwnership(@Nullable Griddable<?> source, CircuitComponent parent, int index);

    /**
     * If this CircuitComponent represents the functional element of a {@link ComponentLink},
     * that link will be returned here.
     * @return The {@link ComponentLink} that owns this CircuitComponent, or <code>null</code> if this
     * CircuitComponent doesn't belong to a link.
     * @see #isLink
     */
    default ComponentLink<?> getLink() {
        return getParentComponent() instanceof ComponentLink<?> cl ? cl : null;
    }

    /**
     * Gets the parent of this CircuitComponent, traversing
     * the parent/child hierarchy upwards until it finds the superparent
     * @return The superparent, or the CircuitComponent with no parent, 
     */
    default @Nullable CircuitComponent traverseUpwards() {
        CircuitComponent superparent = getParentComponent();
        if(superparent == null) return null;
        for(int x = 0; x < 255; x++) {
            CircuitComponent candidate = superparent.getParentComponent();
            if(candidate == null) return superparent;
            superparent = candidate;
        }
        Mechano.LOGGER.warn("Component hierarchy traversal for " + this + " failed to identify a superparent.");
        return null;
    }

    /**
     * Used to enforce a parent/child relationship for components and the 
     * circuits they belong to. CircuitComponent implementations which
     * require {@link Terminal terminals} may derive their parent
     * component from the {@link Node} at <code>terminals[0]</code>
     * @return The CircuitComponent instance that currently owns this one, 
     * or <code>null</code> if this component has no parent.
     */
    @Nullable CircuitComponent getParentComponent();

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
        if(!this.isSignificant()) return null;
        List<Node> collected = new ObjectArrayList<>();
        this.forEachNode(joint -> {
            if(!filter.test(joint)) return;
            collected.add(joint);
        });
        return collected.isEmpty() ? null : collected;
    }

    public abstract class FunctionalComponent implements CircuitComponent {

        private final String componentID;
        private CircuitComponent parent;
        private int nodalIndex;

        public FunctionalComponent(String componentID) {
            CircuitComponent.assertValidID(componentID);
            this.componentID = componentID;
        }

        @Override public final String getComponentID() { return componentID; }
        @Override public boolean isGrounded() { return false; }
        @Override public void saturate() {}
        @Override public void reset() {}
        @Override public int size() { return 1; }
        
        @Override public boolean isSignificant() { 
            if(getTerminals() == null || getTerminals().isEmpty()) return false;
            for(Terminal t : getTerminals()) {
                if(t != null && t.isSignificant()) return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return componentID;
        }

        @Override
        public void updateOwnership(@Nullable Griddable<?> source, CircuitComponent parent, int index) {
            CircuitComponent.assertValidOwnership(this, parent);
            this.parent = parent;
            this.nodalIndex = index;
        }

        @Override
        public @Nullable CircuitComponent getParentComponent() {
            return parent;
        }

        @Override
        public GridReferent getReferentType() {
            return GridReferent.FUNCTIONAL_COMPONENT;
        }

        @Override
        public GridUUID bindUUID(GridUUID id) {
            return id.withBinding(getReferentType(), nodalIndex);
        }
    }

    /**
     * Indicates that implementing subclasses stamp conductance
     * and source terms to the NodalSnapshot.
     */
    public abstract class StampingComponent extends FunctionalComponent {

        protected Terminal[] terminals;

        public StampingComponent(String componentID) {
            super(componentID);
            this.terminals = defineTerminals();
            assertHasTerminals();
        }

        public static void asStamperDo(@Nullable Grid grid, CircuitComponent component, Consumer<StampingComponent> cons) {
        switch(component) {
            case StampingComponent stamper -> cons.accept(stamper);
            case Node n -> n.forEachTerminal(terminal -> { 
                if(terminal.getParentComponent() instanceof StampingComponent stamper)
                    cons.accept(stamper);
            });
            case Circuit circuit -> circuit.forEachComponent(comp -> {
                if(comp instanceof StampingComponent stamper)
                    cons.accept(stamper);
            });
            case null, default -> { if(grid != null) grid.warn("Skipped semantic execution for " + component 
                + " - This component couldn't be paired down to a stamper!"); }
        }
    }

        public StampingComponent(String componentID, Terminal[] terminals) {
            super(componentID);
            if(terminals == null || terminals.length <= 0) {
                throw new IllegalArgumentException("Couldn't instantiate StampingComponent '" 
                    + componentID + "' with a null or empty terminals array!");
            }
            this.terminals = terminals;
            assertHasTerminals();
        }

        public StampingComponent(String componentID, Collection<Terminal> terminals) {
            super(componentID);
            if(terminals == null || terminals.isEmpty()) {
                throw new IllegalArgumentException("Couldn't instantiate StampingComponent '" 
                    + componentID + "' with a null or empty terminals collection!");
            }
            this.terminals = terminals.toArray(new Terminal[terminals.size()]);
            assertHasTerminals();
        }

        protected abstract Terminal[] defineTerminals();

        private void assertHasTerminals() {
            if(this.terminals == null) {
                throw new NullPointerException("Error processing StampingComponent '" 
                    + getComponentID() + " - This component's terminal array is null!");
            }
            for(int x = 0; x < terminals.length; x++) {
                if(terminals[x] == null) {
                    throw new NullPointerException("Error processing StampingComponent '" 
                        + getComponentID() + "' - Terminal at index " + x + " is null!");
                }
            }
        }

        @Override
        public void forEachNode(Consumer<Node> cons) {
            for(int x = 0; x < terminals.length; x++)
                terminals[x].forEachNode(cons);
        }

        @Override
        public Collection<Terminal> getTerminals() {
            if(terminals.length == 1) return Collections.singleton(terminals[0]);
            return Arrays.asList(terminals);
        }

        @Override
        public boolean isGrounded() {
            for(int x = 0; x < terminals.length; x++)
                if(terminals[x].isGrounded()) return true;
            return false;
        }

        @Override
        public int size() {
            return terminals.length;
        }

        /**
         * "Stamping" refers to the process of an individual CircuitComponent
         * declaring its own presence in the NodalSnapshot. This method
         * stamps the topological impact of this component onto the current
         * snapshot. This method is only run when the grid's topology changes.
         * @see #stampDynamic
         */
        public abstract void stamp(ServerGrid grid);

        /**
         * @return <code>true</code> if the grid should call {@link #postProcess}
         * on this element after solving.
         */
        public boolean requiresPostProcessing() { return false; }

        /**
         * Called after the stamping process is complete and the matrix
         * has been resolved for this tick. At the time of invocation,
         * all other stampers have settled, so this is where you
         * @param grid The grid to operate within
         * @param current The current (in amps) that exists at this component
         */
        public void postProcess(ServerGrid grid, double current) {}

        /**
         * A helper method specific to some functional components to quickly
         * get a terminal of a certain type, or <code>null</code> if this particular
         * component does not contain said terminal.
         * @return The pin "A" (arbitrary for when polarity doesn't matter) or null if one doesn't exist here.
         */
        @Nullable public Terminal pinA() { return null; }

        /**
         * A helper method specific to some functional components to quickly
         * get a terminal of a certain type, or <code>null</code> if this particular
         * component does not contain said terminal.
         * @return The pin "B" (arbitrary for when polarity doesn't matter) or null if one doesn't exist here.
         */
        @Nullable public Terminal pinB() { return null; }

        /**
         * A helper method specific to some functional components to quickly
         * get a terminal of a certain type, or <code>null</code> if this particular
         * component does not contain said terminal.
         * @return The anode terminal, or null if one doesn't exist here.
         */
        @Nullable public Terminal anode() { return null; }
        /**
         * A helper method specific to some functional components to quickly
         * get a terminal of a certain type, or <code>null</code> if this particular
         * component does not contain said terminal.
         * @return The cathode terminal, or null if one doesn't exist here.
         */
        @Nullable public Terminal cathode() { return null; }
            /**
         * A helper method specific to some functional components to quickly
         * get a terminal of a certain type, or <code>null</code> if this particular
         * component does not contain said terminal.
         * @return The positive terminal, or null if one doesn't exist here.
         */
        @Nullable public Terminal positive() { return anode(); }
        /**
         * A helper method specific to some functional components to quickly
         * get a terminal of a certain type, or <code>null</code> if this particular
         * component does not contain said terminal.
         * @return The negative terminal, or null if one doesn't exist here.
         */
        @Nullable public Terminal negative() { return cathode(); }

        /**
         * A helper method specific to some functional components to quickly
         * get a terminal of a certain type, or <code>null</code> if this particular
         * component does not contain said terminal.
         * @return The source terminal, or null if one doesn't exist here. Only applies to FETs.
         */
        @Nullable public Terminal source() { return null; }
        /**
         * A helper method specific to some functional components to quickly
         * get a terminal of a certain type, or <code>null</code> if this particular
         * component does not contain said terminal.
         * @return The drain terminal, or null if one doesn't exist here. Only applies to FETs.
         */
        @Nullable public Terminal drain() { return null; }
        /**
         * A helper method specific to some functional components to quickly
         * get a terminal of a certain type, or <code>null</code> if this particular
         * component does not contain said terminal.
         * @return The gate terminal, or null if one doesn't exist here. Only applies to FETs.
         */
        @Nullable public Terminal gate() { return null; }

        /**
         * A helper method specific to some functional components to quickly
         * get a terminal of a certain type, or <code>null</code> if this particular
         * component does not contain said terminal.
         * @return The base terminal, or null if one doesn't exist here. Only applies to transistors.
         */
        @Nullable public Terminal base() { return null; }
        
        /**
         * A helper method specific to some functional components to quickly
         * get a terminal of a certain type, or <code>null</code> if this particular
         * component does not contain said terminal.
         * @return The emitter terminal, or null if one doesn't exist here. Only applies to transistors.
         */
        @Nullable public Terminal emitter() { return null; }

        /**
         * A helper method specific to some functional components to quickly
         * get a terminal of a certain type, or <code>null</code> if this particular
         * component does not contain said terminal.
         * @return The collector terminal, or null if one doesn't exist here. Only applies to transistors.
         */
        @Nullable public Terminal collector() { return null; }
    }

    /**
     * Indicates that implementing {@link StampingComponent stampers}
     * allocate additional doubles in the {@link MNAIndexer}
     * This interface is not needed for simple components (e.g. resistors)
     * that aren't current-dependent.
     */
    public interface StampsDynamically {
        /**
         * This method is essentially a "score" for how complex
         * this component is when {@link StampingComponent#stampDynamic stamping dynamically}. <p>
         * @return The number of additional doubles to allocate in
         * the {@link MNAIndexer} to be flushed into the B vector.
         */
        int getAllocations();

        /**
         * "Stamping" refers to the process of an individual CircuitComponent
         * declaring its own presence in the NodalSnapshot. This method
         * stamps the time-varied values of this component for the current
         * solver step. This method is run continuously as the grid resolves.
         * @see #stamp
         */
        void stampDynamic(ServerGrid grid);
    }

    /**
     * Indicates that implementing {@link StampingComponent stampers}
     * require additional modification after all stamping has completed.
     */
    public interface NeedsPostProcessing {

        /**
         * Called after the stamping process is complete and the matrix
         * has been resolved for this tick. At the time of invocation,
         * all other stampers have settled, so this is where you
         * @param grid The grid to operate within
         * @param current The current (in amps) that exists at this component
         */
        void postProcess(ServerGrid grid);
    }

    @FunctionalInterface
    public interface CurrentChangeCallback {
        /**
         * Called whenever the current in a given component is updated by the grid.
         * @param grid Grid to operate within
         * @param ampsThen The current (in amps) that this source used to have
         * @param ampsNow The current (in amps) that this source has now
         */
        void onCurrentUpdated(ServerGrid grid, double ampsThen, double ampsNow);
    }
}