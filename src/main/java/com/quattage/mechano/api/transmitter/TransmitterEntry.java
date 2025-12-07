
package com.quattage.mechano.api.transmitter;

import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;

import net.neoforged.neoforge.registries.DeferredHolder;

public class TransmitterEntry<T extends CircuitComponent> extends RegistryEntry<TransmitterType<?>, TransmitterType<T>> { 

    public TransmitterEntry(AbstractRegistrate<?> owner, DeferredHolder<TransmitterType<?>, TransmitterType<T>> delegate) {
        super(owner, delegate);
    }

}

