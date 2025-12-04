package com.quattage.mechano.infrastructure.gametest;

import java.util.UUID;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoItems;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.api.switchboard.action.GridActionTask;
import com.quattage.mechano.foundation.tracking.GridUUID;
import com.quattage.mechano.foundation.tracking.GridUUID.EntityUUID;
import com.quattage.mechano.foundation.tracking.GridUUID.VoxelUUID;
import com.quattage.mechano.foundation.tracking.UUIDSourceDiscriminator;

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

    private static void testUUID(GameTestHelper test, GridUUID expected) {
        ItemStack newStack = new ItemStack(MechanoItems.SPOOL_HOOKUP.get(), 1);
        newStack.set(UUIDSourceDiscriminator.ATTACHMENT, expected);
        CompoundTag serialized = (CompoundTag)newStack.save(test.getLevel().registryAccess());
        serialized = serialized.getCompound("components").getCompound(UUIDSourceDiscriminator.ATTACHMENT.getRegisteredName());
        GridUUID result = UUIDSourceDiscriminator.read(serialized);
        if(result == null) {
            test.fail("Serialization returned a null UUID for type '" + expected.getClass().getSimpleName() + "'");
            return;
        }
        if(!result.equals(expected)) {
            test.fail("Malformed serialization output for UUID of type '" + expected.getClass().getSimpleName() 
                + "' - got " + result + ", expected " + expected + " (source tag: " + serialized + ")");
            return;
        }
        test.succeed();
    }

    @GameTest(template = "empty", batch = "persistenceTests")
    public static void verifyGridTasks(GameTestHelper test) {
        for(GridAction action : GridAction.values()) {
            if(!action.isTask()) continue;
            GridActionTask task = action.getTask();
            if(PersistenceTests.verifyTaskValidity(test, action, task)) return;
            if(PersistenceTests.verifyTaskSerialize(test, action.getTask())) return;
        }
        test.succeed();
    }

    private static boolean verifyTaskValidity(GameTestHelper test, GridAction action, GridActionTask task) {
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
        if(template == null) {
            test.fail("Task '" + task.getClass().getSimpleName() + "' returned a null argument template!");
            return true;
        }
        if(template.length <= 0) {
            test.fail("Task '" + task.getClass().getSimpleName() + "' returned an empty argument template!");
            return true;
        }
        for(int x = 0; x < template.length; x++) {
            Class<?> expected = template[x];
            if(expected == null) {
                test.fail("Task '" + task.getClass().getSimpleName() + "' argument template contained a null class at index " + x);
                return true;
            }
        }
        return false;
    }


    private static boolean verifyTaskSerialize(GameTestHelper test, GridActionTask task) {
        // stub for now
        return false;
    }
}
