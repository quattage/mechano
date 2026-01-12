package com.quattage.mechano.api.transmitter;

import java.util.Objects;

import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.component.GridConstruct;
import com.quattage.mechano.api.grid.solver.MNAIndexer;
import com.quattage.mechano.api.grid.topology.netlist.NodeUnionSet;
import com.quattage.mechano.api.grid.topology.vertex.AncillaryNode;
import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.api.switchboard.JackSelector;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class TransmitterType {
    
    private final String name;
    private final UnionFactory factory;
    private final NonNullSupplier<CatenaryRenderProperties> renderProperties;

    /**
     * Retrieves a {@link TransmitterType} from its associated {@link TransmitterEntry} 
     * contained within the {@link BuiltInRegistries#REGISTRY static registry}.
     * This method will throw if no transmitter at <code>name</code> could be found.
     * @param name The ResourceLocation of the desired TransmitterType
     * @return The TransmitterType at <code>name</code>
     */
    public static TransmitterType getByName(ResourceLocation name) {
        TransmitterType trns = Mechano.REGISTRATE.getTransmitterRegistry().get(name); 
        if(trns == null) throw new IllegalArgumentException("Couldn't find TransmitterType entry at " + name);
        return trns;
    }

    public TransmitterType(@Nullable String name, UnionFactory factory, NonNullSupplier<CatenaryRenderProperties> renderProperties) {
        Objects.requireNonNull(factory);
        if(name == null || name.isBlank()) name = "unnamed";
        else name = name.toLowerCase();
        this.factory = factory;
        this.name = name;
        this.renderProperties = renderProperties;
    }

    public UnionFactory getFactory() {
        return factory;
    }

    @OnlyIn(Dist.CLIENT)
    public CatenaryRenderProperties getRenderProperties() {
        return renderProperties.get();
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "TransmitterType[" + name + "]";
    }


    public static class GridUnionException extends RuntimeException {
        public GridUnionException(@Nullable Object src, String message) {
            this(src == null ? "no_source_provided" : src.toString(), message);
        }
        public GridUnionException(String name, String message) {
            super("Failed to apply union operation for object '" + name + "' - " + message);
        }
    }

    public static @Nullable CircuitComponent applyUnion(ServerGrid grid, UnionFactory factory, @Nullable Object src, AncillaryNode<?> start, AncillaryNode<?> end) {
        if(grid == null) throw new GridUnionException(src, "Couldn't run factory on null grid!");
        if(start == null) throw new GridUnionException(src, "start is null!");
        if(end == null) throw new GridUnionException(src, "end is null!");
        CircuitComponent output = null;
        try { output = factory.apply(grid, start, end); }
        catch(RuntimeException e) { 
            if(e instanceof GridUnionException gue) throw gue;
            e.printStackTrace();
            throw new GridUnionException(src, "Encountered an error while applying factory! (See exception above)");
        }
        if(output != null && src instanceof GridConstruct parent && output instanceof GridConstruct child) 
            child.updateOwnership(parent);
        return output;
    }

    @FunctionalInterface
    public interface UnionFactory extends TriFunction<ServerGrid, AncillaryNode<?>, AncillaryNode<?>, CircuitComponent>{

        /**
         * A shorthanded {@link UnionFactory} substitute for unions that represent
         * perfect conductors. (e.g. a wire with no resistence.)
         * @param grid Grid to operate within
         * @param startAncillary {@link AncillaryNode} starting point
         * @param endAncillary {@link AncillaryNode} ending point
         * @return <code>null,</code> since a perfect union doesn't have a component associated with it.
         */
        static CircuitComponent perfectConductor(ServerGrid grid, AncillaryNode<?> startAncillary, AncillaryNode<?> endAncillary) {
            grid.getNetlist().union(startAncillary.getAssociatedNode(), endAncillary.getAssociatedNode());
            return null;
        }

        /**
         * Supply some logic here to create an arbitrary connection between two 
         * {@link AncillaryNode ancillaries} - <code>startAncillary</code> and
         * <code>endAncillary</code>. Within the scope of this function, you are permitted to make 
         * any changes to the {@link ServerGrid grid} you'd like, including 
         * {@link NodeUnionSet topological alterations} and {@link MNAIndexer stamper assertions}.
         * <h3>with great power comes great oh no i broke it</h3>
         * There are no guardrails here; you have direct access to the grid's topology! Be careful
         * not to perform destructive operations that destabilize the grid.
         * @param grid {@link ServerGrid} which provides access to common ground, the netlist, and the solver
         * @param startAncillary The starting point of the union that is being created
         * @param endAncillary The ending point of the union that is being created
         * @return Any {@link CircuitComponent} instance, or <code>null</code> if this union did not result 
         * in the creation of a discrete component.
         */
        @Override 
        @Nullable CircuitComponent apply(ServerGrid grid, AncillaryNode<?> startAncillary, AncillaryNode<?> endAncillary);
    }

    public interface TransmitterProvider {
        /**
         * Evalutaes the provided {@link Node} and returns a {@link GridAction response}
         * indicating whether or not the targeted node should be highlighted by the {@link JackSelector selector}
         * <p>
         * <h3>Remember to tag implementations with</h3> 
         * <pre>@OnlyIn(Dist.CLIENT)</pre>
         * @param world world to operate whithin
         * @param target the {@link AncillaryNode} currently targeted by the {@link Minecraft#player local player}
         * @return A {@link GridAction action} 
         */
        @OnlyIn(Dist.CLIENT)
        default GridAction evaluateTarget(ClientLevel world, AncillaryNode<?> target) {
            return GridAction.RESPONSE_SUCCESS;
        }
        TransmitterType getTransmitter();
    }
}
