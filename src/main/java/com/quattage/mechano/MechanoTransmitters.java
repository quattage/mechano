package com.quattage.mechano;

import com.quattage.mechano.api.catenary.Catenaries;
import com.quattage.mechano.api.grid.functional.PerfectConductor;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.grid.topology.vertex.Node.Joint;
import com.quattage.mechano.api.transmitter.TransmitterEntry;

import net.neoforged.bus.api.IEventBus;

public class MechanoTransmitters {

    public static final TransmitterEntry<CircuitComponent> HOOKUP = 
        Mechano.REGISTRATE.transmitter("hookup")
            .component((start, end) -> {
                Joint j;
                return new PerfectConductor();
            }).renderer(() -> p -> {
                p.extruder(Catenaries.renderPipeline().SQUARE_EXTRUDER);
                p.thickness(Catenaries.RenderPipeline.Thickness.TRIPLE);
            }).register();

    public static void register(IEventBus modBus) {
        
    }
}
