package com.quattage.mechano.infrastructure.gametest;

import java.util.Locale;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoBlockEntities;
import com.quattage.mechano.MechanoBlocks;
import com.quattage.mechano.MechanoTransmitters;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.GriddableTerminus;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.transmitter.TransmitterEntry;
import com.quattage.mechano.foundation.tracking.GridUUID;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Mechano.ID)
@PrefixGameTestTemplate(false)
public class GridActionTests {

    @GameTest(template = "empty", batch="gridActionTests")
    public static void testVoxelJackAccess(GameTestHelper test) {
        BlockPos pos = new BlockPos(4, 2, 7);
        GridActionTests.placeConnector(test, pos);
        Grid grid = Grid.server(test.getLevel());
        Griddable<?>be = GridActionTests.getBEOrFail(test, pos, MechanoBlockEntities.CONNECTOR_SINGLE.get());
        GriddableTerminus terminus;
        try { terminus = be.getTerminus(); }
        catch(Exception e) {
            e.printStackTrace();
            test.fail("Critical failure while initializing terminus!");
            return;
        }
        GridActionTests.testTerminusReachability(test, grid, be, terminus);
        test.succeed();
    }

    @GameTest(template = "empty", batch="gridActionTests")
    public static void testTransmitterAvailability(GameTestHelper test) {
        try { 
            TransmitterEntry<?> trns = MechanoTransmitters.HOOKUP;
        }
        catch(RuntimeException e) { e.printStackTrace(); test.fail(e.getMessage()); }
        test.succeed();
    }

    private static void testTerminusReachability(GameTestHelper test, Grid grid, Griddable<?>source, GriddableTerminus terminus) {
        terminus.forEach(expected -> {
            GridUUID address = grid.getAddressFor(source, expected);
            CircuitComponent result = grid.findComponent(address);
            if(result == null) {
                test.fail("Reachability check for '" + source.getCircuit().getComponentID().toLowerCase(Locale.ROOT) 
                    + "' belonging to " + source.getClass().getSimpleName().toLowerCase() + " failed while acquiring component at " + address);
                return;
            }
            if(result != expected) {
                if(result.getType() == expected.getType()) {
                    test.fail("Reachability check for " + expected + " returned a mismatched instance - expected (" 
                        + expected.hashCode() + "), got (" + result.hashCode() + ")");
                }
                else test.fail("Reachability check failed for " + address + " - expected " + expected + ", got " + result);
            }
        });
        test.succeed();
    }

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> T getBEOrFail(GameTestHelper test, BlockPos pos, BlockEntityType<T> expected) {
        BlockEntity be = test.getBlockEntity(pos);
        if(be == null) {
            test.fail("BlockEntity at " + pos + " was null");
            return null;
        }
        if(be != null && be.getType() != expected) {
            test.fail("BlockEntity at " + pos + " was of an unexpected type '" + be.getClass().getSimpleName() + "'");
            return null;
        }
        return (T)be;
    }

    private static void placeConnector(GameTestHelper test, BlockPos pos) {
        test.setBlock(pos, MechanoBlocks.CONNECTOR_SINGLE.get());
        test.assertBlockPresent(MechanoBlocks.CONNECTOR_SINGLE.get(), pos);
    }
}