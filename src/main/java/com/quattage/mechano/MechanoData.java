

package com.quattage.mechano;


import java.util.function.Supplier;

import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.grid.GridComponentTracker;
import com.quattage.mechano.api.grid.component.ComponentUUID;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;


// DataComponents, DataAttachments, Capabilities, and miscelaneous unsided events
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
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ComponentUUID<?>>> UUID = 
        MechanoData.COMPONENT_REGISTRY.registerComponentType(
            "grid_identifier",
            b -> b.persistent(GridComponentTracker.CODEC).networkSynchronized(GridComponentTracker.STREAM_CODEC)
    );


    public static void register(IEventBus modBus) {
        MechanoData.ATTACHMENT_REGISTRY.register(modBus);
        MechanoData.COMPONENT_REGISTRY.register(modBus);
    }
}