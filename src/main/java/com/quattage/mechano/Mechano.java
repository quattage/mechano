package com.quattage.mechano;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import software.bernie.geckolib3.GeckoLib;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quattage.mechano.content.block.Inductor.InductorBlockRenderer;
import com.quattage.mechano.content.block.Inductor.InductorItemRenderer;
import com.quattage.mechano.registry.ModBlockEntities;
import com.quattage.mechano.registry.ModBlocks;
import com.quattage.mechano.registry.ModItems;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;

public class Mechano implements ModInitializer, ClientModInitializer {
	public static final String MOD_ID = "mechano";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	
	private static final NonNullSupplier<CreateRegistrate> REGISTRATE =
            NonNullSupplier.lazy(() -> CreateRegistrate.create(Mechano.MOD_ID));

	@Override
	public void onInitialize() {
		ModBlocks.register();
		ModBlockEntities.register();
		ModItems.register();
		REGISTRATE.get().register();
		GeckoLib.initialize();
	}

	@Override // TODO move this 
	public void onInitializeClient() {
		BlockEntityRendererRegistry.register(ModBlockEntities.INDUCTOR.get(), InductorBlockRenderer::new);
		GeoItemRenderer.registerItemRenderer(ModItems.INDUCTOR_ITEM, new InductorItemRenderer());
	}

	public static CreateRegistrate registrate() {
		return REGISTRATE.get();
	}	

	public static Identifier newResource(String resourceLocation) {
        return new Identifier(MOD_ID, resourceLocation);
    }

	public static MutableText newKey(String key) {
		return Text.translatable(MOD_ID + "." + key);
	}
}
