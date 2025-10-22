package com.quattage.mechano.infrastructure.gametest;

import java.io.File;
import java.io.PrintWriter;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.circuit.BatteryDatasheet;
import com.quattage.mechano.foundation.api.circuit.EnergyStore;
import com.quattage.mechano.foundation.api.circuit.LeadAcidBattery;
import com.quattage.mechano.foundation.api.circuit.VoltageDecay;
import com.quattage.mechano.foundation.api.circuit.Watt;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Mechano.ID)
@PrefixGameTestTemplate(false)
public class ElectricityTests {
    
    private static final int sampleRes = 250;
    private static final BatteryDatasheet DEF_LUT =  BatteryDatasheet.DEFAULT.copyWith(VoltageDecay.LUT::new);
    private static final BatteryDatasheet DEF_STEPPED =  BatteryDatasheet.DEFAULT.copyWith(VoltageDecay.SteppedLUT::new);
    private static final BatteryDatasheet DEF_CUBIC =  BatteryDatasheet.DEFAULT.copyWith(VoltageDecay.CubicLUT::new);

    @GameTest(template = "empty", batch="electricityTests")
    public static void dumpDecayFunctions(GameTestHelper test) {
        String csv = "SoC,vA,vB,vC,vD\n";
        for(int x = 0; x < sampleRes; x++) {
            float t = (float)x / (float)sampleRes;
            double vA = DEF_LUT.getExpectedVoltage(t);
            double vB = BatteryDatasheet.DEFAULT.getExpectedVoltage(t);
            double vC = DEF_STEPPED.getExpectedVoltage(t);
            double vD = DEF_CUBIC.getExpectedVoltage(t);
            csv += String.format("%.6f", t) + "," + String.format("%.6f", vA) + "," + String.format("%.6f", vB) + "," + String.format("%.6f", vC) + "," + String.format("%.6f", vD) + "\n";
        }
        dumpCSV(test, "default_voltage_decay", csv);
    }

    @GameTest(template = "empty", batch="electricityTests")
    public static void dumpLeadAcidVoC(GameTestHelper test) {
        runBatteryCycleTest("discharge", () -> {
            LeadAcidBattery battery = new LeadAcidBattery();
            return battery.fillEnergy();
        }, battery -> {
            return battery.discharge(40);
        }, test);
    }

    private static void runBatteryCycleTest(String actionName, Supplier<EnergyStore> batterySupplier, Function<EnergyStore, @Nullable Watt> action, GameTestHelper test) {
        String csv = "Tick,SoC,OCV,VTerm,Amps,Charge\n";
        EnergyStore battery = batterySupplier.get();
        battery.fillEnergy();

        // sample (a little bit more than) a 1 hour battery cycle
        int res = 50;
        for(int x = 0; x < 72800; x++) {
            if(battery.isEmpty()) break;
            double soc = battery.getStateOfCharge();
            double ocv = battery.getDatasheet().getExpectedVoltage(soc);
            Watt delivered = action.apply(battery);
            if(delivered == null) {
                test.fail("battery cycle test at sample " + x + " for EnergyStore of type '" + battery.getClass().getSimpleName() + "' produced a null watt value!");
                return;
            }
            // 72000 samples is too dense for excel
            if(x % res == 0) {
                double vterm = delivered.getVoltage();
                double power = delivered.get();
                double current = power / vterm;
                csv += x + "," + String.format("%.6f", soc) + "," + String.format("%.6f", ocv) + "," + String.format("%.6f", vterm) + "," + String.format("%.6f", current) + "," + battery.getStoredCharge().toString() + "\n";
            }
            // if(battery.isFull()) break;
        }
        dumpCSV(test, battery.getClass().getSimpleName().toLowerCase() + "_" + actionName, csv);
    }




    private static void dumpCSV(GameTestHelper test, String filename, String contents) {
        File outputFile = new File("./dumps/" + filename + ".csv");
        try(PrintWriter pw = new PrintWriter(outputFile)) { pw.println(contents.trim()); } catch (Exception e) {
            test.fail("Exception encountered while dumping '" + filename + ".csv' :");
            e.printStackTrace();
            return;
        }
        Mechano.LOGGER.debug("Dumped file '" + filename + ".csv'");
        test.succeed();
    }

}
