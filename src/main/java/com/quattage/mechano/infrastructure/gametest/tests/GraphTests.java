package com.quattage.mechano.infrastructure.gametest.tests;

import com.quattage.mechano.api.GriddableTerminus;
import com.quattage.mechano.content.connector.ConnectorBlockEntity;
import com.quattage.mechano.grid.GridTracking;
import com.quattage.mechano.grid.topology.AncillaryPair;
import com.quattage.mechano.grid.topology.core.CircuitComponent;
import com.quattage.mechano.grid.topology.core.GridUUID;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTestHelper;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTests.MechanoTestHolder;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTests.Repeat;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.world.level.block.Blocks;

@MechanoTestHolder
public class GraphTests {

    @GameTest
    public static void voxelGriddableProvidesCircuit(MechanoGameTestHelper test) {
        ConnectorBlockEntity cbe = test.placeConnector(test.randomPos());
        CircuitComponent component = cbe.getComponent();
        test.assertTrue(component != null, "Griddable failed to provide a non-null circuit");
        test.succeed();
    }

    @Repeat(iterations = 64)
    @GameTest
    public static void voxelGriddableIsReachable(MechanoGameTestHelper test) {
        ConnectorBlockEntity cbe = test.placeConnector(test.randomPos());
        GriddableTerminus terminus = test.getTerminus(cbe);
        terminus.forEach(expected -> {
            GridUUID<?> address = GridTracking.getAddress(cbe, expected);
            CircuitComponent result = test.getComponentSafely(address);
            test.assertFalse(result == null, "Reachability check for '" + cbe.getComponent().getComponentID() 
                + "' belonging to " + cbe.getClass().getSimpleName() + " failed while acquiring component at " + address);
            if(result != expected)
                test.fail("Reachability check failed for " + address + " - expected " + expected + ", got " + result);
        });
        test.succeed();
    }

    @GameTest
    public static void fullConnectionTest(MechanoGameTestHelper test) {

        BlockPos bp = test.randomPos();

        AncillaryPair linkA = test.generateUnion(bp, "linkA");
        AncillaryPair linkB = test.generateUnion(bp.offset(0, 0, 4), "linkB");
        AncillaryPair linkC = test.generateUnion(bp.offset(0, 0, 8), "linkC");
        AncillaryPair linkD = test.generateUnion(bp.offset(0, 0, 12), "linkD");

        test.verifyGrid(4, 8, "initial series");
        test.verifyDomains(1, 2, 0, 2, "initial series");

        AncillaryPair linkAB = test.generateUnion(linkA.getEndAncillary(), linkB.getStartAncillary(), "linkAB");
        test.verifyGrid(3, 10, "subsequent AB");

        AncillaryPair linkCD = test.generateUnion(linkC.getEndAncillary(), linkD.getStartAncillary(), "linkCD");
        test.verifyGrid(2, 12, "subsequent CD");
        test.verifyDomains(1, 4, 0, 4, "subsequent CD");

        AncillaryPair linkBC = test.generateUnion(linkB.getEndAncillary(), linkC.getStartAncillary(), "linkBC");
        test.verifyGrid(1, 14, "subsequent BC"); 
        test.verifyDomains(1, 8, 0, 8, "subsequent BC");

        test.dumpGrid();

        test.setBlock(bp.offset(0, 0, 6), Blocks.AIR);
        test.tickGrid();

        test.dumpActionLog();
        test.dumpGrid();

        test.verifyGrid(2, 10, "post-removal @ offset 6");

        test.setBlock(bp.offset(0, 0, 8), Blocks.AIR);
        test.tickGrid();

        // test.dumpGrid(true, "fullConnectionTest (" + test.absolutePos(bp) + ")");

        // test.verifyDomains(1, 3, 0, 3, "post-removal @ offset 10");
        test.verifyGrid(2, 8, "post-removal @ offset 8");

        test.succeed();
    }
}

