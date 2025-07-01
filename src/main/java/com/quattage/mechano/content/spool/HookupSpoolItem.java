package com.quattage.mechano.content.spool;

import com.quattage.mechano.foundation.SpoolItem;
import com.quattage.mechano.foundation.api.transmitter.MechanoTransmissionTypes;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;

public class HookupSpoolItem extends SpoolItem<HookupTransmitter> {

    public HookupSpoolItem(Properties properties) {
        super(properties);
    }

    @Override
    public TransmitterType<HookupTransmitter> getTransmitterType() {
        return MechanoTransmissionTypes.HOOKUP;
    }
}
