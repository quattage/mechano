package com.quattage.mechano.infrastructure.gametest.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoTransmitters;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.ServerGrid.TopologyProcessQueue;
import com.quattage.mechano.api.grid.GridComponentTracker;
import com.quattage.mechano.api.grid.GriddableTerminus;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.component.ComponentUUID;
import com.quattage.mechano.api.grid.topology.netlist.NodalCluster;
import com.quattage.mechano.api.grid.topology.netlist.NodeUnionSet;
import com.quattage.mechano.api.grid.topology.netlist.NodeUnionSet.NodePair;
import com.quattage.mechano.api.grid.topology.vertex.AncillaryNode;
import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.content.connector.ConnectorBlockEntity;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTestHelper;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTestHelper.MockNode;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTests.MechanoTestHolder;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTests.Repeat;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

@MechanoTestHolder
public class GraphTests {

    @GameTest
    public static void unionCreatesEquivalence(MechanoGameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        MockNode a = new MockNode("A", false);
        MockNode b = new MockNode("B", false);
        uf.union(a, b);
        test.assertValueEqual(uf.find(a), uf.find(b), "rootResult");
        test.succeed();
    }

    @GameTest
    public static void unionIsTransitive(MechanoGameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        MockNode a = new MockNode("A", false);
        MockNode b = new MockNode("B", false);
        MockNode c = new MockNode("C", false);
        uf.union(a, b);
        uf.union(b, c);
        Node root = uf.find(a);
        test.assertValueEqual(uf.find(b), root, "rootResultB");
        test.assertValueEqual(uf.find(c), root, "rootResultC");
        test.succeed();
    }

    @GameTest
    public static void unionIsTransitiveIndirect(MechanoGameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        MockNode a = new MockNode("A", false);
        MockNode b = new MockNode("B", false);
        MockNode c = new MockNode("C", false);
        MockNode d = new MockNode("D", false);
        uf.union(a, c);
        uf.union(a, b);
        uf.union(c, d);
        Node root = uf.find(a);
        test.assertValueEqual(uf.find(b), root, "rootResultB");
        test.assertValueEqual(uf.find(c), root, "rootResultC");
        test.assertValueEqual(uf.find(d), root, "rootResultD");
        test.succeed();
    }

    @GameTest
    public static void unionIsIdempotent(MechanoGameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        MockNode a = new MockNode("A", false);
        MockNode b = new MockNode("B", false);
        uf.union(a, b);
        Node rootFirst = uf.find(a);
        uf.union(a, b);
        Node rootSecond = uf.find(a);
        test.assertValueEqual(rootFirst, rootSecond, "idempotentRoot");
        test.succeed();
    }

    @GameTest
    public static void unionRootsChange(MechanoGameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        MockNode a = new MockNode("A", false);
        MockNode b = new MockNode("B", false);
        MockNode c = new MockNode("C", false);
        uf.add(a);
        uf.add(b);
        uf.add(c);
        test.assertValueEqual(uf.size(), 3, "initialRootCount");
        uf.union(a, b);
        test.assertValueEqual(uf.size(), 2, "secondRootCount");
        uf.union(b, c);
        test.assertValueEqual(uf.size(), 1, "thirdRootCount");
        test.succeed();
    }
    
    @Repeat(iterations = 64)
    @GameTest
    public static void unionPrioritizesGround(MechanoGameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        MockNode gnd = new MockNode("GND", true);
        MockNode a = new MockNode("A", false);
        MockNode b = new MockNode("B", false);
        uf.union(a, b);
        uf.union(a, gnd);
        Node rootA = uf.find(a);
        Node rootB = uf.find(b);
        test.assertTrue(rootA.isGrounded(), "RootA was not grounded");
        test.assertValueEqual(rootA, rootB, "commonGround");
        test.succeed();
    }

    @GameTest
    public static void unionAssignsIndices(MechanoGameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        MockNode gnd = new MockNode("GND", true);
        MockNode a = new MockNode("A", false);
        MockNode b = new MockNode("B", false);
        MockNode c = new MockNode("C", false);
        uf.union(a, b);
        uf.union(a, gnd);
        uf.add(c);
        uf.assignIndices();
        test.assertValueEqual(gnd.getNodalIndex(), -1, "groundIndex");
        test.assertValueEqual(a.getNodalIndex(), -1, "aIndex");
        test.assertValueEqual(b.getNodalIndex(), -1, "bIndex");
        test.assertValueEqual(c.getNodalIndex(), -2, "cIndex");
        test.succeed();
    }

