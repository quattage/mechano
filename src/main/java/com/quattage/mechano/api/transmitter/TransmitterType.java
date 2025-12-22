package com.quattage.mechano.api.transmitter;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.tterrag.registrate.util.nullness.NonNullBiFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class TransmitterType<T extends CircuitComponent> {
    
    private final String name;
    private final NonNullBiFunction<Node, Node, T> factory;
    private final NonNullSupplier<CatenaryRenderProperties> renderProperties;

    /**
     * Retrieves a {@link TransmitterType} from its associated {@link TransmitterEntry} 
     * contained within the {@link BuiltInRegistries#REGISTRY static registry}.
     * This method will throw if no transmitter at <code>name</code> could be found.
     * @param name The ResourceLocation of the desired TransmitterType
     * @return The TransmitterType at <code>name</code>
     */
    public static TransmitterType<?> getByName(ResourceLocation name) {
        TransmitterType<?> trns = Mechano.REGISTRATE.getTransmitterRegistry().get(name); 
        if(trns == null) throw new IllegalArgumentException("Couldn't find TransmitterType entry at " + name);
        return trns;
    }

    public TransmitterType(@Nullable String name, NonNullBiFunction<Node, Node, T> factory, NonNullSupplier<CatenaryRenderProperties> renderProperties) {
        Objects.requireNonNull(factory);
        if(name == null || name.isBlank()) name = "unnamed";
        else name = name.toLowerCase();
        this.factory = factory;
        this.name = name;
        this.renderProperties = renderProperties;
    }

    public T instantiate(Node start, Node end) {
        if(start == null) throw new CircuitComponentInstantiationException(name, "start is null!");
        if(end == null) throw new CircuitComponentInstantiationException(name, "end is null!");
        if(!start.isSignificant()) throw new CircuitComponentInstantiationException(name, "start is insignificant!");
        if(!end.isSignificant()) throw new CircuitComponentInstantiationException(name, "end is insignificant!");
        T out = factory.apply(start, end);
        if(out == null) throw new CircuitComponentInstantiationException(name, "factory failed to provide a non-null component instance!");
        return out;
    }

    public NonNullBiFunction<Node, Node, T> getFactory() {
        return this.factory;
    }

    @OnlyIn(Dist.CLIENT)
    public CatenaryRenderProperties getRenderProperties() {
        return renderProperties.get();
    }

    private static class CircuitComponentInstantiationException extends RuntimeException {
        public CircuitComponentInstantiationException(String name, String message) {
            super("Failed to provide CircuitComponent for Transmitter '" + name + "' - " + message);
        }
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "TransmitterType[" + name + "]";
    }
}
