package com.quattage.mechano.api.transmitter;

import java.util.Objects;
import java.util.function.BiFunction;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.catenary.Catenaries.PhysicalMaterial;
import com.quattage.mechano.catenary.Catenaries.Soundscape;
import com.quattage.mechano.catenary.CatenaryRenderProperties;
import com.quattage.mechano.catenary.model.CatenaryModelProvider;
import com.quattage.mechano.grid.Netlist;
import com.quattage.mechano.grid.ServerGrid;
import com.quattage.mechano.grid.topology.AncillaryNode;
import com.quattage.mechano.grid.topology.AncillaryPair;
import com.quattage.mechano.grid.topology.core.CircuitComponent;
import com.quattage.mechano.grid.topology.core.HierarchicalConstruct;
import com.quattage.mechano.grid.topology.core.Node;
import com.quattage.mechano.switchboard.JackSelector;
import com.quattage.mechano.switchboard.action.GridAction;
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

    public static @Nullable CircuitComponent applyUnion(Netlist netlist, UnionFactory factory, AncillaryPair link) {
        if(netlist == null) throw new GridUnionException(link, "netlist is null!");
        if(link == null) throw new GridUnionException(link, "link is null!");
        CircuitComponent output = null;
        try { output = factory.apply(netlist, link); }
        catch (RuntimeException e) { 
            if(e instanceof GridUnionException gue) throw gue;
            e.printStackTrace();
            throw new GridUnionException(link, "Encountered an error while applying factory! (See exception above)");
        }
        if(output != null && link instanceof HierarchicalConstruct parent && output instanceof HierarchicalConstruct child) 
            child.updateOwnership(parent);
        return output;
    }

    @FunctionalInterface
    public interface UnionFactory extends BiFunction<Netlist, AncillaryPair, CircuitComponent>{

        /**
         * A shorthanded {@link UnionFactory} substitute for unions that represent
         * perfect conductors. (e.g. a wire with no resistence.)
         * @param netlist Domain to append the link to
         * @param link The link to union
         * @return <code>null,</code> since a perfect union doesn't have a component associated with it.
         */
        static CircuitComponent perfectConductor(Netlist netlist, AncillaryPair link) {
            netlist.union(link.getStartNode(), link.getEndNode());
            return null;
        }

        /**
         * Supply some logic here to create an arbitrary connection between two 
         * {@link AncillaryNode ancillaries} - <code>startAncillary</code> and
         * <code>endAncillary</code>. Within the scope of this function, you are permitted to make 
         * any changes to the {@link ServerGrid grid} you'd like, including 
         * {@link NodeUnionSet topological alterations} and {@link NetlistIndexer stamper assertions}.
         * <h3>with great power comes great oh no i broke it</h3>
         * There are no guardrails here; you have direct access to the grid's topology! Be careful
         * not to perform destructive operations that destabilize the grid.
         * @param netlist {@link Netlist} which provides access to,the netlist to be modified
         * @param link the link which contains the start and end points
         * @return Any {@link CircuitComponent} instance, or <code>null</code> if this union did not result 
         * in the creation of a discrete component.
         */
        @Override 
        @Nullable CircuitComponent apply(Netlist netlist, AncillaryPair link);
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
