package com.quattage.mechano.api.grid.component;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.GridUUID.UUIDComposite;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.HierarchicalConstruct;
import com.quattage.mechano.api.grid.HierarchicalConstruct.TerminalProvider;
import com.quattage.mechano.api.grid.topology.MNAIndexer;
import com.quattage.mechano.api.grid.topology.landmark.Node;
import com.quattage.mechano.api.grid.topology.landmark.Terminal;

/**
 * A {@link DiscreteComponent} which contains an array of connected {@link Terminal terminals}
 * and can stamp to the {@link ServerGrid grid}. This class can be tracked by the {@link NodalIndexer}
 */
public abstract class StampingComponent extends DiscreteComponent implements TerminalProvider {

    protected Terminal[] terminals;

    public StampingComponent(String componentID) {
        super(componentID);
        this.terminals = defineTerminals();
        assertHasTerminals();
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

    /**
     * A helper method used to get the index of the provided terminal's
     * attached node from the active indexer attached to the provided grid
     * @param grid to operate within
     * @param terminal to get the index of
     * @return int, -1 if the node is grounded or unindexed.
     */
    public int indexOf(ServerGrid grid, Terminal terminal) {
        Objects.requireNonNull(grid);
        Objects.requireNonNull(terminal);
        Node n = terminal.getAttachedNode();
        if(n == null || n.isGrounded()) return -1;
        return grid.indexer().indexOf(n);
    }

    /**
     * A helper method to quickly get the voltage present at a terminal's
     * attached node. This method is not guaranteed to be accurate
     * or up-to-date, you must be aware of the timings of the grid's
     * update cycle in order for this method to be relevent.
     * This method is intended to be used during 
     * {@link StampsDynamically#stampDynamic dynamic stamping} 
     * to update time-varied values in individual components.
     * @param grid
     * @param terminal
     * @return double value representing voltage 
     */
    public double voltageOf(ServerGrid grid, Terminal terminal) {
        Objects.requireNonNull(grid);
        Objects.requireNonNull(terminal);
        Node n = terminal.getAttachedNode();
        if(n == null) {
        }
        
        // TODO impl
        return 0d;
    }

    @Override
    public @Nullable CircuitComponent getComponent(UUIDComposite binding) {
        return terminals[binding.get()];
    }

    @Override
    public void forEachNode(Consumer<Node> cons) {
        for(int x = 0; x < terminals.length; x++)
            terminals[x].forEachNode(cons);
    }

    @Override
    public Terminal[] getTerminals() {
        return terminals;
    }

    @Override
    public boolean isGrounded() {
        return hasGroundedTerminal();
    }

    @Override
    public void updateOwnership(Griddable<?> source, HierarchicalConstruct parent) {
        super.updateOwnership(source, parent);
        for(int x = 0; x < terminals.length; x++) {
            Terminal t = terminals[x];
            t.updateOwnership(source, this);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        forEachTerminal(Terminal::dispose);
        terminals = null;
    }

    @Override
    public boolean hasBeenDisposed() {
        return terminals == null;
    }

    @Override
    public int indexOfChild(HierarchicalConstruct child) {
        if(!(child instanceof Terminal) || terminals == null || terminals.length <= 0) 
            return -1;
        for(int x = 0; x < terminals.length; x++)
            if(terminals[x] == child) return x;
        return -1;
    }

    /**
     * "Stamping" refers to the process of an individual CircuitComponent
     * declaring its own presence in the NodalSnapshot. This method
     * stamps the topological impact of this component onto the current
     * snapshot. This method is only run when the grid's topology changes.
     * <p> If your particular component requires a continuously ticking update,
     * implement {@link StampsDynamically this interface}
     */
    public abstract void stamp(ServerGrid grid);

    @Override
    public void MNAAllocate(ServerGrid grid) {
        grid.indexer().add(this);
    }

    @Override
    public void MNADeallocate(ServerGrid grid) {
        grid.indexer().remove(this);
    }

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
    @Nullable public Terminal anode() { return pinA(); }

    /**
     * A helper method specific to some functional components to quickly
     * get a terminal of a certain type, or <code>null</code> if this particular
     * component does not contain said terminal.
     * @return The cathode terminal, or null if one doesn't exist here.
     */
    @Nullable public Terminal cathode() { return pinB(); }

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

    /**
     * Indicates that implementing {@link StampingComponent stampers}
     * allocate additional doubles in the {@link MNAIndexer} or need 
     * to resolve some time-varied value each tick. This interface is 
     * not needed for simple components (e.g. resistors) that aren't 
     * current-dependent.
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
         * stamps the time-varied values of this component for the currently
         * active solver step. This method is run continuously as the grid resolves,
         * once per server tick.
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
