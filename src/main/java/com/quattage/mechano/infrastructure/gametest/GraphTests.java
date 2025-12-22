package com.quattage.mechano.infrastructure.gametest;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoBlocks;
import com.quattage.mechano.MechanoTransmitters;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.GridAccelerator;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.solver.NodeUnionSet;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.grid.topology.ComponentLink;
import com.quattage.mechano.api.grid.topology.vertex.AncillaryNode;
import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.api.grid.topology.vertex.Terminal;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.content.connector.ConnectorBlockEntity;
import com.quattage.mechano.foundation.tracking.GridUUID;
import com.quattage.mechano.foundation.tracking.GridUUID.VoxelUUID;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Mechano.ID)
@PrefixGameTestTemplate(false)
public class GraphTests {

    @GameTest(template = "empty", batch="graphTests")
    public static void unionCreatesEquivalence(GameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        TestNode a = new TestNode("A", false);
        TestNode b = new TestNode("B", false);
        uf.union(a, b);
        test.assertValueEqual(uf.find(a), uf.find(b), "rootResult");
        test.succeed();
    }

    @GameTest(template = "empty", batch="graphTests")
    public static void unionIsTransitive(GameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        TestNode a = new TestNode("A", false);
        TestNode b = new TestNode("B", false);
        TestNode c = new TestNode("C", false);
        uf.union(a, b);
        uf.union(b, c);
        Node root = uf.find(a);
        test.assertValueEqual(uf.find(b), root, "rootResultB");
        test.assertValueEqual(uf.find(c), root, "rootResultC");
        test.succeed();
    }

    @GameTest(template = "empty", batch="graphTests")
    public static void unionIsTransitiveIndirect(GameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        TestNode a = new TestNode("A", false);
        TestNode b = new TestNode("B", false);
        TestNode c = new TestNode("C", false);
        TestNode d = new TestNode("D", false);
        uf.union(a, c);
        uf.union(a, b);
        uf.union(c, d);
        Node root = uf.find(a);
        test.assertValueEqual(uf.find(b), root, "rootResultB");
        test.assertValueEqual(uf.find(c), root, "rootResultC");
        test.assertValueEqual(uf.find(d), root, "rootResultD");
        test.succeed();
    }

    @GameTest(template = "empty", batch="graphTests")
    public static void unionIsIdempotent(GameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        TestNode a = new TestNode("A", false);
        TestNode b = new TestNode("B", false);
        uf.union(a, b);
        Node rootFirst = uf.find(a);
        uf.union(a, b);
        Node rootSecond = uf.find(a);
        test.assertValueEqual(rootFirst, rootSecond, "idempotentRoot");
        test.succeed();
    }

    @GameTest(template = "empty", batch="graphTests")
    public static void unionRootsChange(GameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        TestNode a = new TestNode("A", false);
        TestNode b = new TestNode("B", false);
        TestNode c = new TestNode("C", false);
        uf.add(a);
        uf.add(b);
        uf.add(c);
        test.assertValueEqual(3, uf.rootCount(), "initialRootCount");
        uf.union(a, b);
        test.assertValueEqual(2, uf.rootCount(), "secondRootCount");
        uf.union(b, c);
        test.assertValueEqual(1, uf.rootCount(), "thirdRootCount");
        test.succeed();
    }

    @GameTest(template = "empty", batch="graphTests")
    public static void unionPrioritizesGround(GameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        TestNode gnd = new TestNode("GND", true);
        TestNode a = new TestNode("A", false);
        TestNode b = new TestNode("B", false);
        uf.union(a, b);
        uf.union(a, gnd);
        Node rootA = uf.find(a);
        Node rootB = uf.find(b);
        test.assertTrue(rootA.isGrounded(), "RootA was not grounded");
        test.assertValueEqual(rootA, rootB, "commonGround");
        test.succeed();
    }

    @GameTest(template = "empty", batch="graphTests")
    public static void unionAssignsIndices(GameTestHelper test) {
        NodeUnionSet uf = new NodeUnionSet();
        TestNode gnd = new TestNode("GND", true);
        TestNode a = new TestNode("A", false);
        TestNode b = new TestNode("B", false);
        TestNode c = new TestNode("C", false);
        uf.union(a, b);
        uf.union(a, gnd);
        uf.add(c);
        uf.assignIndices();
        test.assertValueEqual(gnd.getNodalIndex(), -1, "groundIndex");
        test.assertValueEqual(a.getNodalIndex(), -1, "aIndex");
        test.assertValueEqual(b.getNodalIndex(), -1, "bIndex");
        test.assertValueEqual(c.getNodalIndex(), 0, "cIndex");
        test.succeed();
    }

