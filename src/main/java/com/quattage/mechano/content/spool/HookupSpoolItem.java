package com.quattage.mechano.content.spool;

import com.quattage.mechano.MechanoTransmitters;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.transmitter.SpoolItem;
import com.quattage.mechano.api.transmitter.TransmitterType;

public class HookupSpoolItem extends SpoolItem {

    public HookupSpoolItem(Properties properties) {
        super(properties);
    }

    @Override
    public CircuitComponent getComponent() {
        return null;
    }

    @Override
    public TransmitterType<?> getTransmitter() {
        return MechanoTransmitters.HOOKUP.get();
    }
}
