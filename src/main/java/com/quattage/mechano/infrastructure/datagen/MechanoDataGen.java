package com.quattage.mechano.infrastructure.datagen;

import java.util.Map;

import com.google.gson.JsonElement;
import com.quattage.mechano.Mechano;
import com.simibubi.create.foundation.utility.FilesHelper;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateLangProvider;

import net.minecraft.data.DataGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public class MechanoDatagen {

    public static final String[] targetFilenames = new String[] {
        "ui"
    };

    public static void collectHighPriority(GatherDataEvent event) {
        if(!event.getMods().contains(Mechano.ID)) return;
        Mechano.REGISTRATE.addDataGenerator(ProviderType.LANG, provider -> {
            MechanoDatagen.provideDefaultLang(provider);
        });
    }

    private static void provideDefaultLang(RegistrateLangProvider prov) {
        for(int x = 0; x < MechanoDatagen.targetFilenames.length; x++) {
            String path = "assets/" + Mechano.ID + "/lang/custom/" + MechanoDatagen.targetFilenames[x] + ".json";
            Mechano.LOGGER.debug("Loading data from '" + path + "'");
            JsonElement file = FilesHelper.loadJsonResource(path);
            if(file == null) {
                Mechano.LOGGER.error("Couldn't merge lang file '" + path + "' - File couldn't be found!");
                return;
            }
            for(Map.Entry<String, JsonElement> element : file.getAsJsonObject().entrySet()) {
                if(element == null) continue;
                prov.add(element.getKey(), element.getValue().getAsString());
            }
        }
    }

    public static void collectLowPriority(GatherDataEvent event) {
        if(!event.getMods().contains(Mechano.ID)) return;
        DataGenerator generator = event.getGenerator();
        generator.addProvider(event.includeDev(), new HitboxDataProvider(generator, event));
    }

    public static void register(IEventBus modBus) {}
}
