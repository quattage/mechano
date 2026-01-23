package com.quattage.mechano.infrastructure.gametest.tests;

import com.quattage.mechano.api.grid.GridComponentTracker;
import com.quattage.mechano.api.grid.component.ComponentUUID;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTestHelper;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTests.MechanoTestHolder;

import net.minecraft.gametest.framework.GameTest;

@MechanoTestHolder
public class PersistenceTests {
    
    @GameTest
    public static void checkUUIDSerialize(MechanoGameTestHelper test) {
        GridComponentTracker[] trackerTypes = GridComponentTracker.values();
        for(GridComponentTracker tracker : trackerTypes) {
            ComponentUUID<?> randomUUID = tracker.createRandomUUID(test.getLevel().random);
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
