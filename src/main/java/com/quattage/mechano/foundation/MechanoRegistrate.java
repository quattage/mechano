package com.quattage.mechano.foundation;


import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.transmitter.TransmitterBuilder;
import com.quattage.mechano.api.transmitter.TransmitterType;
import com.simibubi.create.foundation.data.CreateRegistrate;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;


public class MechanoRegistrate extends CreateRegistrate {

    public static final ResourceKey<Registry<TransmitterType<? extends CircuitComponent>>> 
        TRANSMITTER_KEY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Mechano.ID, "transmitter_type"));

    public static MechanoRegistrate make(String modid) {
        return new MechanoRegistrate(modid);
    }

    protected MechanoRegistrate(String modid) {
        super(modid);
    }

    public <T extends CircuitComponent, P> TransmitterBuilder<T, P> transmitter(String name) {
        return entry(name, callback -> TransmitterBuilder.create(self(), name, callback, MechanoRegistrate.TRANSMITTER_KEY));
    }
}

