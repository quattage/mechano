package com.quattage.mechano;

import com.quattage.mechano.infrastructure.datagen.MechanoDatagen;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = Mechano.ID, dist = Dist.CLIENT)
public class MechanoClient {
    
    public MechanoClient(IEventBus modBus) {
        modBus.addListener(MechanoClientEvents::onRegisterLayers);
        modBus.addListener(MechanoClientEvents::onRegisterReloadListeners);
        modBus.addListener(EventPriority.LOWEST, MechanoDatagen::collect);
    }

}
