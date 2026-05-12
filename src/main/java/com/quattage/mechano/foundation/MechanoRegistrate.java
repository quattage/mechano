package com.quattage.mechano.foundation;


import com.quattage.mechano.api.transmitter.TransmitterBuilder;
import com.quattage.mechano.api.transmitter.TransmitterType;
import com.quattage.mechano.grid.api.component.CircuitComponent;
import com.simibubi.create.foundation.data.CreateRegistrate;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.RegistryBuilder;

public final class MechanoRegistrate extends CreateRegistrate {

    private final ResourceKey<Registry<TransmitterType>> trnsKey;

    public static MechanoRegistrate make(String modid) {
        return new MechanoRegistrate(modid);
    }

    protected MechanoRegistrate(String modid) {
        super(modid);
        this.trnsKey = makeRegistry("transmitter_type", this::supplyTransmitterRegistry);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Registry<TransmitterType> getTransmitterRegistry() {
        return (Registry<TransmitterType>)BuiltInRegistries.REGISTRY.getOrThrow((ResourceKey)trnsKey);
    }

    private RegistryBuilder<TransmitterType> supplyTransmitterRegistry(ResourceKey<Registry<TransmitterType>> key) {
        return new RegistryBuilder<TransmitterType>(key).maxId(256).sync(true);
    }

    public <T extends CircuitComponent, P> TransmitterBuilder<T, P> transmitter(String name) {
        return entry(name, callback -> TransmitterBuilder.create(self(), name, callback, trnsKey));
    }
}

