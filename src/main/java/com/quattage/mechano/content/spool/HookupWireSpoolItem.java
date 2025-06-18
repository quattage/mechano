package com.quattage.mechano.content.spool;

import com.quattage.mechano.foundation.WireSpoolItem;
import com.quattage.mechano.foundation.api.transmitter.MechanoTransmissionTypes;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;

public class HookupWireSpoolItem extends WireSpoolItem<HookupTransmitter> {

    public HookupWireSpoolItem(Properties properties) {
        super(properties);
    }

    @Override
    public TransmitterType<HookupTransmitter> getTransmitterType() {
        return MechanoTransmissionTypes.HOOKUP;
    }
}
