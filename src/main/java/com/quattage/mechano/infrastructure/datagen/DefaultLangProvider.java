package com.quattage.mechano.infrastructure.datagen;

import java.util.Map;

import com.google.gson.JsonElement;
import com.quattage.mechano.Mechano;
import com.simibubi.create.foundation.utility.FilesHelper;
import com.tterrag.registrate.providers.RegistrateLangProvider;

public class DefaultLangProvider {

    public static final String[] fileTargetNames = new String[] {
        "ui"
    };

    public static void generate(RegistrateLangProvider provider) {
        for(int x = 0; x < fileTargetNames.length; x++) {
            String path = "assets/" + Mechano.ID + "/lang/custom/" + fileTargetNames[x] + ".json";
            Mechano.LOGGER.debug("Loading data from '" + path + "'");
            JsonElement file = FilesHelper.loadJsonResource(path);
            if(file == null) {
                Mechano.LOGGER.error("Couldn't merge lang file '" + path + "' - File couldn't be found!");
                return;
            }
            for(Map.Entry<String, JsonElement> element : file.getAsJsonObject().entrySet()) {
                if(element == null) continue;
                provider.add(element.getKey(), element.getValue().getAsString());
            }
        }
    }
}
