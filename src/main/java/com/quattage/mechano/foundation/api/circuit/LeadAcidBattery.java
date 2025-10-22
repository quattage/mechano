package com.quattage.mechano.foundation.api.circuit;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.math.BifrucatedLong;

public class LeadAcidBattery extends EnergyStore {

    public LeadAcidBattery() {
        super();
    }

    @Override
    public @Nullable BatteryDatasheet getDatasheet() {
        return BatteryDatasheet.DEFAULT;
    }

    @Override
    public Watt charge(Watt power) {
        BatteryDatasheet data = getDatasheetSafe();
        if(power.isZero()) return Watt.zero();
        double soc = getStateOfCharge();
        if(soc > 1) {
            // overcharge;
        }
        return Watt.zero();
    }

    @Override
    public Watt discharge(double current) {
        BatteryDatasheet data = getDatasheetSafe();
        if(current <= BifrucatedLong.EPSILON) return Watt.zero();
        current = Math.min(current, data.getMaxDischargeCurrent());
        internalCharge.subtract(current * DELTA);
        double soc = getStateOfCharge();
        double ocv = data.getVoltageCurve().apply(soc);
        double realVoltage = Math.max(0, ocv - current * data.getInternalResistance());
        if(soc < 0.1d) {
            float falloff = (1.0f - (float)Math.exp(-10f * (float)soc / 0.2f)) / (1f - (float)Math.exp(-10d));
            current *= Math.max(0.4, falloff);
        }
        return new Watt(realVoltage, current);
    }
}
