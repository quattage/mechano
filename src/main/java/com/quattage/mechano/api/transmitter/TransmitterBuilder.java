package com.quattage.mechano.api.transmitter;

import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.grid.topology.ancillary.WireJack;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.AbstractBuilder;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.util.RegistrateDistExecutor;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullBiFunction;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import com.tterrag.registrate.util.nullness.NonnullType;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.registries.DeferredHolder;

public class TransmitterBuilder<T extends CircuitComponent, P> extends AbstractBuilder<TransmitterType<?>, TransmitterType<T>, P, TransmitterBuilder<T, P>> {

    public static <T extends CircuitComponent, P> TransmitterBuilder<T, P> create(AbstractRegistrate<?> owner, String name, BuilderCallback callback, ResourceKey<? extends Registry<TransmitterType<?>>> key) {
        return new TransmitterBuilder<T, P>(owner, (P)owner, name, callback, key);
    }

    private NonNullBiFunction<WireJack, WireJack, T> factory;
    private NonNullSupplier<CatenaryRenderProperties> renderProperties;

    public TransmitterBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback,
            ResourceKey<? extends Registry<TransmitterType<?>>> registryKey) {
        super(owner, parent, name, callback, registryKey);
    }

    public TransmitterBuilder<T, P> component(NonNullBiFunction<WireJack, WireJack, T> factory) {
        this.factory = factory;
        return this;
    }

    public TransmitterBuilder<T, P> renderer(NonNullSupplier<NonNullConsumer<CatenaryRenderProperties>> renderProperties) {
        if(this.renderProperties != null) return this;
        RegistrateDistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> 
            () -> this.transformRenderProperties(renderProperties.get()));
        return this;
    }

    @OnlyIn(Dist.CLIENT)
    private void transformRenderProperties(NonNullConsumer<CatenaryRenderProperties> renderProperties) {
        CatenaryRenderProperties rp = new CatenaryRenderProperties(getRegistryKey().location());
        renderProperties.accept(rp);
        this.renderProperties = () -> rp;
    }

    @Override
    protected @NonnullType TransmitterType<T> createEntry() {
        return new TransmitterType<T>(getName(), factory, renderProperties);
    }

    @Override
    protected RegistryEntry<TransmitterType<?>, TransmitterType<T>> createEntryWrapper(
            DeferredHolder<TransmitterType<?>, TransmitterType<T>> delegate) {
        return new TransmitterEntry<>(getOwner(), delegate);
    }

    @Override
    public TransmitterEntry<T> register() {
        return (TransmitterEntry<T>)super.register();
    }
}
