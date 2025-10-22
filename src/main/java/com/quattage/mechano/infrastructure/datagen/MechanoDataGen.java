package com.quattage.mechano.infrastructure.datagen;

import com.quattage.mechano.Mechano;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateDataProvider;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public class MechanoDatagen {

    private static void addGenerators() {
        Mechano.REGISTRATE.addDataGenerator(ProviderType.LANG, DefaultLangProvider::generate);
        Mechano.REGISTRATE.addDataGenerator(ProviderType.GENERIC_SERVER, HitboxDataProvider::generate);
    }

    public static void collect(GatherDataEvent event) {
        addGenerators();
        RegistrateDataProvider provider = new RegistrateDataProvider(Mechano.REGISTRATE, Mechano.ID, event);
        // Mechano.REGISTRATE.setDataProvider(provider);
        event.getGenerator().addProvider(true, provider);
    }

    public static void register(IEventBus modBus) {}
}