    @GameTest(template = "empty", batch="graphTests")
    public static void voxelGriddableUUIDParity(GameTestHelper test) {
        BlockPos posA = new BlockPos(10, 15, 2);
        test.setBlock(posA, MechanoBlocks.CONNECTOR_SINGLE.getDefaultState());
        ConnectorBlockEntity cbeA = test.getBlockEntity(posA);
        test.assertTrue(cbeA != null, "ConnectorBlockEntity A couldn't be located.");

        BlockPos posB = new BlockPos(3, 17, 9);
        test.setBlock(posB, MechanoBlocks.CONNECTOR_SINGLE.getDefaultState());
        ConnectorBlockEntity cbeB = test.getBlockEntity(posB);
        test.assertTrue(cbeB != null, "ConnectorBlockEntity B couldn't be located.");

        GridUUID idA = new VoxelUUID(test.absolutePos(posA));
        test.assertValueEqual(cbeA.getUUIDSafe(), idA, "UUID A");
        GridUUID idB = new VoxelUUID(test.absolutePos(posB));
        test.assertValueEqual(cbeB.getUUIDSafe(), idB, "UUID B");

        test.succeed();
    }

    @GameTest(template = "empty", batch="graphTests")
    public static void voxelGriddableProvidesCircuit(GameTestHelper test) {
        BlockPos posA = new BlockPos(10, 15, 2);
        test.setBlock(posA, MechanoBlocks.CONNECTOR_SINGLE.getDefaultState());
        ConnectorBlockEntity cbe = test.getBlockEntity(posA);
        test.assertTrue(cbe != null, "ConnectorBlockEntity couldn't be located.");
        CircuitComponent component = cbe.getCircuit();
        test.assertTrue(component != null, "Griddable failed to provide a non-null circuit");
        test.assertTrue(component.isSignificant(), "Griddable failed ot provide a valid circuit");
        test.succeed();
    }

