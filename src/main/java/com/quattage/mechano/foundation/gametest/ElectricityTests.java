package com.quattage.mechano.foundation.gametest;

import java.io.File;
import java.io.PrintWriter;
import java.util.Random;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.watt.DoubleSigmoidVoltageCurve;
import com.quattage.mechano.foundation.api.watt.Joule;
import com.quattage.mechano.foundation.api.watt.Voltage;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Mechano.ID)
@PrefixGameTestTemplate(false)
public class ElectricityTests {

    private static Random testRandom = new Random();
    private static int RAND_SCOPE = 10000;
    private static float ASSERT_EPSILON = 0.1f;

    @GameTest(template = "empty", batch="electricityTests")
    public static void jouleIntegrity(GameTestHelper test) {
        test.succeedIf(() -> {
            double r1 = random();
            String fRR1 = String.format("%.3f", r1);
            Joule tJ = new Joule(r1);
            double pJV = 0;
            try {
                pJV = Double.parseDouble(tJ.toString());
            } catch(Exception e) {
                test.fail("Couldn't parse double from returned string '" + tJ.toString() + "', expected" + r1);
                return;
            }
            test.assertTrue(ElectricityTests.approximately(r1, pJV), "Integrity for (" + fRR1 + ") failed! - Got " + pJV);
        });
    }

    @GameTest(template = "empty", batch="electricityTests")
    public static void jouleAddOperation(GameTestHelper test) {
        double r1 = random();
        double r2 = random();
        Joule tJ1 = new Joule(r1);
        Joule tJ2 = new Joule(r2);

        double try1 = 0, try2 = 0;

        try {
            try1 = Double.parseDouble(tJ1.toString());
        } catch(Exception e) {
            test.fail("Couldn't parse double from returned string '" + tJ1.toString() + "', expected approx. " + String.format("%.3f", r2));
            return;
        }

        try {
            try2 = Double.parseDouble(tJ2.toString());
        } catch(Exception e) {
            test.fail("Couldn't parse double from returned string '" + tJ2.toString() + "', expected approx. " + String.format("%.3f", r2));
            return;
        }

        double refAdd = try1 + try2;
        String fRRA = String.format("%.3f", refAdd);
        Joule jAdd = Joule.add(tJ1, tJ2);
        
        double parsed = 0;

        try {
            parsed = Double.parseDouble(jAdd.toString());   
        } catch(Exception e) {
            test.fail("Couldn't parse resulting double from '" + jAdd.toString() + "', expected approx. " + fRRA);
            return;
        }

        if(ElectricityTests.approximately(parsed, refAdd)) {
            test.succeed();
            return;
        }
        test.fail("Operation (" + tJ1 + " + " + tJ2 + "): expected " + fRRA + ", got " + jAdd.toString());
    }

    @GameTest(template = "empty", batch="electricityTests")
    public static void jouleSubtractOperation(GameTestHelper test) {
        double r1 = random();
        double r2 = random();
        Joule tJ1 = new Joule(r1);
        Joule tJ2 = new Joule(r2);

        double try1 = 0, try2 = 0;

        try {
            try1 = Double.parseDouble(tJ1.toString());
        } catch(Exception e) {
            test.fail("Couldn't parse double from returned string '" + tJ1.toString() + "', expected approx. " + String.format("%.3f", r1));
            return;
        }

        try {
            try2 = Double.parseDouble(tJ2.toString());
        } catch(Exception e) {
            test.fail("Couldn't parse double from returned string '" + tJ2.toString() + "', expected approx. " + String.format("%.3f", r2));
            return;
        }

        double refSubtract = try1 - try2;
        String fRRA = String.format("%.3f", refSubtract);
        Joule jSubtract = Joule.subtract(tJ1, tJ2);
        double parsed = 0;

        try {
            parsed = Double.parseDouble(jSubtract.toString());   
        } catch(Exception e) {
            test.fail("Couldn't parse resulting double from '" + jSubtract.toString() + "', expected approx. " + fRRA);
            return;
        }

        if(ElectricityTests.approximately(parsed, refSubtract)) {
            test.succeed();
            return;
        }
        test.fail("Operation (" + tJ1 + " - " + tJ2 + "): expected " + fRRA + ", got " + jSubtract.toString());
    }

