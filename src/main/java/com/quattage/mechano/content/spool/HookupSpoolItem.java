package com.quattage.mechano.content.spool;

import com.quattage.mechano.foundation.gridapi.transmitter.MechanoTransmissionTypes;
import com.quattage.mechano.foundation.gridapi.transmitter.TransmitterRegistry.TransmitterType;
import com.quattage.mechano.foundation.item.SpoolItem;

public class HookupSpoolItem extends SpoolItem<HookupTransmitter> {

    public HookupSpoolItem(Properties properties) {
        super(properties);
    }

    @Override
    public TransmitterType<HookupTransmitter> getTransmitterType() {
        return MechanoTransmissionTypes.HOOKUP;
    }
}
