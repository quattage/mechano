package com.quattage.mechano;

import net.fabricmc.api.ModInitializer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import software.bernie.geckolib3.GeckoLib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quattage.mechano.registry.MechanoBlockEntities;
import com.quattage.mechano.registry.MechanoBlocks;
import com.quattage.mechano.registry.MechanoItems;
import com.quattage.mechano.registry.MechanoRecipes;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

public class Mechano implements ModInitializer {
	public static final String MOD_ID = "mechano";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	
	private static final NonNullSupplier<CreateRegistrate> REGISTRATE =
            NonNullSupplier.lazy(() -> CreateRegistrate.create(Mechano.MOD_ID));

	@Override
	public void onInitialize() {
		MechanoBlocks.register();
		MechanoBlockEntities.register();
		MechanoItems.register();
		MechanoRecipes.register();
		REGISTRATE.get().register();
		GeckoLib.initialize();
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

	public static MutableText newKey(String key, boolean useColon) {
		if(useColon)
			return Text.translatable(MOD_ID + ":" + key);
		return newKey(key);
	}

	public static String log(String input) {
		LOGGER.info(input);
		return input;
	}
}
