package com.quattage.mechano;

import java.util.Map;
import java.util.function.BiConsumer;

import com.google.gson.JsonElement;
import com.quattage.mechano.infrastructure.hitbox.HitboxData;
import com.simibubi.create.foundation.utility.FilesHelper;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateDataProvider;

import net.neoforged.neoforge.data.event.GatherDataEvent;

public class MechanoData {

    public static void collect(GatherDataEvent event) {
        Mechano.REGISTRATE.addDataGenerator(ProviderType.LANG, provider -> {
            mergeLang("ui", provider::add);
		});
        Mechano.REGISTRATE.addDataGenerator(ProviderType.GENERIC_SERVER, HitboxData::generate);
        event.getGenerator().addProvider(true, 
            Mechano.REGISTRATE.setDataProvider(
                new RegistrateDataProvider(Mechano.REGISTRATE, Mechano.ID, event
            )
        ));
    }

    private static void mergeLang(String fileName, BiConsumer<String, String> consumer) {
        String path = "assets/" + Mechano.ID + "/lang/custom/" + fileName + ".json";
        Mechano.LOGGER.debug("Loading data from '" + path + "'");
        JsonElement file = FilesHelper.loadJsonResource(path);
        if(file == null) {
            Mechano.LOGGER.error("Couldn't merge lang file '" + fileName + "' - File couldn't be found!");
            return;
        }
        for(Map.Entry<String, JsonElement> element : file.getAsJsonObject().entrySet()) {
            if(element == null) continue;
            consumer.accept(element.getKey(), element.getValue().getAsString());
        }
    }
}
