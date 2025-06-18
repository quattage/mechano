package com.quattage.mechano;


import java.util.function.Supplier;

import com.quattage.mechano.foundation.api.GridChunkData;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.anchor.EntityAnchorPointHost;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
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

    public static final Supplier<AttachmentType<SidedGridDispatcher.ChunkData>> CHUNK_ATTACHMENT
        = ATTACHMENT_REGISTRY.register(
            Mechano.ID + "_chunk_data", () -> AttachmentType
                .builder(SidedGridDispatcher.ChunkData::createNew)
                .build()
        );

    public static EntityCapability<EntityAnchorPointHost, Void> ANCHOR_CAPABILITY = EntityCapability.createVoid(Mechano.asResource("anchor"), EntityAnchorPointHost.class);

    public static void register(IEventBus modBus) {
        ATTACHMENT_REGISTRY.register(modBus);
        COMPONENT_REGISTRY.register(modBus);
    }

    @EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
    public static class Bus {
        @SubscribeEvent
        public static void attachCaps(RegisterCapabilitiesEvent evt) {
            for(EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
                evt.registerEntity(ANCHOR_CAPABILITY, type, (entity, ctx) -> {
                    if(!(entity instanceof LivingEntity)) return null;
                    return new EntityAnchorPointHost(entity);
                });
            }
        }
    }
}