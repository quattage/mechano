package com.quattage.experimental_tables;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quattage.experimental_tables.registry.ModBlockEntities;
import com.quattage.experimental_tables.registry.ModBlocks;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

public class ExperimentalTables implements ModInitializer {
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
	}

	public static CreateRegistrate registrate() {
		return REGISTRATE.get();
	}
}
