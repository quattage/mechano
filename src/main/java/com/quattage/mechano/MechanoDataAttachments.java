package com.quattage.mechano;


import java.util.function.Supplier;

import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.landmark.base.NodeIdentifier;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class MechanoDataAttachments {

    // data attachments
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_REGISTRY = DeferredRegister.create(
        NeoForgeRegistries.ATTACHMENT_TYPES, Mechano.ID);

    public static final Supplier<AttachmentType<SidedGridDispatcher>> GRID_ATTACHMENT 
        = ATTACHMENT_REGISTRY.register(
            "grid_data", () -> AttachmentType
                .builder(SidedGridDispatcher::createNew)
                .serialize(SidedGridDispatcher.SERIALIZER)
                .build()
    );





    // data components
    private static final DeferredRegister.DataComponents COMPONENT_REGISTRY = DeferredRegister.createDataComponents(
        Registries.DATA_COMPONENT_TYPE, Mechano.ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<NodeIdentifier.Key>> ADDRESS_COMPONENT
        = COMPONENT_REGISTRY.registerComponentType(
            "address",
            b -> b
                .persistent(NodeIdentifier.Key.CODEC)
                .networkSynchronized(NodeIdentifier.Key.STREAM_CODEC)
    );



    public static void register(IEventBus modBus) {
        ATTACHMENT_REGISTRY.register(modBus);
        COMPONENT_REGISTRY.register(modBus);
    }
}