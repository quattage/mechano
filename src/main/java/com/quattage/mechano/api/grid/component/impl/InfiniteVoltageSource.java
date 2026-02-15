package com.quattage.mechano.api.grid.component.impl;

import com.quattage.mechano.api.grid.VoltageDecay;

public class InfiniteVoltageSource extends VoltageSource {

    public InfiniteVoltageSource(float defaultVolts) {
        super("InfiniteVoltageSource", new VoltageDecay.Constant(defaultVolts));
    }

    public void setVolts(float volts) {
        ((VoltageDecay.Constant)decayFunction).setVoltage(volts);
    }

    @Override
    public double getStateOfCharge() {
        return 1;
    }
}
