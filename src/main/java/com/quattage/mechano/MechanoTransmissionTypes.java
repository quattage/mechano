package com.quattage.mechano;

import com.quattage.mechano.content.spool.HookupTransmitter;
import com.quattage.mechano.foundation.api.transmission.PerfectConductor;
import com.quattage.mechano.foundation.api.transmission.PerfectInsulator;
import com.quattage.mechano.foundation.api.transmission.Transmitter;
import com.quattage.mechano.foundation.api.transmission.TransmitterRegistry;
import com.quattage.mechano.foundation.api.transmission.TransmitterRegistry.TransmitterType;

import net.neoforged.bus.api.IEventBus;

public class MechanoTransmissionTypes {

    public static final TransmitterRegistry REGISTRY = new TransmitterRegistry();

    public static final TransmitterType<PerfectConductor> PERFECT_CONDUCTOR = REGISTRY.register(
        Mechano.asResource("perfect_conductor"), () -> Transmitter
            .builder(PerfectConductor::new)
            .writesToNetwork(null)
            .maximumSpannedDistance(Integer.MAX_VALUE)
            .supportsSameBlockConnections()
            .build()
    );

    public static final TransmitterType<PerfectInsulator> PERFECT_INSULATOR = REGISTRY.register(
        Mechano.asResource("perfect_insulator"), () -> Transmitter
            .builder(PerfectInsulator::new)
            .writesToNetwork(null)
            .maximumSpannedDistance(Integer.MAX_VALUE)
            .supportsSameBlockConnections()
            .build()
    );

    public static final TransmitterType<HookupTransmitter> HOOKUP = REGISTRY.register(
        Mechano.asResource("hookup"), () -> Transmitter
            .builder(HookupTransmitter::new)
            .writesToNetwork(null)
            .maximumSpannedDistance(16)
            .build()
    );


    public static void register(IEventBus modBus) {
        REGISTRY.register(modBus);
    }
}
