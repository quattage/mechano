package com.quattage.mechano.content.spool;

import com.quattage.mechano.api.item.SpoolItem;
import com.quattage.mechano.api.transmitter.MechanoTransmissionTypes;
import com.quattage.mechano.api.transmitter.TransmitterType;

public class HookupSpoolItem extends SpoolItem<HookupTransmitter> {

    public HookupSpoolItem(Properties properties) {
        super(properties);
    }

    @Override
    public TransmitterType<HookupTransmitter> getTransmitterType() {
        return MechanoTransmissionTypes.HOOKUP;
    }
}