    @GameTest(template = "empty", batch="graphTests")
    public static void voxelGriddableIsReachable(GameTestHelper test) {
        BlockPos pos = new BlockPos(4, 2, 7);
        test.setBlock(pos, MechanoBlocks.CONNECTOR_SINGLE.getDefaultState());
        ConnectorBlockEntity cbe = test.getBlockEntity(pos);
        test.assertTrue(cbe != null, "ConnectorBlockEntity couldn't be located.");
        Grid grid = Grid.server(test.getLevel());
        test.assertTrue(grid.isReachable(cbe), "ConnectorBlockEntity couldn't be reached.");
        GridAccelerator accelerator;
        try { accelerator = cbe.getAccelerator(); }
        catch(Exception e) {
            e.printStackTrace();
            test.fail("Critical failure while initializing accelerator!");
            return;
        }
        accelerator.forEach(expected -> {
            GridUUID address = grid.getAddressFor(cbe, expected);
            CircuitComponent result = grid.findComponent(address);
            if(result == null) {
                test.fail("Reachability check for '" + cbe.getCircuit().getComponentID().toLowerCase(Locale.ROOT) 
                    + "' belonging to " + cbe.getClass().getSimpleName().toLowerCase() + " failed while acquiring component at " + address);
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

    @GameTest(template = "empty", batch="graphTests")
    public static void voxelGriddableCanBeLinked(GameTestHelper test) {

        BlockPos posA = new BlockPos(10, 15, 2), posB = new BlockPos(3, 17, 9);
        test.setBlock(posA, MechanoBlocks.CONNECTOR_SINGLE.getDefaultState());
        test.setBlock(posB, MechanoBlocks.CONNECTOR_SINGLE.getDefaultState());

        ConnectorBlockEntity cbeA = test.getBlockEntity(posA), cbeB = test.getBlockEntity(posB);
        test.assertTrue(cbeA != null, "ConnectorBlockEntity A couldn't be located.");
        test.assertTrue(cbeB != null, "ConnectorBlockEntity B couldn't be located.");

        GridUUID idA = cbeA.getUUID(), idB = cbeB.getUUID();
        AncillaryNode startNode = cbeA.getDefaultAncillary(), endNode = cbeB.getDefaultAncillary();
        test.assertTrue(startNode != null, "ConnectorBlockEntity A couldn't provide a default ancilllary");
        test.assertTrue(endNode != null, "ConnectorBlockEntity B couldn't provide a default ancilllary");
        test.assertTrue(startNode.isSignificant(), "ConnectorBlockEntity A couldn't provide a significant ancillary");
        test.assertTrue(endNode.isSignificant(), "ConnectorBlockEntity B couldn't provide a significant ancillary");

        GridUUID startID = startNode.bindUUID(idA.copy());
        GridUUID endID = endNode.bindUUID(idB.copy());
        test.assertTrue(startID != idA, "ConnectorBlockEntity A's uuid copy returned the same instance");
        test.assertTrue(endID != idB, "ConnectorBlockEntity B's uuid copy returned the same instance");
        test.assertTrue(!idA.equals(startID), "The starting node didn't alter the binding of ConnectorBlockEntity A's uuid");
        test.assertTrue(!idB.equals(endID), "The starting node didn't alter the binding of ConnectorBlockEntity A's uuid");

        ComponentLink<?> link = new ComponentLink<>(MechanoTransmitters.HOOKUP.get(), startID, startNode, endID, endNode);
        link.validate();
        ComponentLink<?> linkInverted = link.flippedCopy();
        linkInverted.validate();
        test.assertTrue(link.getStart().equals(linkInverted.getEnd()) && link.getEnd().equals(linkInverted.getStart()), "The link and its inverted copy aren't functionally equal");

        ServerGrid grid = Grid.server(test.getLevel());
        GridAction linkResult = grid.addLink(link);
        test.assertTrue(linkResult.getActionType().indicatesSuccess(), "Linking returned failure case " + linkResult);

        List<ComponentLink<?>> linkAcquire = grid.getLinksBelongingTo(startID), linkAcquireInverted = grid.getLinksBelongingTo(endID);
        test.assertFalse(linkAcquire == null || linkAcquire.isEmpty(), "Link re-acquisition returned null or empty list");
        test.assertFalse(linkAcquireInverted == null || linkAcquireInverted.isEmpty(), "Inverted link re-acquisition returned null or empty list");
        test.assertTrue(linkAcquire.contains(link), "Link re-aquisition failed to identify original link");
        test.assertTrue(linkAcquireInverted.contains(linkInverted), "Link re-aquisition failed to identify inverted link");
        grid.warn(grid.writeManifest(test.makeMockPlayer(GameType.CREATIVE)));
        test.succeed();
    }

    private static class TestNode implements Node {

        private final String id;
        private final boolean isGrounded;
        private int index = -2;

        private TestNode(String id, boolean isGrounded) {
            this.id = id;
            this.isGrounded = isGrounded;
        }

        @Override
        public void updateOwnership(@Nullable Griddable<?> source, CircuitComponent parent, int index) {
            
        }

        @Override
        public @Nullable CircuitComponent getParentComponent() {
            return null;
        }

        @Override
        public boolean attach(Terminal pin) {
            return false;
        }

        @Override
        public boolean attach(@Nullable Griddable<?> source, AncillaryNode jack) {
            return false;
        }

        @Override
        public boolean detach(Terminal pin) {
            return false;
        }

        @Override
        public boolean detach(@Nullable Griddable<?> source, AncillaryNode jack) {
            return false;
        }

        @Override
        public List<AncillaryNode> getAncillaries() {
            return Collections.emptyList();
        }

        @Override
        public List<Terminal> getTerminals() {
            return Collections.emptyList();
        }

        @Override
        public double getVoltage() {
            return 0;
        }

        @Override
        public void setVoltage(double volts) {
            
        }

        @Override
        public int getNodalIndex() {
            return index;
        }

        @Override
        public void setNodalIndex(int index) {
            this.index = index;
        }

        @Override
        public int getCircuitIndex() {
            return -1;
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
        public String toString() {
            return getComponentID() + "[" + describeState() + "]";
        }
    }
}

