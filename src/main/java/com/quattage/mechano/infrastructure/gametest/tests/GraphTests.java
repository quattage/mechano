package com.quattage.mechano.infrastructure.gametest.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.quattage.mechano.api.grid.GridTracking;
import com.quattage.mechano.api.grid.GridUUID;
import com.quattage.mechano.api.grid.GriddableTerminus;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.topology.GridDomain;
import com.quattage.mechano.api.grid.topology.NodalCluster;
import com.quattage.mechano.api.grid.topology.NodeUnionSet;
import com.quattage.mechano.api.grid.topology.landmark.Node;
import com.quattage.mechano.api.grid.topology.landmark.link.AncillaryPair;
import com.quattage.mechano.api.grid.topology.landmark.link.NodePair;
import com.quattage.mechano.content.connector.ConnectorBlockEntity;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTestHelper;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTestHelper.MockNode;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTests.MechanoTestHolder;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTests.PrintGridAfter;
import com.quattage.mechano.infrastructure.gametest.MechanoGameTests.Repeat;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;

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
    public static void unionRootsChangeRapidlyEdgeCase(MechanoGameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        MockNode a = new MockNode("A", false);
        MockNode b = new MockNode("B", false);
        MockNode c = new MockNode("C", false);
        uf.add(a); uf.add(b); uf.add(c);
        uf.union(a, b); uf.union(b, c);
        // at some point this would occasionally cause a stackoverflow but it's not anymore and i don't know why
        for(int x = 0; x < 64; x++) {
            uf.remove(a);
            uf.union(a, b);
        }
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
        List<NodalCluster> clusters = NodalCluster.ofClusters(uf, splitBranch);
        test.assertValueEqual(clusters.size(), 2, "amount of clusters");
        test.assertValueEqual(clusters.get(0).size(), 10, "cluster size A");
        test.assertValueEqual(clusters.get(1).size(), 10, "cluster size B");
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
        uf.massRemove(test.getGrid(), null, pairs);
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
        uf.massRemove(test.getGrid(), null, pairs);
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
        uf.massRemove(test.getGrid(), singles, pairs);
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
    @PrintGridAfter
    public static void fullConnectionTest(MechanoGameTestHelper test) {

        test.getGrid().load();
        BlockPos bp = test.randomPos();
        AncillaryPair linkA = test.generateUnion(bp);
        AncillaryPair linkB = test.generateUnion(bp.offset(0, 0, 4));
        AncillaryPair linkC = test.generateUnion(bp.offset(0, 0, 8));

        test.assertValueEqual(test.getGrid().domains().size(), 3, "domain count");
        for(int x = 0; x < test.getGrid().domains().size(); x++) {
            GridDomain domain = test.getGrid().domains().get(x);
            String didx = "domain " + x;
            test.assertValueEqual(domain.netlist().size(), 1, didx + " transitive size");
            test.assertValueEqual(domain.netlist().deepSize(), 2, didx + " deep size");
            test.assertValueEqual(domain.indexer().sourceCount(), 0, didx + " indexer source count");
            test.assertValueEqual(domain.indexer().nodeCount(), 2, didx + " indexer node count");
            GraphTests.assertDomainValid(test, domain, x);
        }

        

        test.assertValueEqual(test.getGrid().lookup().size(), 6, "lookup size");
        test.succeed();
    }

    private static void assertDomainValid(MechanoGameTestHelper test, GridDomain domain, int index) {
        try {
            domain.netlist().forEachTransitive((head, branch) -> {
                test.assertValueEqual(head.getDomainIndex(), index, " domain index @ node #" + System.identityHashCode(head));
                int headIndex = domain.indexer().get(head);
                test.assertValueEqual(headIndex, 0, " nodal index @ node #" + System.identityHashCode(head));
                branch.forEach(leaf -> {
                    test.assertValueEqual(leaf.getDomainIndex(), index, " domain index @ node #" + System.identityHashCode(leaf));
                    test.assertValueEqual(domain.indexer().get(leaf), headIndex, " nodal index @ node #" + System.identityHashCode(leaf));
                });
            });
        } catch (IllegalStateException e) {
            e.printStackTrace();
            test.fail("Encountered exception while traversing " + index + "'s netlist (See stacktrace above)");
        }
    }
}

