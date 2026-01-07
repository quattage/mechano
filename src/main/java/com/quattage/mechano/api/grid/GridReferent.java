package com.quattage.mechano.api.grid;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.grid.topology.Circuit;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.grid.topology.CircuitComponent.FunctionalComponent;
import com.quattage.mechano.api.grid.topology.ComponentLink;
import com.quattage.mechano.api.grid.topology.vertex.AncillaryNode;
import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.api.grid.topology.vertex.Terminal;
import com.quattage.mechano.foundation.tracking.GridIdentifiable;
import com.quattage.mechano.foundation.tracking.GridUUID;

import net.minecraft.util.StringRepresentable;

public enum GridReferent implements StringRepresentable {

    COMPONENT_LINK(2, null, ComponentLink.class),

    COMPOSING_CIRCUIT(0, null, Circuit.class, COMPONENT_LINK),

    EMITTER_NODE(3, (grid, circuit, address) -> {
        Node target = circuit.getNode(address.getBindingA());
        if(target != null) return target;
        grid.warn("Couldn't acquire component from " + address + " - Query located subcomponent " + target 
            + ", but this component can't conform to the expected type '" + address.getReferentType() + "'");
        return null;
    }, Node.class, COMPOSING_CIRCUIT),

    ANCILLARY_NODE(4, (grid, circuit, address) -> {
        CircuitComponent target = circuit.getNodes().get(address.getBindingA());
        if(target instanceof AncillaryNode ancillary) {
            if(address.getBindingB() != -1) {
                grid.warn("Acquired CircuitComponent, but " + address + " has extraneous bindings that were ignored.");
                address.withBinding(address.getReferentType(), address.getBindingA(), -1);
            }
            return ancillary;
        }
        if(!(target instanceof Node node)) {
            grid.warn("Couldn't acquire component from " + address + " - Query located subcomponent " + target 
                + ", but this component can't conform to the expected type '" + GridReferent.EMITTER_NODE + "'");
            return null;
        }
        if(!node.hasAncillaries()) {
            grid.warn("Couldn't acquire component from " + address 
                + " - Query located " + node + ", but this node contains no ancillaries.");
            return null;
        }
        List<AncillaryNode> ancillaries = node.getAncillaries();
        int index = address.getBindingB();
        if(index < 0 || index >= ancillaries.size()) {
            grid.warn("Couldn't acquire component from " + address 
                + " - Query located " + node + ", but the index " + index + " is out of range for an ancillary list of " + ancillaries.size() + " members.");
            return null;
        }
        AncillaryNode output = ancillaries.get(index);
        if(output == null) {
            grid.warn("Couldn't acquire component from " + address 
                + " - Query located " + node + ", the mapping at index " + index + " is null.");
            return null;
        }
        return output;
    }, AncillaryNode.class, EMITTER_NODE),

    FUNCTIONAL_COMPONENT(1, (grid, circuit, address) -> {
        FunctionalComponent target = circuit.getComponent(address.getBindingA());
        if(target != null) return target;
        grid.warn("Couldn't acquire component from " + address + " - Query located subcomponent " + target 
            + ", but this component can't conform to the expected type '" + address.getReferentType() + "'");
        return null;
    }, FunctionalComponent.class, COMPOSING_CIRCUIT, COMPONENT_LINK),

    TERMINAL(5, null, Terminal.class, FUNCTIONAL_COMPONENT);

    private final byte mergePriority;
    private final TriFunction<Grid, Circuit, GridUUID, CircuitComponent> finderFunction;
    private final Class<? extends CircuitComponent> typeClass;
    private final GridReferent[] parents;

    GridReferent(int mergePriority, TriFunction<Grid, Circuit, GridUUID, CircuitComponent> finderFunction, Class<? extends CircuitComponent> typeClass) {
        this.mergePriority = (byte)mergePriority;
        this.finderFunction = finderFunction;
        this.typeClass = typeClass;
        this.parents = new GridReferent[0];
    }

    GridReferent(int mergePriority, TriFunction<Grid, Circuit, GridUUID, CircuitComponent> finderFunction, Class<? extends CircuitComponent> typeClass, GridReferent... parents) {
        this.mergePriority = (byte)mergePriority;
        this.finderFunction = finderFunction;
        this.typeClass = typeClass;
        this.parents = parents;
    }

