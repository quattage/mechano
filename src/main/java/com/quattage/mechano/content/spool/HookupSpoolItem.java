package com.quattage.mechano.content.spool;

import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.transmitter.SpoolItem;

public class HookupSpoolItem extends SpoolItem {

    public HookupSpoolItem(Properties properties) {
        super(properties);
    }

    @Override
    public CircuitComponent getComponent() {
        return null;
    }
}
