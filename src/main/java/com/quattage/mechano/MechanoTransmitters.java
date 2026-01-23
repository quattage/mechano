
package com.quattage.mechano;

import com.quattage.mechano.api.catenary.Catenaries;
import com.quattage.mechano.api.transmitter.TransmitterEntry;
import com.quattage.mechano.api.transmitter.TransmitterType.UnionFactory;

import net.neoforged.bus.api.IEventBus;

public class MechanoTransmitters {

    public static final TransmitterEntry HOOKUP = 
        Mechano.REGISTRATE.transmitter("hookup")
            .functionsAs(UnionFactory::perfectConductor)
            .soundsLike(Catenaries.Soundscape.CABLE)
            .feelsLike(Catenaries.PhysicalMaterial.ROPE)
            .renderer(() -> p -> p
                .extruder(Catenaries.renderPipeline().SQUARE_EXTRUDER)
                .thickness(Catenaries.RenderPipeline.Thickness.TRIPLE))
            .register();

    public static void register(IEventBus modBus) {
        
    }
}