    public static void throwIfMismatch(GridIdentifiable<?> obj, GridReferent expectedType) {
        if(obj == null) throw new NullPointerException("Couldn't preform operation on GridIdentifiable - The provided object is null!");
        if(expectedType == null) throw new NullPointerException("Couldn't preform operation on GridIdentifiable - The expected referent type is null!");
        GridReferent.throwIfMismatch(obj.getUUID(), expectedType);
    }

    public static void throwIfMismatch(GridUUID id, GridReferent expectedType) {
        if(id == null) throw new NullPointerException("Couldn't preform operation on GridUUID - The provided ID is null or none could be acquired!");
        if(expectedType == null) throw new NullPointerException("Couldn't preform operation on GridUUID - The expected referent type is null!");
        if(id.getReferentType() == expectedType) return;
        throw new UnexpectedReferentException(id, expectedType);
    }

    public static void throwIfMismatch(CircuitComponent component, GridReferent expectedType) {
        if(component == null) throw new NullPointerException("Couldn't preform operation on CircuitComponent - The provided ID is null or none could be acquired!");
        if(expectedType == null) throw new NullPointerException("Couldn't preform operation on CircuitComponent - The expected referent type is null!");
        if(component.getReferentType() == expectedType) return;
        throw new UnexpectedReferentException(component, expectedType);
    }
    
    public Class<? extends CircuitComponent> getTypeClass() {
        return typeClass;
    }

    public @Nullable CircuitComponent findTarget(Grid grid, Circuit circuit, GridUUID address) {
        Objects.requireNonNull(grid);
        Objects.requireNonNull(circuit);
        Objects.requireNonNull(address);
        if(finderFunction == null) throw new ComponentUnqueryableException(address);
        return finderFunction.apply(grid, circuit, address);
    }
    
    public boolean canBeOwnedBy(GridReferent type) {
        if(parents.length < 0) return false;
        for(int x = 0; x < parents.length; x++)
            if(parents[x] == type) return true;
        return false;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return getSerializedName();
    }

    public int getMergePriority() {
        return mergePriority;
    }

    public interface SourceIdentifier {

        /**
         * Provides access to the instantiator/composer source object
         * that created <code>obj</code>. Allows grid-scoped
         * code to access the {@link Griddable} (and, by extension, the world)
         * at arbitrary moments in time. <p>
         * Implementations may <code>null</code> to indicate that their source couldn't be found
         * or that <code>obj</code> is not an instance of the {@link SourceIdentifier} interface.
         * @return The {@link Griddable} that this object belongs to
         */
        static @Nullable Griddable<?> getSourceFor(Object obj) {
            return obj instanceof SourceIdentifier si ? si.getSource() : null;
        }

        /**
         * Provides access to the instantiator/composer source object
         * that created this GridAPI component. Allows grid-scoped
         * code to access the {@link Griddable} (and, by extension, the world)
         * at arbitrary moments in time. <p>
         * Implementations may <code>null</code> to indicate that their source couldn't be found.
         * @return The {@link Griddable} that this object belongs to
         */
        @Nullable Griddable<?> getSource();
    }

    public static class ComponentHierarchyInvalidException extends RuntimeException {
        public ComponentHierarchyInvalidException(CircuitComponent root) {
            super("Component of type '" + root.getClass().getSimpleName() + "' cannot be owned by itself.");
        }
        public ComponentHierarchyInvalidException(CircuitComponent root, CircuitComponent parent) {
            super("Component of type '" + root.getClass().getSimpleName() + "' cannot be owned by '" + parent.getClass().getSimpleName() + "'");
        }
        public ComponentHierarchyInvalidException(CircuitComponent root, CircuitComponent parent, String message) {
            super("Component of type '" + root.getClass().getSimpleName() + "' cannot be owned by '" + parent.getClass().getSimpleName() + "' - (" + message + ")");
        }
    }

    public static class ComponentUnqueryableException extends RuntimeException {
        public ComponentUnqueryableException(GridUUID address) {
            super("Component of type '" + address.getReferentType() + "' cannot be queried!");
        }
    }

    public static class UnexpectedReferentException extends RuntimeException {
        public UnexpectedReferentException(GridUUID address, GridReferent expected) {
            super("Got " + address + ", but operation expected the referent '" + expected + "'");
        }
        public UnexpectedReferentException(CircuitComponent component, GridReferent expected) {
            super("Got '" + component.getComponentID() + "' (of type '" + component.getReferentType() + "'), but operation expected the referent '" + expected + "'");
        }
    }
}

    
