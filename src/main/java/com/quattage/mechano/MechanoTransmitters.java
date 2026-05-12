
package com.quattage.mechano;

import com.quattage.mechano.api.transmitter.TransmitterEntry;
import com.quattage.mechano.api.transmitter.TransmitterType.UnionFactory;
import com.quattage.mechano.catenary.Catenaries;

import net.neoforged.bus.api.IEventBus;

public class MechanoTransmitters {

    public static final TransmitterEntry HOOKUP = 
        Mechano.REGISTRATE.transmitter("hookup")
            .whenCreated(UnionFactory::perfectConductor)
            .soundsLike(Catenaries.Soundscape.CABLE)
            .feelsLike(Catenaries.PhysicalMaterial.ROPE)
            .renderer(() -> p -> p
                .extruder(Catenaries.renderPipeline().SQUARE_EXTRUDER)
                .thickness(Catenaries.RenderPipeline.Thickness.TRIPLE)
                .material(Catenaries.renderPipeline().SOLID_MATERIAL))
            .register();

    public static final TransmitterEntry PERFECT_CONDUCTOR = 
        Mechano.REGISTRATE.transmitter("perfect_conductor")
            .whenCreated(UnionFactory::perfectConductor)
            .soundsLike(Catenaries.Soundscape.AIR)
            .feelsLike(Catenaries.PhysicalMaterial.AIR)
            .renderer(() -> p -> p
                .extruder(Catenaries.renderPipeline().INVISIBLE_EXTRUDER)
                .thickness(Catenaries.RenderPipeline.Thickness.ZERO)
                .material(Catenaries.renderPipeline().SOLID_MATERIAL))
            .register();

    public static void register(IEventBus modBus) {
        
    }
}
