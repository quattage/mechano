package com.quattage.mechano;


import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class MechanoCapabilities {

    // public static final class WattStorage {
    //     public static final BlockCapability<WattStorable, @Nullable Direction> BLOCK = BlockCapability.createSided(Mechano.asResource("watt"), WattStorable.class);
    //     public static final EntityCapability<WattStorable, @Nullable Direction> ENTITY = EntityCapability.createSided(Mechano.asResource("watt"), WattStorable.class);
    //     public static final ItemCapability<WattStorable, Void> ITEM = ItemCapability.createVoid(Mechano.asResource("watt"), WattStorable.class);
    // }


}