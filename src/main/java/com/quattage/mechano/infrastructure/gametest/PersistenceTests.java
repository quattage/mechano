package com.quattage.mechano.infrastructure.gametest;

import java.util.UUID;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoData;
import com.quattage.mechano.MechanoItems;
import com.quattage.mechano.api.grid.component.ComponentTracker;
import com.quattage.mechano.api.grid.component.ComponentUUID;
import com.quattage.mechano.api.grid.component.ComponentUUID.EntityUUID;
import com.quattage.mechano.api.grid.component.ComponentUUID.VoxelUUID;
import com.quattage.mechano.api.switchboard.action.ActionTask;
import com.quattage.mechano.api.switchboard.action.GridAction;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Mechano.ID)
@PrefixGameTestTemplate(false)
public class PersistenceTests {
    
    @GameTest(template = "empty", batch = "persistenceTests")
    public static void checkVoxelUUIDSerialize(GameTestHelper test) {
        PersistenceTests.testUUID(test, new VoxelUUID(11, 5, 2));
    }

    @GameTest(template = "empty", batch = "persistenceTests")
    public static void checkEntityUUIDSerialize(GameTestHelper test) {
        PersistenceTests.testUUID(test, new EntityUUID(UUID.randomUUID()));
    }

    private static void testUUID(GameTestHelper test, ComponentUUID<?> expected) {
        ItemStack newStack = new ItemStack(MechanoItems.SPOOL_HOOKUP.get(), 1);
        newStack.set(MechanoData.UUID, expected);
        CompoundTag serialized = (CompoundTag)newStack.save(test.getLevel().registryAccess());
        serialized = serialized.getCompound("components").getCompound(MechanoData.UUID.getRegisteredName());
        ComponentUUID<?> result = ComponentTracker.read(serialized);
        test.assertFalse(result == null, "Serialization returned a null UUID for type '" + expected.getClass().getSimpleName() + "'");
        test.assertFalse(result == expected, "what");
        test.assertValueEqual(expected, result, "serialization output");
        test.succeed();
    }

    @GameTest(template = "empty", batch = "persistenceTests")
    public static void verifyGridTasks(GameTestHelper test) {
        for(GridAction action : GridAction.values()) {
            if(!action.isTask()) continue;
            ActionTask task = action.getTask();
            if(PersistenceTests.verifyTaskValidity(test, action, task)) return;
            if(PersistenceTests.verifyTaskSerialize(test, action.getTask())) return;
        }
        test.succeed();
    }

    private static boolean verifyTaskValidity(GameTestHelper test, GridAction action, ActionTask task) {
        Class<?>[] template;
        if(action.getTask() == null) {
            test.fail("Task for action '" + action + "' returned null, despite being a task type!");
            return true;
        }
        try { template = task.getArgumentTemplate(); }
        catch(Exception e) {
            e.printStackTrace();
            test.fail("Encountered an exception while getting argument template for '" + task.getClass().getSimpleName() + "'");
            return true;
        }
        if(template == null) return false;
        for(int x = 0; x < template.length; x++) {
            Class<?> expected = template[x];
            if(expected == null) {
                test.fail("Task '" + task.getClass().getSimpleName() + "' argument template contained a null class at index " + x);
                return true;
            }
        }
        return false;
    }


    private static boolean verifyTaskSerialize(GameTestHelper test, ActionTask task) {
        // stub for now
        return false;
    }
}
