
package com.quattage.mechano.api.transmitter;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;

import net.neoforged.neoforge.registries.DeferredHolder;

public class TransmitterEntry extends RegistryEntry<TransmitterType, TransmitterType> { 
    public TransmitterEntry(AbstractRegistrate<?> owner, DeferredHolder<TransmitterType, TransmitterType> delegate) {
        super(owner, delegate);
    }
}

