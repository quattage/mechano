package com.quattage.mechano.infrastructure.gametest;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Random;
import java.util.function.BiFunction;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.math.BifrucatedLong;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Mechano.ID)
@PrefixGameTestTemplate(false)
public class ArithmeticTests {

    private static Random testRandom = new Random();
    private static int RAND_SCOPE = 100000;

    @GameTest(template = "empty", batch="arithmeticTests", attempts=64)
    public static void fixedPointConversions(GameTestHelper test) {
        runFixedReferenceTest(test, (a, b) -> a, (a, b) -> a);
    }

    @GameTest(template = "empty", batch="arithmeticTests", attempts=64)
    public static void fixedPointConversionsExact(GameTestHelper test) {
        BigDecimal input = new BigDecimal(random(Integer.MAX_VALUE));
        BifrucatedLong converter = new BifrucatedLong(input.toPlainString());
        BigDecimal output = converter.bigValue();
        if(fuzzyEquals(input, output, BifrucatedLong.EPSILON)) test.succeed();
        else test.fail("expected " + input + ", got " + output);
    }

    @GameTest(template = "empty", batch="arithmeticTests")
    public static void fixedAdd(GameTestHelper test) {
        runFixedReferenceTest(test, (a, b) -> (a.add(b, MathContext.DECIMAL128)), BifrucatedLong::add);
    }

    @GameTest(template = "empty", batch="arithmeticTests", attempts=64)
    public static void fixedSubtract(GameTestHelper test) {
        runFixedReferenceTest(test, (a, b) -> (a.subtract(b, MathContext.DECIMAL128).max(BigDecimal.ZERO)), BifrucatedLong::subtract);
    }

    @GameTest(template = "empty", batch="arithmeticTests", attempts=64)
    public static void fixedSubtractExact(GameTestHelper test) {
        BigDecimal stimulusA = new BigDecimal(random(Integer.MAX_VALUE));
        BigDecimal stimulusB = new BigDecimal(0.00000004d);
        BifrucatedLong testA = new BifrucatedLong(stimulusA);
        BifrucatedLong testB = new BifrucatedLong(stimulusB);
        BigDecimal expected = stimulusA.subtract(stimulusB, MathContext.DECIMAL128);
        BifrucatedLong response = testA.mutableCopy().subtract(testB);
        if(fuzzyEquals(expected, response.bigValue(), BifrucatedLong.EPSILON)) test.succeed();
        else test.fail("expected " + expected + ", got " + response);
    }

    private static void runFixedReferenceTest(GameTestHelper test, BiFunction<BigDecimal, BigDecimal, BigDecimal> stimulus, BiFunction<BifrucatedLong, BifrucatedLong, BifrucatedLong> response) {
        BigDecimal a = new BigDecimal(random()), b = new BigDecimal(random()), parsedValue;
        BifrucatedLong testA = new BifrucatedLong(a.toPlainString());
        BifrucatedLong testB = new BifrucatedLong(b.toPlainString());
        BigDecimal referenceValue = stimulus.apply(a, b);
        BifrucatedLong actualValue = response.apply(testA, testB);
        if(actualValue == null) { test.fail("(" + a + ", " + b + ") Response function returned a null value!"); return; }
        String realValueAsString = actualValue.toString();
        try { parsedValue = new BigDecimal(realValueAsString); } catch(NullPointerException | NumberFormatException e) {
            test.fail("(" + a + ", " + b + ") string parse failure: expected " + String.format("%.2f", referenceValue) + ", got " + realValueAsString);
            return;
        }
        if(!fuzzyEquals(referenceValue, parsedValue, BifrucatedLong.EPSILON)) {
            test.fail("(" + a + ", " + b + ") string value mismatch: expected " + String.format("%.2f", referenceValue) + ", got " + String.format("%.2f", parsedValue));
            return;
        }
        if(!fuzzyEquals(referenceValue, actualValue.bigValue(), BifrucatedLong.EPSILON)) {
            test.fail("(" + a + ", " + b + ") raw value mismatch: expected " + String.format("%.2f", referenceValue) + ", got " + String.format("%.2f", actualValue.doubleValue()));
            return;
        }
        test.succeed();
    }

    private static double random() {
        return random(RAND_SCOPE);
    }
    
    private static double random(double mag) {
        return testRandom.nextDouble() * mag;
    }

    private static boolean fuzzyEquals(BigDecimal a, BigDecimal b, double eps) {
        BigDecimal sub = a.subtract(b).abs();
        return sub.doubleValue() < eps;
    }
}
