package com.quattage.mechano;

import com.quattage.mechano.content.block.Inductor.InductorBlockRenderer;
import com.quattage.mechano.registry.MechanoBlockEntities;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import net.fabricmc.api.ClientModInitializer;

public class MechanoClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		Mechano.log("Registering Client");
		BlockEntityRendererRegistry.register(MechanoBlockEntities.INDUCTOR.get(), InductorBlockRenderer::new);
	}
	
}
