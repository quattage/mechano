package com.quattage.mechano.infrastructure.gametest.tests;

import java.math.BigDecimal;
import java.math.MathContext;

import com.quattage.mechano.foundation.numeric.Bifrucated64;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTestHelper;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTests.MechanoTestHolder;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTests.Repeat;

import net.minecraft.gametest.framework.GameTest;

@MechanoTestHolder
public class ArithmeticTests {

    @GameTest
    public static void fixedPointConversions(MechanoGameTestHelper test) {
        test.checkNumeric((a, b) -> a, (a, b) -> a);
        test.succeed();
    }

    @GameTest
    public static void fixedPointConversionsExact(MechanoGameTestHelper test) {
        BigDecimal input = new BigDecimal(test.random(Integer.MAX_VALUE));
        Bifrucated64 converter = new Bifrucated64(input.toPlainString());
        BigDecimal output = converter.bigValue();
        test.assertFuzzyEquals(input, output, Bifrucated64.EPSILON);
        test.succeed();
    }

    @GameTest
    public static void fixedAdd(MechanoGameTestHelper test) {
        test.checkNumeric((a, b) -> (a.add(b, MathContext.DECIMAL128)), Bifrucated64::add);
        test.succeed();
    }

    @GameTest
    public static void fixedSubtract(MechanoGameTestHelper test) {
        test.checkNumeric((a, b) -> (a.subtract(b, MathContext.DECIMAL128).max(BigDecimal.ZERO)), Bifrucated64::subtract);
        test.succeed();
    }

    @GameTest
    @Repeat(iterations = 64)
    public static void fixedSubtractExact(MechanoGameTestHelper test) {
        BigDecimal stimulusA = new BigDecimal(test.random(Integer.MAX_VALUE));
        BigDecimal stimulusB = new BigDecimal(0.00000004d);
        Bifrucated64 testA = new Bifrucated64(stimulusA);
        Bifrucated64 testB = new Bifrucated64(stimulusB);
        BigDecimal expected = stimulusA.subtract(stimulusB, MathContext.DECIMAL128);
        Bifrucated64 response = testA.mutableCopy().subtract(testB);
        test.assertFuzzyEquals(expected, response.bigValue(), Bifrucated64.EPSILON);
        test.succeed();
    }
}
