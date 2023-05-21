package com.quattage.mechano.registry;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.block.Inductor.InductorBlockRenderer;

import net.minecraftforge.client.event.EntityRenderersEvent;

// This is where block renderers get registered
// CALLED BY LOGICAL CLIENT ONLY
public class MechanoRenderers {
    public static void register(final EntityRenderersEvent.RegisterRenderers event) {
        Mechano.log("Registering Mechano renderers");
        event.registerBlockEntityRenderer(MechanoBlockEntities.INDUCTOR.get(), InductorBlockRenderer::new);
    }
}
