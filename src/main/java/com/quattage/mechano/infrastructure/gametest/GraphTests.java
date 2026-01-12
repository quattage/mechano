package com.quattage.mechano.infrastructure.gametest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoBlocks;
import com.quattage.mechano.MechanoTransmitters;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.GriddableTerminus;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.component.ComponentTracker;
import com.quattage.mechano.api.grid.component.ComponentUUID;
import com.quattage.mechano.api.grid.component.ComponentUUID.ComponentBinding;
import com.quattage.mechano.api.grid.component.ComponentUUID.VoxelUUID;
import com.quattage.mechano.api.grid.component.GridConstruct;
import com.quattage.mechano.api.grid.topology.AncillaryPair;
import com.quattage.mechano.api.grid.topology.ComponentLink;
import com.quattage.mechano.api.grid.topology.netlist.NodalCluster;
import com.quattage.mechano.api.grid.topology.netlist.NodeUnionSet;
import com.quattage.mechano.api.grid.topology.vertex.AncillaryNode;
import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.api.grid.topology.vertex.Terminal;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.content.connector.ConnectorBlockEntity;
import com.quattage.mechano.infrastructure.EnqueuedGridManifest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Mechano.ID)
@PrefixGameTestTemplate(false)
public class GraphTests {

    @GameTest(template = "empty", batch = "graphTests")
    public static void unionCreatesEquivalence(GameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        MockNode a = new MockNode("A", false);
        MockNode b = new MockNode("B", false);
        uf.union(a, b);
        test.assertValueEqual(uf.find(a), uf.find(b), "rootResult");
        test.succeed();
    }

