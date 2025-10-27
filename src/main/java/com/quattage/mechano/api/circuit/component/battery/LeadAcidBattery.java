package com.quattage.mechano.api.circuit.component.battery;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.circuit.Watt;
import com.quattage.mechano.api.circuit.topology.CircuitComponent;
import com.quattage.mechano.api.griddable.Griddable;
import com.quattage.mechano.foundation.numeric.Bifrucated64;

public class LeadAcidBattery extends Battery {

    // peukert min, peukert max, and an arbitrary reisistive falloff
    protected static final float[] FACMAP = new float[] {0.15f, 1.2f, 1.0f};

    public LeadAcidBattery() {
        super("LeadAcidBattery");
    }

    @Override
    public @Nullable BatteryDatasheet getDatasheet() {
        return BatteryDatasheet.DEFAULT;
    }

    @Override
    public Watt charge(Watt power) {
        if(power.isZero()) return Watt.zero();
        final BatteryDatasheet data = getDatasheetSafe();
        double wattsIn = power.get();
        double soc = getStateOfCharge();
        double ocv = data.getVoltageLUT().apply(soc);
        double maxVolts = data.getVoltageLUT().nominal();
        double resistance = data.getInternalResistance();

        double cmax = data.getMaxChargeCurrent();
        double imax = Math.min(Math.max(0, (resistance <= 1e-12 
            ? wattsIn / Math.max(ocv, 1e-6) 
            : (-ocv + Math.sqrt(Math.max(0,  ocv * ocv + 4 * resistance * wattsIn))) / (2 * resistance))), cmax);
        double vt = ocv + imax * resistance;

        double etaRate = 1d / (1d + FACMAP[0] * Math.pow(imax / Math.max(cmax, 1e-6), FACMAP[1]));
        double alpha = 1d - (soc * soc * soc * soc);

        double etaOv = (vt <= maxVolts) ? 1d : Math.max(0d, 1d - FACMAP[2] * ((vt - maxVolts) / maxVolts));
        double etaEff = Math.max(0d, Math.min(1d, data.getChargeEfficiency() * etaRate * alpha * etaOv));
        double deltaAmps = imax * etaEff;
        internalCharge.add(deltaAmps * CircuitComponent.DELTA_AH);
        return new Watt(vt, deltaAmps);
    }

    @Override
    public Watt discharge(double current) {
        if(current <= Bifrucated64.EPSILON) return Watt.zero();
        final BatteryDatasheet data = getDatasheetSafe();
        current = Math.min(current, data.getMaxDischargeCurrent());
        double soc = getStateOfCharge();
        double ocv = data.getVoltageLUT().apply(soc);
        double realVoltage = Math.max(0, ocv - current * data.getInternalResistance());
        if(soc < 0.1d) {
            float falloff = (1.0f - (float)Math.exp(-60f * (float)soc)) / (1f - (float)Math.exp(-10d));
            current *= Math.max(0.001, falloff);
        }
        internalCharge.subtract(current * CircuitComponent.DELTA_AH);
        return new Watt(realVoltage, current);
    }

    @Override
    public void tick(Griddable<?> host) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'tick'");
    }
}
