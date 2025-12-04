

package com.quattage.mechano;


import java.util.function.Supplier;

import com.quattage.mechano.api.Grid;
import com.quattage.mechano.foundation.tracking.UUIDSourceDiscriminator;

import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

// DataComponents, DataAttachments, and Capabilities
public class MechanoData {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_REGISTRY = DeferredRegister.create(
        NeoForgeRegistries.ATTACHMENT_TYPES, Mechano.ID);
    public static final DeferredRegister.DataComponents COMPONENT_REGISTRY = DeferredRegister.createDataComponents(
        Registries.DATA_COMPONENT_TYPE, Mechano.ID);

    public static final Supplier<AttachmentType<Grid>> GRID
        = MechanoData.ATTACHMENT_REGISTRY.register(
            Mechano.ID + "_grid", () -> AttachmentType
                .builder(Grid::createNew)
                .build()
                // TODO CODEC SERIALIZE
    );

    public static void register(IEventBus modBus) {
        UUIDSourceDiscriminator.register(modBus);
        MechanoData.ATTACHMENT_REGISTRY.register(modBus);
        MechanoData.COMPONENT_REGISTRY.register(modBus);
    }
}