    @GameTest(template = "empty", batch = "graphTests")
    public static void unionIsTransitive(GameTestHelper test) {
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

    @GameTest(template = "empty", batch = "graphTests")
    public static void unionIsTransitiveIndirect(GameTestHelper test) {
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

    @GameTest(template = "empty", batch = "graphTests")
    public static void unionIsIdempotent(GameTestHelper test) {
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

    @GameTest(template = "empty", batch = "graphTests")
    public static void unionRootsChange(GameTestHelper test) {
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

    @GameTest(template = "empty", batch = "graphTests")
    public static void unionPrioritizesGround(GameTestHelper test) {
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

    @GameTest(template = "empty", batch = "graphTests")
    public static void unionAssignsIndices(GameTestHelper test) {
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

    @GameTest(template = "empty", batch = "graphTests")
    public static void unionsMaintanBranches(GameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        MockNode aLast = GraphTests.fillUnionSet(uf, "A", 6);
        MockNode bLast = GraphTests.fillUnionSet(uf, "B", 8);
        Node[] aBranch = NodalCluster.getConstituents(uf, aLast);
        Node[] bBranch = NodalCluster.getConstituents(uf, bLast);
        test.assertValueEqual(aBranch.length, 6, "Branch size A");
        test.assertValueEqual(bBranch.length, 8, "Branch size B");
        test.succeed();
    }

    @GameTest(template = "empty", batch = "graphTests", attempts = 1024, requiredSuccesses = 1024)
    public static void unionCreatesBranches(GameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        GraphTests.fillUnionSet(uf, "T", 21);
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

    @GameTest(template = "empty", batch = "graphTests", attempts = 1024, requiredSuccesses = 1024)
    public static void unionRemovalMaintainsTransitivity(GameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        GraphTests.fillUnionSet(uf, "T", 21);
        MockNode split = (MockNode)uf.getByComponentID("T10");
        test.assertTrue(split != null, "wtf");
        uf.remove(split);
        test.assertValueEqual(uf.size(), 2, "set transitive size");
        test.assertValueEqual(uf.deepSize(), 20, "set deep size");
        test.succeed();
    }

    @GameTest(template = "empty", batch = "graphTests", attempts = 1024, requiredSuccesses = 1024)
    public static void unionRemovalRespectsRoots(GameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        GraphTests.fillUnionSet(uf, "T", 10);
        MockNode split = new MockNode("T10", false).setPrimary();
        uf.union(uf.getByComponentID("T9"), split); 
        uf.compress();
        uf.remove(split);
        test.assertValueEqual(uf.size(), 1, "set transitive size");
        test.assertValueEqual(uf.deepSize(), 10, "set deep size");
        test.succeed();
    }

    @GameTest(template = "empty", batch = "graphTestsRigourous", attempts = 8192, requiredSuccesses = 8192)
    public static void unionRemovalBatch(GameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        GraphTests.fillUnionSet(uf, "T", 21);
        uf.removeAll(Arrays.asList(
            uf.getByComponentID("T10"),
            uf.getByComponentID("T15"),
            uf.getByComponentID("T6")
        ));
        // Mechano.LOGGER.warn("::\n" + uf);
        test.assertValueEqual(uf.size(), 4, "set transitive size");
        test.succeed();
    }

    @GameTest(template = "empty", batch = "graphTestsRigourous", attempts = 8192, requiredSuccesses = 8192)
    public static void unionRemovalBatchEdgeCase(GameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        GraphTests.fillUnionSet(uf, "T", 36);
        uf.removeAll(Arrays.asList(
            uf.getByComponentID("T0"),
            uf.getByComponentID("T1"),
            uf.getByComponentID("T2")
        ));
        // Mechano.LOGGER.warn("::\n" + uf);
        test.assertValueEqual(uf.size(), 1, "set transitive size");
        test.succeed();
    }

    @GameTest(template = "empty", batch = "graphTests")
    public static void unionMemoryStressTest(GameTestHelper test) {
        ServerGrid grid = Grid.server(test.getLevel());
        GraphTests.fillUnionSet(grid.getNetlist(), "StressTestNode", 200);
        grid.tick();
        EnqueuedGridManifest.getImmediately(grid);
        // Mechano.LOGGER.warn("\n" + manifest);
        test.succeed();
    }

    private static MockNode fillUnionSet(NodeUnionSet uf, String prefix, int length) {
        MockNode prev = null;
        for(int x = 0; x < length; x++) {
            MockNode node = new MockNode(prefix + x, false);
            if(prev != null) uf.union(prev, node);
            prev = node;
        }
        return prev;
    }

    @GameTest(template = "empty", batch = "graphTests")
    public static void voxelGriddableUUIDParity(GameTestHelper test) {
        BlockPos posA = new BlockPos(10, 15, 2);
        test.setBlock(posA, MechanoBlocks.CONNECTOR_SINGLE.getDefaultState());
        ConnectorBlockEntity cbeA = test.getBlockEntity(posA);
        test.assertTrue(cbeA != null, "ConnectorBlockEntity A couldn't be located.");

        BlockPos posB = new BlockPos(3, 17, 9);
        test.setBlock(posB, MechanoBlocks.CONNECTOR_SINGLE.getDefaultState());
        ConnectorBlockEntity cbeB = test.getBlockEntity(posB);
        test.assertTrue(cbeB != null, "ConnectorBlockEntity B couldn't be located.");

        ComponentUUID<?> idA = new VoxelUUID(test.absolutePos(posA));
        test.assertValueEqual(cbeA.getUUIDSafe(), idA, "UUID A");
        ComponentUUID<?> idB = new VoxelUUID(test.absolutePos(posB));
        test.assertValueEqual(cbeB.getUUIDSafe(), idB, "UUID B");

        test.succeed();
    }

    @GameTest(template = "empty", batch = "graphTests")
    public static void voxelGriddableProvidesCircuit(GameTestHelper test) {
        BlockPos posA = new BlockPos(10, 15, 2);
        test.setBlock(posA, MechanoBlocks.CONNECTOR_SINGLE.getDefaultState());
        ConnectorBlockEntity cbe = test.getBlockEntity(posA);
        test.assertTrue(cbe != null, "ConnectorBlockEntity couldn't be located.");
        CircuitComponent component = cbe.getComponent();
        test.assertTrue(component != null, "Griddable failed to provide a non-null circuit");
        test.succeed();
    }

    @GameTest(template = "empty", batch = "graphTests")
    public static void voxelGriddableIsReachable(GameTestHelper test) {
        BlockPos pos = new BlockPos(4, 2, 7);
        test.setBlock(pos, MechanoBlocks.CONNECTOR_SINGLE.getDefaultState());
        ConnectorBlockEntity cbe = test.getBlockEntity(pos);
        test.assertTrue(cbe != null, "ConnectorBlockEntity couldn't be located.");
        Grid grid = Grid.server(test.getLevel());
        test.assertTrue(grid.isReachable(cbe), "ConnectorBlockEntity couldn't be reached.");
        GriddableTerminus accelerator;
        try { accelerator = cbe.getTerminus(); }
        catch(Exception e) {
            e.printStackTrace();
            test.fail("Critical failure while initializing accelerator!");
            return;
        }
        accelerator.forEach(expected -> {
            ComponentUUID<?> address = grid.getAddressFor(cbe, expected);
            CircuitComponent result = ComponentTracker.find(grid, address);
            if(result == null) {
                test.fail("Reachability check for '" + cbe.getComponent().getComponentID().toLowerCase(Locale.ROOT) 
                    + "' belonging to " + cbe.getClass().getSimpleName().toLowerCase() + " failed while acquiring component at " + address);
                return;
            }
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

    @GameTest(template = "empty", batch = "graphTests")
    public static void voxelGriddableCanBeLinked(GameTestHelper test) {

        BlockPos posA = new BlockPos(10, 15, 2), posB = new BlockPos(3, 17, 9);
        test.setBlock(posA, MechanoBlocks.CONNECTOR_SINGLE.getDefaultState());
        test.setBlock(posB, MechanoBlocks.CONNECTOR_SINGLE.getDefaultState());

        ConnectorBlockEntity cbeA = test.getBlockEntity(posA), cbeB = test.getBlockEntity(posB);
        test.assertTrue(cbeA != null, "ConnectorBlockEntity A couldn't be located.");
        test.assertTrue(cbeB != null, "ConnectorBlockEntity B couldn't be located.");

        ComponentUUID<?> idA = cbeA.getUUID(), idB = cbeB.getUUID();
        AncillaryNode<?> startNode = cbeA.getDefaultAncillary(), endNode = cbeB.getDefaultAncillary();
        test.assertTrue(startNode != null, "ConnectorBlockEntity A couldn't provide a default ancilllary");
        test.assertTrue(endNode != null, "ConnectorBlockEntity B couldn't provide a default ancilllary");

        ComponentUUID<?> startID = startNode.bindUUID(idA.copy());
        ComponentUUID<?> endID = endNode.bindUUID(idB.copy());
        test.assertTrue(startID != idA, "ConnectorBlockEntity A's uuid copy returned the same instance");
        test.assertTrue(endID != idB, "ConnectorBlockEntity B's uuid copy returned the same instance");
        test.assertTrue(!idA.equals(startID), "The starting node didn't alter the binding of ConnectorBlockEntity A's uuid");
        test.assertTrue(!idB.equals(endID), "The starting node didn't alter the binding of ConnectorBlockEntity A's uuid");

        ComponentLink<?> link = new ComponentLink<>(MechanoTransmitters.HOOKUP.get(), startID, startNode, endID, endNode);
        link.validateSelf();
        ComponentLink<?> linkInverted = link.flippedCopy();
        linkInverted.validateSelf();
        test.assertTrue(link.getStartNode().equals(linkInverted.getEndNode()) && link.getEndNode().equals(linkInverted.getStartNode()), "The link and its inverted copy aren't functionally equal");

        ServerGrid grid = Grid.server(test.getLevel());
        GridAction linkResult = grid.addLink(link);
        test.assertTrue(linkResult.getActionType().indicatesSuccess(), "Linking returned failure case " + linkResult);

        List<AncillaryPair> linkAcquire = grid.getLinksBelongingTo(startID), linkAcquireInverted = grid.getLinksBelongingTo(endID);
        test.assertFalse(linkAcquire == null || linkAcquire.isEmpty(), "Link re-acquisition returned null or empty list");
        test.assertFalse(linkAcquireInverted == null || linkAcquireInverted.isEmpty(), "Inverted link re-acquisition returned null or empty list");
        test.assertTrue(linkAcquire.contains(link), "Link re-aquisition failed to identify original link");
        test.assertTrue(linkAcquireInverted.contains(linkInverted), "Link re-aquisition failed to identify inverted link");
        test.succeed();
    }

    private static class MockNode implements Node {

        private final String id;
        private final boolean isGrounded;
        private boolean isMP = false;
        private int index = -2;

        private MockNode(String id, boolean isGrounded) {
            this.id = id;
            this.isGrounded = isGrounded;
        }

        @Override
        public @Nullable GridConstruct getParentConstruct() {
            return null;
        }

        @Override
        public boolean localAttach(Terminal pin) {
            return false;
        }

        @Override
        public boolean localAttach(@Nullable Griddable<?> source, AncillaryNode<?> jack) {
            return false;
        }

        @Override
        public boolean localDetach(Terminal pin) {
            return false;
        }

        @Override
        public boolean localDetach(@Nullable Griddable<?> source, AncillaryNode<?> jack) {
            return false;
        }

        @Override
        public List<AncillaryNode<?>> getAncillaries() {
            return Collections.emptyList();
        }

        protected MockNode setPrimary() {
            this.isMP = true;
            return this;
        }

        @Override
        public int getMergePriority() {
            return isMP ? -50 : 2;
        }

        @Override
        public Terminal[] getTerminals() {
            return new Terminal[0];
        }

        @Override
        public int getNodalIndex() {
            return isGrounded ? -1 : index;
        }

        @Override
        public void setNodalIndex(int index) {
            this.index = index;
        }

        @Override
        public int getHierarchyIndex() {
            return index;
        }

        @Override
        public void dispose() {
            this.index = -2;
        }

        @Override
        public boolean isGrounded() {
            return isGrounded;
        }

        @Override
        public String getComponentID() {
            return id;
        }

        @Override
        public @Nullable CircuitComponent getComponent(ComponentBinding binding) {
            return this;
        }
    }
}

