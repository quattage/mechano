package com.quattage.mechano.grid.component;

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
