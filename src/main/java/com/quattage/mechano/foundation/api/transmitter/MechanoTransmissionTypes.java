package com.quattage.mechano.foundation.api.transmitter;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.spool.HookupTransmitter;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes.ModelType;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes.Tension;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes.Thickness;

import net.neoforged.bus.api.IEventBus;

public class MechanoTransmissionTypes {

    public static final TransmitterType<PerfectConductor> PERFECT_CONDUCTOR = TransmitterRegistry.INSTANCE.register(
        Mechano.asResource("perfect_conductor"), () -> Transmitter
            .builder(PerfectConductor::new)
            .writesToNetwork(null)
            .maximumSpannedDistance(Integer.MAX_VALUE)
            .supportsSameBlockConnections()
            .withAttributes(CatenaryAttributes.INVISIBLE)
            .build()
    );

    public static final TransmitterType<PerfectInsulator> PERFECT_INSULATOR = TransmitterRegistry.INSTANCE.register(
        Mechano.asResource("perfect_insulator"), () -> Transmitter
            .builder(PerfectInsulator::new)
            .writesToNetwork(null)
            .maximumSpannedDistance(Integer.MAX_VALUE)
            .supportsSameBlockConnections()
            .withAttributes(CatenaryAttributes.INVISIBLE)
            .build()
    );

    public static final TransmitterType<HookupTransmitter> HOOKUP = TransmitterRegistry.INSTANCE.register(
        Mechano.asResource("hookup"), () -> Transmitter
            .builder(HookupTransmitter::new)
            .writesToNetwork(null)
            .maximumSpannedDistance(32)
            .withAttributes(CatenaryAttributes
                .as(ModelType.SQUARE)
                .withTension(Tension.AVERAGE)
                .withThickness(Thickness.TRIPLE)
            ).build()
    );

    public static void register(IEventBus modBus) {
        TransmitterRegistry.INSTANCE.register(modBus);
    }
}
