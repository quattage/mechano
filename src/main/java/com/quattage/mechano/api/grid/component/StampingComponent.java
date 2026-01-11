package com.quattage.mechano.api.grid.component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.solver.MNAIndexer;
import com.quattage.mechano.api.grid.topology.Circuit;
import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.api.grid.topology.vertex.Terminal;

/**
 * A {@link DiscreteComponent} which contains an array of connected {@link Terminal terminals}
 * and can stamp to the {@link ServerGrid grid}. This class can be tracked by the {@link NodalIndexer}
 */
public abstract class StampingComponent extends DiscreteComponent {

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
                if(terminal.getParentConstruct() instanceof StampingComponent stamper)
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
        if(terminals.length <= 0) {
            throw new NullPointerException("Error processing StampingComponent '" 
                + getComponentID() + " - This component's terminal array is empty!");
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
    public void updateOwnership(GridConstruct parent, int index) {
        super.updateOwnership(parent, index);
        for(int x = 0; x < terminals.length; x++) {
            Terminal t = terminals[x];
            t.updateOwnership(this, x);
        }
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
