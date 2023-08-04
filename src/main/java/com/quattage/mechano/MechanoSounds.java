package com.quattage.mechano;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

public class MechanoSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Mechano.MOD_ID);
    
    public static final RegistryObject<SoundEvent>
        CABLE_CREATE = create("cable_create")
    ;

    

    private static RegistryObject<SoundEvent> create(String name) {
        ResourceLocation id = Mechano.asResource(name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus event) {
        SOUNDS.register(event);
    }
}
