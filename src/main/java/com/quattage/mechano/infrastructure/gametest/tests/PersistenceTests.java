package com.quattage.mechano.infrastructure.gametest.tests;

import com.quattage.mechano.MechanoData;
import com.quattage.mechano.MechanoItems;
import com.quattage.mechano.api.blockEntity.GriddableBlockEntity;
import com.quattage.mechano.grid.GridTracking;
import com.quattage.mechano.grid.topology.AncillaryNode;
import com.quattage.mechano.grid.topology.core.CircuitComponent;
import com.quattage.mechano.grid.topology.core.GridUUID;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTestHelper;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTests.MechanoTestHolder;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTests.Repeat;
import com.quattage.mechano.switchboard.action.GridAction;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

@MechanoTestHolder
public class PersistenceTests {

    @GameTest
    public static void gridTasksHaveTemplates(MechanoGameTestHelper test) {
        for(GridAction action : GridAction.values()) {
            if(!action.isTask()) continue;
            test.verifyArgumentTemplateFor(action, action.getTask());
        }
        test.succeed();
    }

    @Repeat(iterations = 64)
    @GameTest
    public static void uuidIsSerializable(MechanoGameTestHelper test) {
        GridTracking[] trackerTypes = GridTracking.values();
        for(GridTracking tracker : trackerTypes) {
            GridUUID<?> randomUUID = tracker.createRandomUUID(test.getLevel().random);
            test.failIfNull(randomUUID, "tracker failed to produce a fresh random uuid instance");
            ItemStack newStack = new ItemStack(MechanoItems.SPOOL_HOOKUP.get(), 1);
            newStack.set(MechanoData.UUID, randomUUID);
            CompoundTag serialized = (CompoundTag)newStack.save(test.getLevel().registryAccess());
            serialized = serialized.getCompound("components").getCompound(MechanoData.UUID.getRegisteredName());
            GridUUID<?> result = GridTracking.read(serialized);
            test.assertFalse(result == null, "Serialization returned a null UUID for type '" + randomUUID.getClass().getSimpleName() + "'");
            test.assertFalse(result == randomUUID, "what");
            test.assertValueEqual(result, randomUUID, "serialization output");
        }
        test.succeed();
    }

    @Repeat(iterations = 64)
    @GameTest
    public static void uuidIsLocatable(MechanoGameTestHelper test) {
        GriddableBlockEntity gbe = test.placeConnector(test.randomPos());
        AncillaryNode<?> node = null;
        try { node = gbe.getTerminus().getAncillary(); }
        catch(Exception e) {
            e.printStackTrace();
            throw e;
        }
        test.failIfNull(node, "connector failed to provide a default ancillary");
        GridUUID<?> id = GridTracking.getAddress(gbe, node);
        if(!id.hasBindings()) test.fail("The returned UUID has no bindings!");
        id.forEachBinding(composite -> {
            test.failIfNull(composite, "The returned UUID contained a null binding!");
            if(composite.isValid()) return;
            test.fail("The returned ID included a binding " + composite + ", which describes an invalid target!");
        });
        CircuitComponent reacquire = GridTracking.getComponent(test.getLevel(), id);
        test.failIfNull(reacquire, "Failed to reaquire previously existing component at " + id);
        test.assertValueEqual(reacquire, node, "component at " + id);
        test.succeed();
    }
}
