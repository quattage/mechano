package com.quattage.mechano.api.transmitter;

import java.util.Objects;

import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.grid.topology.ancillary.WireJack;
import com.tterrag.registrate.util.nullness.NonNullBiFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class TransmitterType<T extends CircuitComponent> {
    
    private final ResourceLocation loc;
    private final NonNullBiFunction<WireJack, WireJack, T> factory;
    private final NonNullSupplier<CatenaryRenderProperties> renderProperties;

    public TransmitterType(ResourceLocation loc, NonNullBiFunction<WireJack, WireJack, T> factory, NonNullSupplier<CatenaryRenderProperties> renderProperties) {
        Objects.requireNonNull(factory);
        Objects.requireNonNull(loc);
        this.factory = factory;
        this.loc = loc;
        this.renderProperties = renderProperties;
    }

    public T create(WireJack start, WireJack end) {
        if(start == null) throw new CircuitComponentInstantiationException(loc, "start is null!");
        if(end == null) throw new CircuitComponentInstantiationException(loc, "end is null!");
        if(!start.isSignificant()) throw new CircuitComponentInstantiationException(loc, "start is insignificant!");
        if(!end.isSignificant()) throw new CircuitComponentInstantiationException(loc, "end is insignificant!");
        T out = factory.apply(start, end);
        if(out == null) throw new CircuitComponentInstantiationException(loc, "factory failed to provide a non-null component instance!");
        return out;
    }

    public NonNullBiFunction<WireJack, WireJack, T> getFactory() {
        return this.factory;
    }

    public ResourceLocation getResourceLocation() {
        return loc;
    }

    @OnlyIn(Dist.CLIENT)
    public CatenaryRenderProperties getRenderProperties() {
        return renderProperties.get();
    }

    private static class CircuitComponentInstantiationException extends RuntimeException {
        public CircuitComponentInstantiationException(ResourceLocation loc, String message) {
            super("Failed to provide CircuitComponent for Transmitter '" + loc + "' - " + message);
        }
    }

    @Override
    public String toString() {
        return "TransmitterType[" + loc + ", client init? " + (renderProperties != null) + "]";
    }
}