    @GameTest(template = "empty", batch="electricityTests")
    public static void voltIntegrity(GameTestHelper test) {
        int r1 = (int)Math.round(random() / 1000);
        int expected = Voltage.getNearest(r1);
        Voltage tV1 = new Voltage(r1);
        int parsed = Integer.parseInt(tV1.toString());
        if(expected == parsed) {
            test.succeed();
            return;
        }
        test.fail("Packing (" + r1 + "): expected " + expected + ", got " + tV1);
    }

    @GameTest(template = "empty", batch="electricityTests")
    public static void voltAddOperation(GameTestHelper test) {
        int r1 = (int)Math.round(random() / 1000);
        int r2 = (int)Math.round(random() / 1000);
        int r1c = Voltage.getNearest(r1);
        int r2c = Voltage.getNearest(r2);
        int expected = r1c + r2c;
        Voltage tv1 = new Voltage(r1);
        Voltage tv2 = new Voltage(r2);
        Voltage vAdd = Voltage.add(tv1, tv2);
        int parsed = Integer.parseInt(vAdd.toString());
        if(expected == parsed) {
            test.succeed();
            return;
        }
        if(expected - 4 == parsed) {
            test.succeed();
            return;
        }
        if(expected + 4 == parsed) {
            test.succeed();
            return;
        }
        test.fail("Operation (" + r1 + " + " + r2 + "): expected " + expected + ", got " + parsed);
    }

    @GameTest(template = "empty", batch="electricityTests")
    public static void voltSubtractOperation(GameTestHelper test) {
        int r1 = (int)Math.round(random() / 1000);
        int r2 = (int)Math.round(random() / 1000);
        int r1c = Voltage.getNearest(r1);
        int r2c = Voltage.getNearest(r2);
        int expected = r1c - r2c;
        Voltage tv1 = new Voltage(r1);
        Voltage tv2 = new Voltage(r2);
        Voltage vSubtract = Voltage.subtract(tv1, tv2);
        int parsed = Integer.parseInt(vSubtract.toString());
        if(expected == parsed) {
            test.succeed();
            return;
        }

        // Rounding by way of integer truncation occurs towards negative infinity, 
        // rather than towards zero. Sometimes the unit test can round up but the 
        // voltage constructor rounds down. this is fine, since 90% of the time 
        // voltage will be supplied with a multiple of 4 anyway, but may be a
        // problem in the future when dealing with voltage conversions.
        if(expected - 4 == parsed) {
            test.succeed();
            return;
        }
        if(expected + 4 == parsed) {
            test.succeed();
            return;
        }
        test.fail("Operation (" + r1 + " - " + r2 + "): expected " + expected + ", got " + parsed);
    }

    // https://webutility.io/csv-to-chart-online
    @GameTest(template = "empty", batch="electricityTests")
    public static void voltSigmoidDump(GameTestHelper test) {
        Voltage[] voltages = DoubleSigmoidVoltageCurve.DEFAULT.getPrecomputed(256);
        String output = "%,vA\n";
        for(int x = 0; x < voltages.length; x++) {
            Voltage volt = voltages[x];
            output += x + "," + volt.get() + "\n";
        }
        File outputFile = new File("./default_voltage_decay.csv");
        try(PrintWriter pw = new PrintWriter(outputFile)) {
            pw.println(output);
        } catch (Exception e) {
            test.fail("Couldn't write to csv!");
            return;
        }
        test.succeed();
    }

    private static double random() {
        return testRandom.nextDouble() * RAND_SCOPE - (RAND_SCOPE / 2f);
    }

    private static boolean approximately(double a, double b) {
        return Math.abs(a - b) < ASSERT_EPSILON;
    }
}
