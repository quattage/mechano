package com.quattage.mechano;


import java.util.function.Supplier;

import com.quattage.mechano.api.LinkDataStorage;
import com.quattage.mechano.api.SidedGridDispatcher;
import com.quattage.mechano.api.entity.GriddableEntityAttachment;
import com.quattage.mechano.api.landmark.GridConnection;

import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

// DataComponents, DataAttachments, and Capabilities
public class MechanoData {

    // data attachments
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_REGISTRY = DeferredRegister.create(
        NeoForgeRegistries.ATTACHMENT_TYPES, Mechano.ID);
    public static final DeferredRegister.DataComponents COMPONENT_REGISTRY = DeferredRegister.createDataComponents(
        Registries.DATA_COMPONENT_TYPE, Mechano.ID);

    public static final Supplier<AttachmentType<SidedGridDispatcher>> GRID_ATTACHMENT 
        = ATTACHMENT_REGISTRY.register(
            Mechano.ID + "_world_data", () -> AttachmentType
                .builder(SidedGridDispatcher::createNew)
                .serialize(SidedGridDispatcher.SERIALIZER)
                .build()
        );

    public static final Supplier<AttachmentType<LinkDataStorage<GridConnection>>> LINK_ATTACHMENT
        = ATTACHMENT_REGISTRY.register(
            Mechano.ID + "_chunk_data", () -> AttachmentType
                .builder(LinkDataStorage::make)
                .build()
        );

    public static final Supplier<AttachmentType<GriddableEntityAttachment>> ANCHOR_ATTACHMENT
        = ATTACHMENT_REGISTRY.register(
            Mechano.ID + "_entity_data", () -> AttachmentType
                .builder(GriddableEntityAttachment::new)
                .serialize(GriddableEntityAttachment.SERIALIZER)
                .build()
        );

    public static void register(IEventBus modBus) {
        ATTACHMENT_REGISTRY.register(modBus);
        COMPONENT_REGISTRY.register(modBus);
    }
}