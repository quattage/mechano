package com.quattage.experimental_tables;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.entity.TropicalFishEntityRenderer;
import software.bernie.example.registry.TileRegistry;
import software.bernie.geckolib3.GeckoLib;
import software.bernie.geckolib3.model.AnimatedGeoModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quattage.experimental_tables.content.block.entity.InductorBlockEntity;
import com.quattage.experimental_tables.content.block.entity.model.InductorBlockModel;
import com.quattage.experimental_tables.content.block.entity.renderer.InductorRenderer;
import com.quattage.experimental_tables.registry.ModBlockEntities;
import com.quattage.experimental_tables.registry.ModBlocks;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;

public class ExperimentalTables implements ModInitializer, ClientModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final String MOD_ID = "experimental_tables";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	
	private static final NonNullSupplier<CreateRegistrate> REGISTRATE =
            NonNullSupplier.lazy(() -> CreateRegistrate.create(ExperimentalTables.MOD_ID));

	@Override
	public void onInitialize() {
		ModBlocks.register();
		ModBlockEntities.register();
		REGISTRATE.get().register();
		GeckoLib.initialize();
	}

	@Override
	public void onInitializeClient() {
		BlockEntityRendererRegistry.register(ModBlockEntities.INDUCTOR.get(), InductorRenderer::new);
	}

	public static CreateRegistrate registrate() {
		return REGISTRATE.get();
	}	
}
