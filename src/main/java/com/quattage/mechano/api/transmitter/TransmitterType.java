package com.quattage.mechano.api.transmitter;

import java.util.Objects;

import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.catenary.Catenaries.PhysicalMaterial;
import com.quattage.mechano.api.catenary.Catenaries.Soundscape;
import com.quattage.mechano.api.catenary.model.CatenaryModelProvider;
import com.quattage.mechano.api.grid.HierarchicalConstruct;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.topology.MNAIndexer;
import com.quattage.mechano.api.grid.topology.NodeUnionSet;
import com.quattage.mechano.api.grid.topology.landmark.AncillaryNode;
import com.quattage.mechano.api.grid.topology.landmark.Node;
import com.quattage.mechano.api.switchboard.JackSelector;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class TransmitterType {

    private ResourceLocation registryID;
    private final UnionFactory factory;
    private final NonNullSupplier<CatenaryRenderProperties> renderProperties;
    private final PhysicalMaterial phys;
    private final Soundscape sounds;
    private final int maxSpan;

    public TransmitterType(ResourceLocation registryID, @Nullable String name, UnionFactory factory, NonNullSupplier<CatenaryRenderProperties> renderProperties, PhysicalMaterial phys, Soundscape sounds, int maxSpan) {
        Objects.requireNonNull(factory);
        if(name == null || name.isBlank()) name = "unnamed";
        else name = name.toLowerCase();
        this.registryID = ResourceLocation.fromNamespaceAndPath(registryID.getNamespace(), name);
        this.factory = factory;
        this.renderProperties = renderProperties;
        this.phys = phys == null ? PhysicalMaterial.AIR : phys;
        this.sounds = sounds == null ? Soundscape.AIR : sounds;
        this.maxSpan = maxSpan;
    }

    protected void setRegistryID(ResourceLocation registryID) {
        this.registryID = registryID;
    }

    public ResourceLocation getRegistryID() {
        return registryID;
    }

    public UnionFactory getFactory() {
        return factory;
    }

    @OnlyIn(Dist.CLIENT)
    public CatenaryRenderProperties getRenderProperties() {
        CatenaryRenderProperties rp = renderProperties.get();
        rp.configure(this);
        return rp;
    }

    public PhysicalMaterial getPhysicalProperties() {
        return phys;
    }

    public int getMaximumSpan() {
        return maxSpan;
    }

    public Soundscape getSounds() {
        return sounds;
    }

    public String getName() {
        return registryID.getNamespace();
    }

    @Override
    public String toString() {
        return "TransmitterType[" + registryID + "]";
    }

    @OnlyIn(Dist.CLIENT)
    public void applyResourceReloadResult(@Nullable CatenaryModelProvider.ModelDefinition model) {
        if(model == null) this.renderProperties.get().applyTextures(null, null);
        else this.renderProperties.get().applyTextures(model.getAtlas(), model.getTexture());
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
        if(start == null) throw new GridUnionException(src, "start is null! (this method may have been called by an AncillaryPair that hasn't been initialized)");
        if(end == null) throw new GridUnionException(src, "end is null! (this method may have been called by an AncillaryPair that hasn't been initialized)");
        CircuitComponent output = null;
        try { output = factory.apply(grid, start, end); }
        catch (RuntimeException e) { 
            if(e instanceof GridUnionException gue) throw gue;
            e.printStackTrace();
            throw new GridUnionException(src, "Encountered an error while applying factory! (See exception above)");
        }
        if(output != null && src instanceof HierarchicalConstruct parent && output instanceof HierarchicalConstruct child) 
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
            grid.netlist().union(startAncillary.getAssociatedNode(), endAncillary.getAssociatedNode());
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
