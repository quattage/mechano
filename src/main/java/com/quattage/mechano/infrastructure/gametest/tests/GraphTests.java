package com.quattage.mechano.infrastructure.gametest.tests;

import java.util.ArrayList;
import java.util.List;

import com.quattage.mechano.api.GridDomain;
import com.quattage.mechano.content.connector.ConnectorBlockEntity;
import com.quattage.mechano.grid.GridTracking;
import com.quattage.mechano.grid.GridUUID;
import com.quattage.mechano.grid.GriddableTerminus;
import com.quattage.mechano.grid.api.component.CircuitComponent;
import com.quattage.mechano.grid.topology.Node;
import com.quattage.mechano.grid.topology.NodeUnionSet;
import com.quattage.mechano.grid.topology.link.AncillaryPair;
import com.quattage.mechano.grid.topology.link.NodePair;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTestHelper;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTestHelper.MockNode;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTests.MechanoTestHolder;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTests.Repeat;
import com.quattage.mechano.switchboard.RemovalLedger.RemovalEntry;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.world.level.block.Blocks;

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
    public static void unionMergeChangesRoots(MechanoGameTestHelper test) {
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

    @GameTest
    public static void unionAdjacencyIsCyclic(MechanoGameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        test.populateUF(uf, "A", 6);
        Node start = uf.getByComponentID("A0");
        Node end = uf.getByComponentID("A5");
        uf.union(start, end);
        test.assertTrue(uf.hasConnections(start), "starting node had its adjacency corrupted");
        test.assertTrue(uf.hasConnections(end), "ending node had its adjacency corrupted");
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
        GridDomain domain = test.getTestDomain();
        uf.finalizeTopology(domain, 0);
        test.assertValueEqual(domain.indexer().get(gnd), -1, "groundIndex");
        test.assertValueEqual(domain.indexer().get(a), -1, "aIndex");
        test.assertValueEqual(domain.indexer().get(b), -1, "bIndex");
        test.assertValueEqual(domain.indexer().get(c), -2, "cIndex");
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
        uf.massRemove(test.getGrid(), RemovalEntry.of(null, pairs));
        test.assertValueEqual(uf.size(), 2, "set transitive size");
        test.succeed();
    }

    @GameTest
    public static void unionMergeUpdatesTransitivity(MechanoGameTestHelper test) {
        NodeUnionSet uf0 = new NodeUnionSet();
        test.populateUF(uf0, "A", 11);
        NodeUnionSet uf1 = new NodeUnionSet();
        test.populateUF(uf1, "B", 9);
        NodeUnionSet combine = NodeUnionSet.concatenate(uf0, uf1);
        combine.union(combine.getByComponentID("A5"), combine.getByComponentID("B5"));
        test.assertTrue(combine == uf0, "UF concat didn't respect size");
        test.assertValueEqual(combine.size(), 1, "UF concat transitive size");
        test.assertValueEqual(combine.deepSize(), 20, "UF concat deep size");
        test.succeed();
    }

    @Repeat(iterations = 64)
    @GameTest
    public static void unionSingleRemovalEdgeCase(MechanoGameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        test.populateUF(uf, "T", 10);
        List<NodePair> pairs = new ArrayList<>();
        pairs.add(new NodePair(uf.getByComponentID("T0"), uf.getByComponentID("T1")));
        uf.massRemove(test.getGrid(), RemovalEntry.of(null, pairs));
        test.assertValueEqual(uf.size(), 1, "set transitive size");
        test.succeed();
    }

    @GameTest
    public static void unionRemovalCombination(MechanoGameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        test.populateUF(uf, "T", 20);
        List<NodePair> pairs = new ArrayList<>();
        pairs.add(new NodePair(uf.getByComponentID("T0"), 
        uf.getByComponentID("T1")));
        List<Node> singles = new ArrayList<>();
        singles.add(uf.getByComponentID("T10"));
        StringBuilder removalManifest = new StringBuilder();
        uf.massRemove(test.getGrid(), RemovalEntry.of(singles, pairs), removalManifest);
        test.getGrid().warn("::::\n" + removalManifest);
        test.assertValueEqual(uf.size(), 2, "set transitive size");
        test.assertValueEqual(uf.deepSize(), 18, "set deep size");
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

        test.setBlock(bp.offset(0, 0, 6), Blocks.AIR);
        test.tickGrid();
        test.verifyGrid(2, 10, "post-removal @ offset 6");

        test.setBlock(bp.offset(0, 0, 8), Blocks.AIR);
        test.tickGrid();

        // test.dumpGrid(true, "fullConnectionTest (" + test.absolutePos(bp) + ")");

        // test.verifyDomains(1, 3, 0, 3, "post-removal @ offset 10");
        test.verifyGrid(2, 8, "post-removal @ offset 8");

        test.succeed();
    }
}

