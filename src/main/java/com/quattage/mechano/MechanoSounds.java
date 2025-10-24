package com.quattage.mechano;

import com.quattage.mechano.api.catenary.CatenaryAttributable;

import net.neoforged.bus.api.IEventBus;

public class MechanoSounds {

    public static void register(IEventBus modBus) {
        CatenaryAttributable.Soundscape.registerResources();
    }

}
