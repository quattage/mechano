package com.quattage.mechano.registry;

import com.quattage.mechano.Mechano;

import net.minecraftforge.client.event.EntityRenderersEvent;

// This is where renderers get registered to define custom block looks and such
// CALLED BY LOGICAL CLIENT ONLY
public class MechanoRenderers {
    public static void register(final EntityRenderersEvent.RegisterRenderers event) {
        Mechano.log("Registering mod renderers");
    }
}
