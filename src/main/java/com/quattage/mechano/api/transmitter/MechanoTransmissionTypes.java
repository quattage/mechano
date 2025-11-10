package com.quattage.mechano.api.transmitter;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.catenary.CatenaryAttributable.PhysicalMaterial;
import com.quattage.mechano.api.catenary.CatenaryAttributable.Soundscape;
import com.quattage.mechano.content.spool.HookupTransmitter;

import net.neoforged.bus.api.IEventBus;

public class MechanoTransmissionTypes {

    public static final TransmitterType<PerfectConductor> PERFECT_CONDUCTOR = TransmitterRegistry.INSTANCE.register(
        Mechano.asResource("perfect_conductor"), () -> Transmitter
            .builder(PerfectConductor::new)
            .writesToNetwork(null)
            .withAttributes(attr -> { attr
                .withModelType(6)
                .withThickness(0)
                .withMaterial(PhysicalMaterial.AIR)
                .withSounds(Soundscape.AIR)
                .enableInterconnectivity();
            })
            .build()
    );

    public static final TransmitterType<PerfectInsulator> PERFECT_INSULATOR = TransmitterRegistry.INSTANCE.register(
        Mechano.asResource("perfect_insulator"), () -> Transmitter
            .builder(PerfectInsulator::new)
            .writesToNetwork(null)
            .withAttributes(attr -> { attr
                .withModelType(6)
                .withThickness(0)
                .withMaterial(PhysicalMaterial.AIR)
                .withSounds(Soundscape.AIR)
                .enableInterconnectivity();
            })
            .build()
    );

    public static final TransmitterType<HookupTransmitter> HOOKUP = TransmitterRegistry.INSTANCE.register(
        Mechano.asResource("hookup"), () -> Transmitter
            .builder(HookupTransmitter::new)
            .writesToNetwork(null)
            .withAttributes(attr -> { attr
                .withModelType(0)
                .withThickness(3)
                .withMaterial(PhysicalMaterial.ROPE)
                .withSounds(Soundscape.CABLE);
            })
            .build()
    );

    public static void register(IEventBus modBus) {
        TransmitterRegistry.INSTANCE.register(modBus);
    }
}
