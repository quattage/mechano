package com.quattage.mechano.api.grid;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.grid.topology.Circuit;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.grid.topology.CircuitComponent.FunctionalComponent;
import com.quattage.mechano.api.grid.topology.ComponentLink;
import com.quattage.mechano.api.grid.topology.vertex.AncillaryNode;
import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.api.grid.topology.vertex.Terminal;
import com.quattage.mechano.foundation.tracking.GridUUID;

import net.minecraft.util.StringRepresentable;

public enum GridHierarchy implements StringRepresentable {


    COMPONENT_LINK(null, ComponentLink.class),

    COMPOSING_CIRCUIT(null, Circuit.class, COMPONENT_LINK),

    EMITTER_NODE((grid, circuit, address) -> {
        Node target = circuit.getNode(address.getBindingA());
        if(target != null) return target;
        grid.warn("Couldn't acquire component from " + address + " - Query located subcomponent " + target 
            + ", but this component can't conform to the expected type '" + address.getType() + "'");
        return null;
    }, Node.class, COMPOSING_CIRCUIT),

    ANCILLARY_NODE((grid, circuit, address) -> {
        CircuitComponent target = circuit.getNodes().get(address.getBindingA());
        if(target instanceof AncillaryNode ancillary) {
            if(address.getBindingB() != -1) {
                grid.warn("Acquired CircuitComponent, but " + address + " has extraneous bindings that were ignored.");
                address.withBinding(address.getType(), address.getBindingA(), -1);
            }
            return ancillary;
        }
        if(!(target instanceof Node node)) {
            grid.warn("Couldn't acquire component from " + address + " - Query located subcomponent " + target 
                + ", but this component can't conform to the expected type '" + GridHierarchy.EMITTER_NODE + "'");
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

    FUNCTIONAL_COMPONENT((grid, circuit, address) -> {
        FunctionalComponent target = circuit.getComponent(address.getBindingA());
        if(target != null) return target;
        grid.warn("Couldn't acquire component from " + address + " - Query located subcomponent " + target 
            + ", but this component can't conform to the expected type '" + address.getType() + "'");
        return null;
    }, FunctionalComponent.class, COMPOSING_CIRCUIT, COMPONENT_LINK),

    TERMINAL(null, Terminal.class, FUNCTIONAL_COMPONENT);

    private final TriFunction<Grid, Circuit, GridUUID, CircuitComponent> finderFunction;
    private final Class<? extends CircuitComponent> typeClass;
    private final GridHierarchy[] parents;

    GridHierarchy(TriFunction<Grid, Circuit, GridUUID, CircuitComponent> finderFunction, Class<? extends CircuitComponent> typeClass) {
        this.finderFunction = finderFunction;
        this.typeClass = typeClass;
        this.parents = new GridHierarchy[0];
    }

    GridHierarchy(TriFunction<Grid, Circuit, GridUUID, CircuitComponent> finderFunction, Class<? extends CircuitComponent> typeClass, GridHierarchy... parents) {
        this.finderFunction = finderFunction;
        this.typeClass = typeClass;
        this.parents = parents;
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
    
    public boolean canBeOwnedBy(GridHierarchy type) {
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

    public interface SourceIdentifier {
        /**
         * Provides access to the instantiator/composer source object
         * that created this GridAPI component. Allows grid-scoped
         * code to access the {@link Griddable} (and, by extension, the world)
         * at arbitrary moments in time. <p>
         * Implementations will never return <code>null.</code>
         * @return The {@link Griddable} that this object belongs to
         */
        @NotNull Griddable<?>getSource();
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
            super("Component of type '" + address.getType() + "' cannot be queried!");
        }
    }
}

    
