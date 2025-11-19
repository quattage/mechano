package com.quattage.mechano;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = Mechano.ID, dist = Dist.CLIENT)
public class MechanoClient {

    public MechanoClient(IEventBus modBus) {
        modBus.addListener(MechanoClientEvents::onRegisterLayers);
        modBus.addListener(MechanoClientEvents::onRegisterReloadListeners);
    }

}
