package com.quattage.mechano.infrastructure.gametest.tests;

import com.quattage.mechano.api.grid.GridTracking;
import com.quattage.mechano.api.grid.GridUUID;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTestHelper;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTests.MechanoTestHolder;

import net.minecraft.gametest.framework.GameTest;

@MechanoTestHolder
public class PersistenceTests {
    
    @GameTest
    public static void checkUUIDSerialize(MechanoGameTestHelper test) {
        GridTracking[] trackerTypes = GridTracking.values();
        for(GridTracking tracker : trackerTypes) {
            GridUUID<?> randomUUID = tracker.createRandomUUID(test.getLevel().random);
            test.checkUUID(randomUUID);
        }
        test.succeed();
    }

    @GameTest
    public static void verifyGridTasks(MechanoGameTestHelper test) {
        for(GridAction action : GridAction.values()) {
            if(!action.isTask()) continue;
            test.checkTask(action, action.getTask());
        }
        test.succeed();
    }
}
