package com.quattage.mechano.infrastructure.gametest.tests;

import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTestHelper;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTests.MechanoTestHolder;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.world.level.block.Rotation;

@MechanoTestHolder
public class StateIntegrityTests {

    @GameTest
    public static void checkOrientations(MechanoGameTestHelper test) {
        String result = "";

        result += StateIntegrityTests.test(CombinedOrientation.NORTH_UP, Rotation.CLOCKWISE_90, CombinedOrientation.EAST_UP);
        result += StateIntegrityTests.test(CombinedOrientation.EAST_UP, Rotation.CLOCKWISE_90, CombinedOrientation.SOUTH_UP);
        result += StateIntegrityTests.test(CombinedOrientation.SOUTH_UP, Rotation.CLOCKWISE_90, CombinedOrientation.WEST_UP);
        result += StateIntegrityTests.test(CombinedOrientation.WEST_UP, Rotation.CLOCKWISE_90, CombinedOrientation.NORTH_UP);
        
        result += StateIntegrityTests.test(CombinedOrientation.UP_NORTH, Rotation.CLOCKWISE_90, CombinedOrientation.UP_EAST);
        result += StateIntegrityTests.test(CombinedOrientation.UP_EAST, Rotation.CLOCKWISE_90, CombinedOrientation.UP_SOUTH);
        result += StateIntegrityTests.test(CombinedOrientation.UP_SOUTH, Rotation.CLOCKWISE_90, CombinedOrientation.UP_WEST);
        result += StateIntegrityTests.test(CombinedOrientation.UP_WEST, Rotation.CLOCKWISE_90, CombinedOrientation.UP_NORTH);
        
        result += StateIntegrityTests.test(CombinedOrientation.DOWN_NORTH, Rotation.CLOCKWISE_90, CombinedOrientation.DOWN_EAST);
        result += StateIntegrityTests.test(CombinedOrientation.DOWN_EAST, Rotation.CLOCKWISE_90, CombinedOrientation.DOWN_SOUTH);
        result += StateIntegrityTests.test(CombinedOrientation.DOWN_SOUTH, Rotation.CLOCKWISE_90, CombinedOrientation.DOWN_WEST);
        result += StateIntegrityTests.test(CombinedOrientation.DOWN_WEST, Rotation.CLOCKWISE_90, CombinedOrientation.DOWN_NORTH);

        result += StateIntegrityTests.test(CombinedOrientation.NORTH_UP, Rotation.COUNTERCLOCKWISE_90, CombinedOrientation.WEST_UP);
        result += StateIntegrityTests.test(CombinedOrientation.EAST_UP, Rotation.COUNTERCLOCKWISE_90, CombinedOrientation.NORTH_UP);
        result += StateIntegrityTests.test(CombinedOrientation.SOUTH_UP, Rotation.COUNTERCLOCKWISE_90, CombinedOrientation.EAST_UP);
        result += StateIntegrityTests.test(CombinedOrientation.WEST_UP, Rotation.COUNTERCLOCKWISE_90, CombinedOrientation.SOUTH_UP);

        result += StateIntegrityTests.test(CombinedOrientation.UP_NORTH, Rotation.COUNTERCLOCKWISE_90, CombinedOrientation.UP_WEST);
        result += StateIntegrityTests.test(CombinedOrientation.UP_EAST, Rotation.COUNTERCLOCKWISE_90, CombinedOrientation.UP_NORTH);
        result += StateIntegrityTests.test(CombinedOrientation.UP_SOUTH, Rotation.COUNTERCLOCKWISE_90, CombinedOrientation.UP_EAST);
        result += StateIntegrityTests.test(CombinedOrientation.UP_WEST, Rotation.COUNTERCLOCKWISE_90, CombinedOrientation.UP_SOUTH);
        
        result += StateIntegrityTests.test(CombinedOrientation.DOWN_NORTH, Rotation.COUNTERCLOCKWISE_90, CombinedOrientation.DOWN_WEST);
        result += StateIntegrityTests.test(CombinedOrientation.DOWN_EAST, Rotation.COUNTERCLOCKWISE_90, CombinedOrientation.DOWN_NORTH);
        result += StateIntegrityTests.test(CombinedOrientation.DOWN_SOUTH, Rotation.COUNTERCLOCKWISE_90, CombinedOrientation.DOWN_EAST);
        result += StateIntegrityTests.test(CombinedOrientation.DOWN_WEST, Rotation.COUNTERCLOCKWISE_90, CombinedOrientation.DOWN_SOUTH);

        result += StateIntegrityTests.test(CombinedOrientation.NORTH_UP, Rotation.CLOCKWISE_180, CombinedOrientation.SOUTH_UP);
        result += StateIntegrityTests.test(CombinedOrientation.EAST_UP, Rotation.CLOCKWISE_180, CombinedOrientation.WEST_UP);
        result += StateIntegrityTests.test(CombinedOrientation.SOUTH_UP, Rotation.CLOCKWISE_180, CombinedOrientation.NORTH_UP);
        result += StateIntegrityTests.test(CombinedOrientation.WEST_UP, Rotation.CLOCKWISE_180, CombinedOrientation.EAST_UP);
        
        result += StateIntegrityTests.test(CombinedOrientation.UP_NORTH, Rotation.CLOCKWISE_180, CombinedOrientation.UP_SOUTH);
        result += StateIntegrityTests.test(CombinedOrientation.UP_EAST, Rotation.CLOCKWISE_180, CombinedOrientation.UP_WEST);
        result += StateIntegrityTests.test(CombinedOrientation.UP_SOUTH, Rotation.CLOCKWISE_180, CombinedOrientation.UP_NORTH);
        result += StateIntegrityTests.test(CombinedOrientation.UP_WEST, Rotation.CLOCKWISE_180, CombinedOrientation.UP_EAST);
        
        result += StateIntegrityTests.test(CombinedOrientation.DOWN_NORTH, Rotation.CLOCKWISE_180, CombinedOrientation.DOWN_SOUTH);
        result += StateIntegrityTests.test(CombinedOrientation.DOWN_EAST, Rotation.CLOCKWISE_180, CombinedOrientation.DOWN_WEST);
        result += StateIntegrityTests.test(CombinedOrientation.DOWN_SOUTH, Rotation.CLOCKWISE_180, CombinedOrientation.DOWN_NORTH);
        result += StateIntegrityTests.test(CombinedOrientation.DOWN_WEST, Rotation.CLOCKWISE_180, CombinedOrientation.DOWN_EAST);

        if(result.isEmpty()) test.succeed();
        else test.fail("Orientation failed integrity test: \n" + result);
    }

    private static String test(CombinedOrientation orient, Rotation rotation, CombinedOrientation expected) {
        CombinedOrientation applied = orient.applyRotation(rotation);
        if(applied != expected) return "\t" + orient + " -> " + rotation + " -> " + applied + ", expected " + expected + "\n";
        return "";
    }
}
