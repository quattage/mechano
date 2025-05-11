package com.quattage.mechano.content.spool;

import com.quattage.mechano.MechanoTransmissionTypes;
import com.quattage.mechano.foundation.api.WireSpoolItem;
import com.quattage.mechano.foundation.api.transmission.TransmitterRegistry.TransmitterType;

public class HookupWireSpoolItem extends WireSpoolItem<HookupTransmitter> {

    public HookupWireSpoolItem(Properties properties) {
        super(properties);
    }

    @Override
    public TransmitterType<HookupTransmitter> getTransmitterType() {
        return MechanoTransmissionTypes.HOOKUP;
    }
}