    @GameTest
    public static void unionsMaintanBranches(MechanoGameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        MockNode aLast = test.populateUF(uf, "A", 6);
        MockNode bLast = test.populateUF(uf, "B", 8);
        Node[] aBranch = NodalCluster.getConstituents(uf, aLast);
        Node[] bBranch = NodalCluster.getConstituents(uf, bLast);
        test.assertValueEqual(aBranch.length, 6, "Branch size A");
        test.assertValueEqual(bBranch.length, 8, "Branch size B");
        test.succeed();
    }

    @GameTest
    public static void unionCreatesBranches(MechanoGameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        test.populateUF(uf, "T", 21);
        MockNode split = (MockNode)uf.getByComponentID("T10");
        test.assertTrue(split != null, "wtf");
        Node[] splitBranch = NodalCluster.getConstituents(uf, split);
        test.assertValueEqual(splitBranch.length, 21, "branch size");
        uf.remove(split, false);
        test.assertValueEqual(uf.deepSize(), 20, "set size after removal");
        List<NodalCluster> clusters = NodalCluster.ofDiscontinuities(uf, splitBranch);
        test.assertValueEqual(clusters.size(), 2, "amount of clusters");
        test.assertValueEqual(clusters.get(0).size(), 10, "cluster size A");
        test.assertValueEqual(clusters.get(1).size(), 10, "cluster size B");
        test.succeed();
    }

    @GameTest
    public static void unionRemovalMaintainsTransitivity(MechanoGameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        test.populateUF(uf, "T", 21);
        MockNode split = (MockNode)uf.getByComponentID("T10");
        test.assertTrue(split != null, "wtf");
        uf.remove(split);
        test.assertValueEqual(uf.size(), 2, "set transitive size");
        test.assertValueEqual(uf.deepSize(), 20, "set deep size");
        test.succeed();
    }

    @Repeat(iterations = 64)
    @GameTest
    public static void unionRemovalRespectsRoots(MechanoGameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        test.populateUF(uf, "T", 10);
        MockNode split = new MockNode("T10", false).setPrimary();
        uf.union(uf.getByComponentID("T9"), split); 
        uf.compress();
        uf.remove(split);
        test.assertValueEqual(uf.size(), 1, "set transitive size");
        test.assertValueEqual(uf.deepSize(), 10, "set deep size");
        test.succeed();
    }

    @Repeat(iterations = 1024)
    @GameTest
    public static void unionRemovalBatch(MechanoGameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        test.populateUF(uf, "T", 21);
        uf.removeAll(Arrays.asList(
            uf.getByComponentID("T10"),
            uf.getByComponentID("T15"),
            uf.getByComponentID("T6")
        ));
        test.assertValueEqual(uf.size(), 4, "set transitive size");
        test.succeed();
    }

    @Repeat(iterations = 64)
    @GameTest
    public static void unionRemovalBatchEdgeCase(MechanoGameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        test.populateUF(uf, "T", 36);
        uf.removeAll(Arrays.asList(
            uf.getByComponentID("T0"),
            uf.getByComponentID("T1"),
            uf.getByComponentID("T2")
        ));
        test.assertValueEqual(uf.size(), 1, "set transitive size");
        test.succeed();
    }

    @Repeat(iterations = 64)
    @GameTest
    public static void unionSingleRemoval(MechanoGameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        test.populateUF(uf, "T", 10);
        Node a = uf.getByComponentID("T5");
        Node b = uf.getByComponentID("T6");
        List<NodePair> pairs = new ArrayList<>();
        pairs.add(new NodePair(a, b));
        uf.massRemove(null, pairs);
        test.assertValueEqual(uf.size(), 2, "set transitive size");
        test.succeed();
    }

    @Repeat(iterations = 64)
    @GameTest
    public static void unionSingleRemovalEdgeCase(MechanoGameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        test.populateUF(uf, "T", 10);
        List<NodePair> pairs = new ArrayList<>();
        pairs.add(new NodePair(uf.getByComponentID("T0"), uf.getByComponentID("T1")));
        uf.massRemove(null, pairs);
        test.assertValueEqual(uf.size(), 1, "set transitive size");
        test.succeed();
    }

