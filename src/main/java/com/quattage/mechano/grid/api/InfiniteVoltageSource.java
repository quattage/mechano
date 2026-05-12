package com.quattage.mechano.grid.api;

import com.quattage.mechano.grid.VoltageDecay;

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
