package com.quattage.mechano.api.grid.topology;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.grid.CircuitFactory;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.foundation.numeric.Bifrucated64;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

public interface CircuitComponent extends StringRepresentable {

    static void checkID(String componentID) {
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
    void forEachJoint(Consumer<Node> cons);

    /**
     * The arbitrary identifier for this component. This identifier 
     * doesn't need to be logically significant.
     * @return
     */
    String getComponentID();

    /**
     * @return
     */
    String describeState();

    /**
     * Useful for translation keys or textures.
     * @return This Components {@link #getComponentID() id} as a {@link ResourceLocation}
     */
    ResourceLocation asResource();

    /**
     * Used to determine whether or not this component is participating in
     * whatever circuit/instantiator it is a part of, if applicable.
     * This method returns <code>false</code> in cases where this component 
     * hasn't been properly initialized or isn't attached to anything. 
     * The {@link Circuit} automatically {@link Circuit#trim trims} 
     * components and joints that are insignificant. Insignificant
     * components being trimmed out of fresh {@link Griddable griddables}
     * indicates misuse of the {@link CircuitFactory} during initialization.
     * @return
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

    CircuitComponent.Type getType();

    default void updateOwnership(CircuitComponent parent, int index) { updateOwnership(null, parent, index); }
    void updateOwnership(@Nullable Griddable source, CircuitComponent parent, int index);


    /**
     * Used to enforce a parent/child relationship for components and the 
     * circuits they belong to. CircuitComponent implementations which
     * require {@link Terminal terminals} may derive their parent
     * component from the {@link Node} at <code>terminals[0]</code>
     * @return The CircuitComponent instance that currently owns this one, 
     * or <code>null</code> if this component has no parent.
     */
    @Nullable CircuitComponent getParentComponent();

    @Override
    default String getSerializedName() {
        return getComponentID().toLowerCase(Locale.ROOT);
    }

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
     * empty, but will instead return <code>null</code>.
     */
    @Nullable default List<Node> getAllJointsMatching(Predicate<Node> filter) {
        Objects.requireNonNull(filter);
        if(!this.isSignificant()) return null;
        List<Node> collected = new ObjectArrayList<>();
        this.forEachJoint(joint -> {
            if(!filter.test(joint)) return;
            collected.add(joint);
        });
        return collected.isEmpty() ? null : collected;
    }

    default boolean assertCanBeOwnedBy(CircuitComponent other) {
        if(this == other) throw new IllegalArgumentException("Cannot add component " + this + " - This component cannot be parented to itself!");
        if(this instanceof Circuit) throw new IllegalArgumentException("Cannot add component '" + this + "' to '" + other + " - These components are incompatible!");
        return true;
    }




    public abstract class FunctionalComponent implements CircuitComponent {

        private final String componentID;
        private CircuitComponent parent;

        public FunctionalComponent(String componentID) {
            CircuitComponent.checkID(componentID);
            this.componentID = componentID;
        }

        @Override public final String getComponentID() { return componentID; }
        @Override public String describeState() { return "No state descriptor"; }
        @Override public ResourceLocation asResource() { return Mechano.asResource(getSerializedName().replace(" ", "_")); }
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
            return componentID + "[" + describeState() + "]@" + hashCode();
        }

        @Override
        public void updateOwnership(@Nullable Griddable source, CircuitComponent parent, int index) {
            this.parent = parent;
        }

        @Override
        public @Nullable CircuitComponent getParentComponent() {
            return parent;
        }

        
        @Override
        public CircuitComponent.Type getType() {
            return CircuitComponent.Type.FUNCTIONAL_COMPONENT;
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


    public enum Type implements StringRepresentable {
        CIRCUIT,
        EMITTER_NODE,
        ANCILLARY_NODE,
        TERMINAL,
        FUNCTIONAL_COMPONENT,
        TRANSMITTER;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        @Override
        public String toString() {
            return getSerializedName();
        }
    }
}