    @GameTest
    public static void unionRemovalCombination(MechanoGameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        test.populateUF(uf, "T", 20);
        List<NodePair> pairs = new ArrayList<>();
        pairs.add(new NodePair(uf.getByComponentID("T0"), uf.getByComponentID("T1")));
        List<Node> singles = new ArrayList<>();
        singles.add(uf.getByComponentID("T10"));
        uf.massRemove(singles, pairs);
        Mechano.LOGGER.error("OUTPUT: \n" + uf);
        test.assertValueEqual(uf.size(), 2, "set transitive size");
        test.assertValueEqual(uf.deepSize(), 18, "set deep size");
        test.succeed();
    }

    @Repeat(iterations = 2)
    @GameTest
    public static void unionMemoryStressTest(MechanoGameTestHelper test) {
        ServerGrid grid = Grid.server(test.getLevel());
        test.populateUF(grid.netlist(), "StressTestNode", 200);
        grid.tick();
        test.dumpGrid(false);
        test.succeed();
    }

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
            ComponentUUID<?> address = GridComponentTracker.getAddress(cbe, expected);
            CircuitComponent result = test.getComponentSafely(address);
            test.assertFalse(result == null, "Reachability check for '" + cbe.getComponent().getComponentID() 
                + "' belonging to " + cbe.getClass().getSimpleName() + " failed while acquiring component at " + address);
            if(result != expected) {
                if(result.getHierarchyType() == expected.getHierarchyType()) {
                    test.fail("Reachability check for " + expected + " returned a mismatched instance - expected (" 
                        + expected.hashCode() + "), got (" + result.hashCode() + ")");
                }
                else test.fail("Reachability check failed for " + address + " - expected " + expected + ", got " + result);
            }
        });
        test.succeed();
    }

    @GameTest
    public static void voxelGriddableCanBeLinked(MechanoGameTestHelper test) {

        ConnectorBlockEntity cbeA = test.placeConnector(test.randomPos());
        ConnectorBlockEntity cbeB = test.placeConnector(test.randomPos());

        ComponentUUID<?> idA = cbeA.getUUIDSafe(), idB = cbeB.getUUIDSafe();
        AncillaryNode<?> startNode = cbeA.getDefaultAncillary();
        AncillaryNode<?> endNode = cbeB.getDefaultAncillary();
        test.failIfNull(startNode, "ConnectorBlockEntity A couldn't provide a default ancilllary");
        test.failIfNull(endNode, "ConnectorBlockEntity B couldn't provide a default ancilllary");

        ComponentUUID<?> startID = GridComponentTracker.getAddress(cbeA, startNode);
        ComponentUUID<?> endID = GridComponentTracker.getAddress(cbeB, endNode);

        test.assertTrue(startID != idA, "ConnectorBlockEntity A's uuid copy returned the same instance");
        test.assertTrue(endID != idB, "ConnectorBlockEntity B's uuid copy returned the same instance");
        test.assertTrue(!idA.equals(startID), "The starting node didn't alter the binding of ConnectorBlockEntity A's uuid");
        test.assertTrue(!idB.equals(endID), "The starting node didn't alter the binding of ConnectorBlockEntity A's uuid");

        Player fakePlayer = test.makeMockPlayer(GameType.CREATIVE);
        try {
            test.doGridTask(GridAction.TASK_LINK_CREATE)
                .from(startNode, endNode)
                .withArguments(
                    startID, 
                    endID, 
                    MechanoTransmitters.HOOKUP.get(), 
                    fakePlayer.getUUID()
                ).executeImmediately();
        } catch (Exception e) {
            e.printStackTrace();
            test.fail("Failed while performing linking task [ " + startID + " -> " + endID + "] (See stacktrace above)");
        }
        test.tickGrid();
        test.dumpGrid("voxelGriddableCanBeLinked");
        test.succeed();
    }

    @GameTest
    public static void gridQueueIsOrdered(MechanoGameTestHelper test) {
        test.tryOrFailVerbosely(() -> {
            ServerGrid grid = test.getGrid();
            TopologyProcessQueue queue = grid.getProcessQueue();
            for(int x = 0; x < 32; x++)
                queue.addRandom(grid, test.getLevel().random);
            String logOutput = queue.toString();
            test.assertFalse(logOutput == null || logOutput.isBlank(), "Queue produced no string!");
            // observe the natural ordering in the console - no assertions here since it would just be re-implementing the same code in the queue itself
            // grid.warn(":: \n" + logOutput);
            test.assertTrue(queue.size() == 32, "Queue size was altered by toString()");
            test.succeed();
        }, "??");
    }
}

