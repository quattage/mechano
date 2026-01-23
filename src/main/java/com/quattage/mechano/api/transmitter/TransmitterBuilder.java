package com.quattage.mechano.api.transmitter;

import com.quattage.mechano.api.catenary.Catenaries.PhysicalMaterial;
import com.quattage.mechano.api.catenary.Catenaries.Soundscape;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.transmitter.TransmitterType.UnionFactory;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.AbstractBuilder;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.util.RegistrateDistExecutor;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import com.tterrag.registrate.util.nullness.NonnullType;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.registries.DeferredHolder;

public class TransmitterBuilder<T extends CircuitComponent, P> extends AbstractBuilder<TransmitterType, TransmitterType, P, TransmitterBuilder<T, P>> {

    @SuppressWarnings("unchecked")
    public static <T extends CircuitComponent, P> TransmitterBuilder<T, P> create(AbstractRegistrate<?> owner, String name, BuilderCallback callback, ResourceKey<? extends Registry<TransmitterType>> key) {
        return new TransmitterBuilder<T, P>(owner, (P)owner, name, callback, key);
    }

    private UnionFactory factory;
    private NonNullSupplier<CatenaryRenderProperties> renderProperties;
    private PhysicalMaterial phys;
    private Soundscape sounds;
    private int maxSpan = Integer.MAX_VALUE;

    public TransmitterBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback,
            ResourceKey<? extends Registry<TransmitterType>> registryKey) {
        super(owner, parent, name, callback, registryKey);
    }

    /**
     * This is where you provide the actual implementation for this Transmitter. 
     * You should supply a {@link UnionFactory} here that makes changes to the
     * grid as needed and returns a {@link CircuitComponent} instance (or <code>null</code>)
     * <p>
     * It's reccomended
     * for most use cases to store this function statically somewhere outside of the definition
     * for the transmitter itself.
     * @param factory The factory that this transmitter will run whenever connections are made
     * @return This builder for chaining
     */
    public TransmitterBuilder<T, P> functionsAs(UnionFactory factory) {
        this.factory = factory;
        return this;
    }

    public TransmitterBuilder<T, P> renderer(NonNullSupplier<NonNullConsumer<CatenaryRenderProperties>> renderProperties) {
        if(this.renderProperties != null) return this;
        RegistrateDistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> 
            () -> this.transformRenderProperties(renderProperties.get()));
        return this;
    }

    public TransmitterBuilder<T, P> feelsLike(PhysicalMaterial phys) {
        if(this.phys != null) return this;
        this.phys = phys;
        return this;
    }

    public TransmitterBuilder<T, P> soundsLike(Soundscape sounds) {
        if(this.sounds != null) return this;
        this.sounds = sounds;
        return this;
    }

    public TransmitterBuilder<T, P> canBeAsLongAs(int meters) {
        this.maxSpan = meters;
        return this;
    }

    @OnlyIn(Dist.CLIENT)
    private void transformRenderProperties(NonNullConsumer<CatenaryRenderProperties> renderProperties) {
        CatenaryRenderProperties rp = new CatenaryRenderProperties(getRegistryKey().location());
        renderProperties.accept(rp);
        this.renderProperties = () -> rp;
    }

    @Override
    protected @NonnullType TransmitterType createEntry() {
        return new TransmitterType(getName(), factory, renderProperties, phys, sounds, maxSpan);
    }

    @Override
    protected RegistryEntry<TransmitterType, TransmitterType> createEntryWrapper(
            DeferredHolder<TransmitterType, TransmitterType> delegate) {
        return new TransmitterEntry(getOwner(), delegate);
    }

    @Override
    public TransmitterEntry register() {
        return (TransmitterEntry)super.register();
    }
}
