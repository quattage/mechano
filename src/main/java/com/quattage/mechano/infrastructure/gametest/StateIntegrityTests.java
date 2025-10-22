package com.quattage.mechano.infrastructure.gametest;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Mechano.ID)
@PrefixGameTestTemplate(false)
public class StateIntegrityTests {


    @GameTest(template = "empty", batch="stateIntegrityTests")
    public static void checkOrientations(GameTestHelper test) {
        String result = "";

        result += test(CombinedOrientation.NORTH_UP, Rotation.CLOCKWISE_90, CombinedOrientation.EAST_UP);
        result += test(CombinedOrientation.EAST_UP, Rotation.CLOCKWISE_90, CombinedOrientation.SOUTH_UP);
        result += test(CombinedOrientation.SOUTH_UP, Rotation.CLOCKWISE_90, CombinedOrientation.WEST_UP);
        result += test(CombinedOrientation.WEST_UP, Rotation.CLOCKWISE_90, CombinedOrientation.NORTH_UP);
        
        result += test(CombinedOrientation.UP_NORTH, Rotation.CLOCKWISE_90, CombinedOrientation.UP_EAST);
        result += test(CombinedOrientation.UP_EAST, Rotation.CLOCKWISE_90, CombinedOrientation.UP_SOUTH);
        result += test(CombinedOrientation.UP_SOUTH, Rotation.CLOCKWISE_90, CombinedOrientation.UP_WEST);
        result += test(CombinedOrientation.UP_WEST, Rotation.CLOCKWISE_90, CombinedOrientation.UP_NORTH);
        
        result += test(CombinedOrientation.DOWN_NORTH, Rotation.CLOCKWISE_90, CombinedOrientation.DOWN_EAST);
        result += test(CombinedOrientation.DOWN_EAST, Rotation.CLOCKWISE_90, CombinedOrientation.DOWN_SOUTH);
        result += test(CombinedOrientation.DOWN_SOUTH, Rotation.CLOCKWISE_90, CombinedOrientation.DOWN_WEST);
        result += test(CombinedOrientation.DOWN_WEST, Rotation.CLOCKWISE_90, CombinedOrientation.DOWN_NORTH);

        result += test(CombinedOrientation.NORTH_UP, Rotation.COUNTERCLOCKWISE_90, CombinedOrientation.WEST_UP);
        result += test(CombinedOrientation.EAST_UP, Rotation.COUNTERCLOCKWISE_90, CombinedOrientation.NORTH_UP);
        result += test(CombinedOrientation.SOUTH_UP, Rotation.COUNTERCLOCKWISE_90, CombinedOrientation.EAST_UP);
        result += test(CombinedOrientation.WEST_UP, Rotation.COUNTERCLOCKWISE_90, CombinedOrientation.SOUTH_UP);

        result += test(CombinedOrientation.UP_NORTH, Rotation.COUNTERCLOCKWISE_90, CombinedOrientation.UP_WEST);
        result += test(CombinedOrientation.UP_EAST, Rotation.COUNTERCLOCKWISE_90, CombinedOrientation.UP_NORTH);
        result += test(CombinedOrientation.UP_SOUTH, Rotation.COUNTERCLOCKWISE_90, CombinedOrientation.UP_EAST);
        result += test(CombinedOrientation.UP_WEST, Rotation.COUNTERCLOCKWISE_90, CombinedOrientation.UP_SOUTH);
        
        result += test(CombinedOrientation.DOWN_NORTH, Rotation.COUNTERCLOCKWISE_90, CombinedOrientation.DOWN_WEST);
        result += test(CombinedOrientation.DOWN_EAST, Rotation.COUNTERCLOCKWISE_90, CombinedOrientation.DOWN_NORTH);
        result += test(CombinedOrientation.DOWN_SOUTH, Rotation.COUNTERCLOCKWISE_90, CombinedOrientation.DOWN_EAST);
        result += test(CombinedOrientation.DOWN_WEST, Rotation.COUNTERCLOCKWISE_90, CombinedOrientation.DOWN_SOUTH);

        result += test(CombinedOrientation.NORTH_UP, Rotation.CLOCKWISE_180, CombinedOrientation.SOUTH_UP);
        result += test(CombinedOrientation.EAST_UP, Rotation.CLOCKWISE_180, CombinedOrientation.WEST_UP);
        result += test(CombinedOrientation.SOUTH_UP, Rotation.CLOCKWISE_180, CombinedOrientation.NORTH_UP);
        result += test(CombinedOrientation.WEST_UP, Rotation.CLOCKWISE_180, CombinedOrientation.EAST_UP);
        
        result += test(CombinedOrientation.UP_NORTH, Rotation.CLOCKWISE_180, CombinedOrientation.UP_SOUTH);
        result += test(CombinedOrientation.UP_EAST, Rotation.CLOCKWISE_180, CombinedOrientation.UP_WEST);
        result += test(CombinedOrientation.UP_SOUTH, Rotation.CLOCKWISE_180, CombinedOrientation.UP_NORTH);
        result += test(CombinedOrientation.UP_WEST, Rotation.CLOCKWISE_180, CombinedOrientation.UP_EAST);
        
        result += test(CombinedOrientation.DOWN_NORTH, Rotation.CLOCKWISE_180, CombinedOrientation.DOWN_SOUTH);
        result += test(CombinedOrientation.DOWN_EAST, Rotation.CLOCKWISE_180, CombinedOrientation.DOWN_WEST);
        result += test(CombinedOrientation.DOWN_SOUTH, Rotation.CLOCKWISE_180, CombinedOrientation.DOWN_NORTH);
        result += test(CombinedOrientation.DOWN_WEST, Rotation.CLOCKWISE_180, CombinedOrientation.DOWN_EAST);

        if(result.isEmpty()) test.succeed();
        else test.fail("Orientation failed integrity test: \n" + result);
    }

    private static String test(CombinedOrientation orient, Rotation rotation, CombinedOrientation expected) {
        CombinedOrientation applied = orient.applyRotation(rotation);
        if(applied != expected) return "\t" + orient + " -> " + rotation + " -> " + applied + ", expected " + expected + "\n";
        return "";
    }
}
