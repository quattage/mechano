package com.quattage.mechano.foundation.api.transmitter;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.spool.HookupTransmitter;
import com.quattage.mechano.foundation.api.catenary.CatenaryAttributes.ModelType;
import com.quattage.mechano.foundation.api.catenary.CatenaryAttributes.PhysicalMaterial;
import com.quattage.mechano.foundation.api.catenary.CatenaryAttributes.Soundscape;
import com.quattage.mechano.foundation.api.catenary.CatenaryAttributes.Thickness;

import net.neoforged.bus.api.IEventBus;

public class MechanoTransmissionTypes {

    public static final TransmitterType<PerfectConductor> PERFECT_CONDUCTOR = TransmitterRegistry.INSTANCE.register(
        Mechano.asResource("perfect_conductor"), () -> Transmitter
            .builder(PerfectConductor::new)
            .writesToNetwork(null)
            .withAttributes(attr -> {
                attr.withModelType(ModelType.NO_DRAW);
                attr.withThickness(Thickness.ZERO);
                attr.withMaterial(PhysicalMaterial.AIR);
                attr.withSounds(Soundscape.AIR);
                attr.enableInterconnectivity();
            })
            .build()
    );

    public static final TransmitterType<PerfectInsulator> PERFECT_INSULATOR = TransmitterRegistry.INSTANCE.register(
        Mechano.asResource("perfect_insulator"), () -> Transmitter
            .builder(PerfectInsulator::new)
            .writesToNetwork(null)
            .withAttributes(attr -> {
                attr.withModelType(ModelType.NO_DRAW);
                attr.withThickness(Thickness.ZERO);
                attr.withMaterial(PhysicalMaterial.AIR);
                attr.withSounds(Soundscape.AIR);
                attr.enableInterconnectivity();
            })
            .build()
    );

    public static final TransmitterType<HookupTransmitter> HOOKUP = TransmitterRegistry.INSTANCE.register(
        Mechano.asResource("hookup"), () -> Transmitter
            .builder(HookupTransmitter::new)
            .writesToNetwork(null)
            .withAttributes(attr -> {
                attr.withModelType(ModelType.SQUARE);
                attr.withThickness(Thickness.TRIPLE);
                attr.withMaterial(PhysicalMaterial.ROPE);
                attr.withSounds(Soundscape.CABLE);
            })
            .build()
    );

    public static void register(IEventBus modBus) {
        TransmitterRegistry.INSTANCE.register(modBus);
    }
}
