package com.quattage.mechano.foundation.gametest;

import com.quattage.mechano.Mechano;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Mechano.ID)
@PrefixGameTestTemplate(false)
public class AdjacencyTests {
    
    @GameTest(template = "empty", batch="electricityTests")
    public static void gridSerializeDeterminism(GameTestHelper test) {
        test.succeed();
    }
}
