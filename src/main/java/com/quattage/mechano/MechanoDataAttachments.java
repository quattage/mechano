package com.quattage.mechano;


import java.util.function.Supplier;

import com.quattage.mechano.foundation.api.grid.SidedGridDispatcher;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class MechanoDataAttachments {

    private static final DeferredRegister<AttachmentType<?>> REGISTRY = DeferredRegister.create(
        NeoForgeRegistries.ATTACHMENT_TYPES, Mechano.ID);

        public static final Supplier<AttachmentType<SidedGridDispatcher>> GRID_DATA = REGISTRY.register(
            "grid_data", () -> AttachmentType
                .builder(SidedGridDispatcher::createNew)
                .serialize(SidedGridDispatcher.SERIALIZER)
                .build());

    public static void register(IEventBus modBus) {
        REGISTRY.register(modBus);
    }
}