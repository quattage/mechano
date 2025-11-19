

package com.quattage.mechano;


import com.quattage.mechano.foundation.tracking.DataSourceIdentifier;

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

    // public static final Supplier<AttachmentType<SidedGridDispatcher>> GRID_ATTACHMENT 
    //     = MechanoData.ATTACHMENT_REGISTRY.register(
    //         Mechano.ID + "_world_data", () -> AttachmentType
    //             .builder(SidedGridDispatcher::createNew)
    //             .serialize(SidedGridDispatcher.SERIALIZER)
    //             .build()
    //     );

    // public static final Supplier<AttachmentType<LinkDataStorage<GridConnection>>> LINK_ATTACHMENT
    //     = MechanoData.ATTACHMENT_REGISTRY.register(
    //         Mechano.ID + "_chunk_data", () -> AttachmentType
    //             .builder(LinkDataStorage::make)
    //             .build()
    //     );

    public static void register(IEventBus modBus) {
        DataSourceIdentifier.register(modBus);
        MechanoData.ATTACHMENT_REGISTRY.register(modBus);
        MechanoData.COMPONENT_REGISTRY.register(modBus);
    }
}