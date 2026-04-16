package com.quattage.mechano.infrastructure.gametest;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.MechanoBlocks;
import com.quattage.mechano.MechanoTransmitters;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.GridTracking;
import com.quattage.mechano.api.grid.GridUUID;
import com.quattage.mechano.api.grid.GridUUID.UUIDComposite;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.GriddableTerminus;
import com.quattage.mechano.api.grid.HierarchicalConstruct;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.topology.GridDomain;
import com.quattage.mechano.api.grid.topology.NodeUnionSet;
import com.quattage.mechano.api.grid.topology.landmark.AncillaryNode;
import com.quattage.mechano.api.grid.topology.landmark.Node;
import com.quattage.mechano.api.grid.topology.landmark.Terminal;
import com.quattage.mechano.api.grid.topology.landmark.link.AncillaryPair;
import com.quattage.mechano.api.switchboard.action.ActionTask;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.api.switchboard.action.GridAction.ActionRunner;
import com.quattage.mechano.content.connector.ConnectorBlockEntity;
import com.quattage.mechano.foundation.numeric.Bifrucated64;
import com.quattage.mechano.foundation.numeric.EsoMath;
import com.quattage.mechano.infrastructure.EnqueuedGridManifest;
import com.simibubi.create.foundation.mixin.accessor.GameTestHelperAccessor;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MechanoGameTestHelper extends GameTestHelper {

    private static int RAND_SCOPE = 100000;

    public static MechanoGameTestHelper of(GameTestHelper original) {
		GameTestHelperAccessor access = (GameTestHelperAccessor) original;
		MechanoGameTestHelper helper = new MechanoGameTestHelper(access.getTestInfo());
		GameTestHelperAccessor newAccess = (GameTestHelperAccessor) helper;
		newAccess.setFinalCheckAdded(access.getFinalCheckAdded());
		return helper;
	}

    public MechanoGameTestHelper(GameTestInfo testInfo) {
        super(testInfo);
    }

    public void runRepeatedly(int iterations, Runnable runner) {
        for(int x = 0; x < iterations; x++) {
            try { runner.run(); } catch (Exception e) { throw new GameTestIterativeRunException(e); }
        }
    }

    public MockNode populateUF(NodeUnionSet uf, String prefix, int length) {
        MockNode prev = null;
        for(int x = 0; x < length; x++) {
            MockNode node = new MockNode(prefix + x, false);
            if(prev != null) uf.union(prev, node);
            prev = node;
        }
        return prev;
    }

    public void tickGrid() {
        getGrid().tick();
    }

    public void dumpGrid() {
        dumpGrid(true);
    }

    public void dumpGrid(String clarify) {
        dumpGrid(true, clarify);
    }

    public String dumpGrid(boolean log) {
        return dumpGrid(log, "");
    }

    public String dumpGrid(boolean log, String clarify) {
        String output = "[Empty]";
        try { output = EnqueuedGridManifest.getImmediately(getGrid()); }
        catch (Exception e) {
            e.printStackTrace();
            fail("Grid manifest acquisition failed to resolve (See stacktrace above)");
        }
        if(log)
            getGrid().warn(clarify + " dumped test manifest:\n" + output);
        return output;
    }

    public ServerGrid getGrid() {
        return Grid.server(getLevel());
    }

    public void runTaskOrFail(GridAction action, Consumer<ActionRunner> runnerCons) {
        failIfNull(action, "Failed while running grid task - The provided action is null!");
        assertTrue(action.isTask(), "Failed while running grid task - The provided action is not a task!");
        failIfNull(runnerCons, "Failed while running grid task - The provided runner consumer is null!");
        ActionRunner runner = getGrid().initiateTask(action);
        failIfNull(runner, "Failed while running grid task - The action '" + action + "' couldn't provide an action runner!");
        runnerCons.accept(runner);
        getGrid().debug("Running grid task " + action);
        try { runner.executeImmediately(); } 
        catch(Exception e) {
            e.printStackTrace();
            fail("Failed while running grid task '" + action + "' (See stacktrace above)");
        }
    }

    public GridDomain getTestDomain() {
        List<GridDomain> domains = getGrid().domains();
        if(domains.isEmpty())
            return getGrid().makeFreshDomain();
        return domains.getFirst();
    }

    public GriddableTerminus getTerminus(Griddable<?> source) {
        GriddableTerminus output = null;
        try { output = source.getTerminus(); } catch (Exception e) {
            e.printStackTrace();
            fail("Couldn't initialize GriddableTerminus for " + source + "(see exception above)");
        }
        failIfNull(output, "Couldn't find terminus at " + source);
        return output;
    }

    public CircuitComponent getComponentSafely(GridUUID<?> address) {
        failIfNull(address);
        CircuitComponent result = null;
        try { result = GridTracking.getComponentOrThrow(getGrid(), address); } 
        catch (Exception e) { 
            e.printStackTrace(); 
            fail("Couldn't resolve component acquisition for " + address + " (see exception above)");
        }
        failIfNull(result, "Couldn't find component at " + address);
        return result;
    }

    public ConnectorBlockEntity placeConnector(BlockPos pos) {
        failIfNull(pos);
        BlockPos abs = absolutePos(pos);
        abs = new BlockPos(abs.getX(), Mth.clamp(abs.getY(), -60, 256), abs.getZ());
        boolean blockSet = getLevel().setBlock(abs, MechanoBlocks.CONNECTOR_SINGLE.getDefaultState(), 3);
        if(!blockSet) throw new GameTestAssertException("Block at " + pos + "(" + abs + ") resulted in no change!");
        BlockEntity be = getLevel().getBlockEntity(abs);
        if(!(be instanceof ConnectorBlockEntity cbe))
            throw new GameTestAssertException("Connector BlockEntity couldn't be acquired at " + pos);
        Grid grid = Grid.server(getLevel());
        assertTrue(GridTracking.isReachable(grid.getWorld(), cbe), "ConnectorBlockEntity couldn't be reached by active grid.");
        return cbe;
    }

    public void checkNumeric(BiFunction<BigDecimal, BigDecimal, BigDecimal> stimulus, BiFunction<Bifrucated64, Bifrucated64, Bifrucated64> response) {
        BigDecimal a = new BigDecimal(random()), b = new BigDecimal(random()), parsedValue;
        Bifrucated64 testA = new Bifrucated64(a.toPlainString());
        Bifrucated64 testB = new Bifrucated64(b.toPlainString());
        BigDecimal referenceValue = stimulus.apply(a, b);
        Bifrucated64 actualValue = response.apply(testA, testB);
        if(actualValue == null) { fail("(" + a + ", " + b + ") Response function returned a null value!"); return; }
        String realValueAsString = actualValue.toString();
        try { parsedValue = new BigDecimal(realValueAsString); } catch (NullPointerException | NumberFormatException e) {
            fail("(" + a + ", " + b + ") string parse failure: expected " + String.format("%.2f", referenceValue) + ", got " + realValueAsString);
            return;
        }
        if(!fuzzyEquals(referenceValue, parsedValue, Bifrucated64.EPSILON)) {
            fail("(" + a + ", " + b + ") string value mismatch: expected " + String.format("%.2f", referenceValue) + ", got " + String.format("%.2f", parsedValue));
            return;
        }
        if(!fuzzyEquals(referenceValue, actualValue.bigValue(), Bifrucated64.EPSILON)) {
            fail("(" + a + ", " + b + ") raw value mismatch: expected " + String.format("%.2f", referenceValue) + ", got " + String.format("%.2f", actualValue.doubleValue()));
            return;
        }
    }

    public boolean verifyArgumentTemplateFor(GridAction action, ActionTask task) {
        Class<?>[] template;
        if(action.getTask() == null) {
            fail("Task for action '" + action + "' returned null, despite being a task type!");
            return true;
        }
        try { template = task.getArgumentTemplate(); }
        catch (Exception e) {
            e.printStackTrace();
            fail("Encountered an exception while getting argument template for '" + task.getClass().getSimpleName() + "'");
            return true;
        }
        if(template == null) return false;
        for(int x = 0; x < template.length; x++) {
            Class<?> expected = template[x];
            if(expected == null) {
                fail("Task '" + task.getClass().getSimpleName() + "' argument template contained a null class at index " + x);
                return true;
            }
        }
        return false;
    }

    /**
     * Places two connectors and attaches them together using
     * {@link MechanoTransmitters#PERFECT_CONDUCTOR}
     * @param bp An arbitrary position to use when placing connectors
     * @return A new {@link AncillaryPair} that's already been added to the grid
     */
    public AncillaryPair generateUnion(BlockPos bp) {

        ConnectorBlockEntity cbeA = placeConnector(bp);
        ConnectorBlockEntity cbeB = placeConnector(bp.offset(0, 0, 2));

        GridUUID<?> idA = cbeA.getUUID();
        GridUUID<?> idB = cbeB.getUUID();
        failIfNull(idA, "ConnectorBlockEntity A couldn't provide a UUID");
        failIfNull(idB, "ConnectorBlockEntity B couldn't provide a UUID");
        AncillaryNode<?> startNode = cbeA.getDefaultAncillary();
        AncillaryNode<?> endNode = cbeB.getDefaultAncillary();
        failIfNull(startNode, "ConnectorBlockEntity A couldn't provide a default ancilllary");
        failIfNull(endNode, "ConnectorBlockEntity B couldn't provide a default ancilllary");

        GridUUID<?> startID = GridTracking.getAddress(cbeA, startNode);
        GridUUID<?> endID = GridTracking.getAddress(cbeB, endNode);

        assertTrue(startID != idA, "ConnectorBlockEntity A's uuid copy returned the same instance");
        assertTrue(endID != idB, "ConnectorBlockEntity B's uuid copy returned the same instance");
        assertTrue(!idA.equals(startID), "The starting node didn't alter the binding of ConnectorBlockEntity A's uuid");
        assertTrue(!idB.equals(endID), "The starting node didn't alter the binding of ConnectorBlockEntity A's uuid");

        runTaskOrFail(GridAction.TASK_LINK_CREATE, 
            runner -> {
                runner.targeting(startNode, endNode);
                runner.withArguments(  
                    startID, 
                    endID, 
                    MechanoTransmitters.PERFECT_CONDUCTOR.get(), 
                    makeMockPlayer(GameType.CREATIVE).getUUID()
                );
            }
        );

        tickGrid();
        AncillaryPair link = getGrid().lookup().getLink(startNode, endNode);
        failIfNull(link, "Couldn't re-aquire newly created link");
        return link;
    }

    public AncillaryPair generateUnion(AncillaryNode<?> startNode, AncillaryNode<?> endNode) {

        failIfNull(startNode);
        failIfNull(endNode);
        GridUUID<?> startID = GridTracking.getAddress((HierarchicalConstruct)startNode);
        GridUUID<?> endID = GridTracking.getAddress((HierarchicalConstruct)endNode);

        runTaskOrFail(GridAction.TASK_LINK_CREATE, 
            runner -> {
                runner.targeting(startNode, endNode);
                runner.withArguments(  
                    startID, 
                    endID, 
                    MechanoTransmitters.PERFECT_CONDUCTOR.get(), 
                    makeMockPlayer(GameType.CREATIVE).getUUID()
                );
            }
        );

        tickGrid();
        AncillaryPair link = getGrid().lookup().getLink(startNode, endNode);
        failIfNull(link, "Couldn't re-aquire newly created link");
        return link;
    }

    /**
     * for some reason minecraft's vanilla test runner appears to swallow all exceptions that 
     * occur within the scope of a test function and produce a log error without the stacktrace
     */
    public void tryOrFailVerbosely(Runnable action, String message) {
        try{ action.run(); }
        catch (Exception e) {
            e.printStackTrace();
            fail(message + " (See stacktrace above)");
        }
    }

    public void failIfNull(Object obj) {
        failIfNull(obj, "Couldn't perform action because an input object was null");
    }

    public void failIfNull(Object obj, String message) {
        if(obj == null) fail(message);
    }

    public BlockPos randomPos() {
        return new BlockPos(randomInt(), randomInt(-20, 256), randomInt());
    }

    public int randomInt() {
        return EsoMath.randomInt(getLevel().random);
    }

    public int randomInt(int min, int max) {
        return EsoMath.randomInt(getLevel().random, min, max);
    }

    public double random() {
        return random(MechanoGameTestHelper.RAND_SCOPE);
    }
    
    public double random(double mag) {
        return getLevel().random.nextDouble() * mag;
    }

    public boolean fuzzyEquals(BigDecimal a, BigDecimal b, double eps) {
        BigDecimal sub = a.subtract(b).abs();
        return sub.doubleValue() < eps;
    }

    public void assertFuzzyEquals(BigDecimal expected, BigDecimal actual, double eps) {
        if(!fuzzyEquals(expected, actual, eps)) throw new GameTestAssertException("Value " + expected + " is too dissimilar to " + actual);
    }

    private static class GameTestIterativeRunException extends RuntimeException {
        private GameTestIterativeRunException(Exception parent) {
            super(parent);
        }
        private GameTestIterativeRunException(int iter, int max) {
            super("An error occured at iteration " + iter + "/" + max + " of this gametest! (See above for details)");
        }
    }

    public static class MockNode extends Node {

        private final String id;
        private final boolean isGrounded;
        private boolean isMP = false;
        private int domainIndex = -2;

        public MockNode(String id, boolean isGrounded) {
            this.id = id;
            this.isGrounded = isGrounded;
        }

        @Override
        public @Nullable HierarchicalConstruct getParentConstruct() {
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

        public MockNode setPrimary() {
            this.isMP = true;
            return this;
        }

        @Override
        public int getMergePriority() {
            return isMP ? -50 : isGrounded ? -5 : 2;
        }

        @Override
        public Terminal[] getTerminals() {
            return new Terminal[0];
        }

        @Override
        public void dispose() {
            domainIndex = -2;
        }

        @Override
        public boolean hasBeenDisposed() {
            return false;
        }

        @Override
        public boolean isGrounded() {
            return isGrounded;
        }

        @Override
        public void markGrounded(boolean isGrounded) {
            isGrounded = true;
        }

        @Override
        public String getComponentID() {
            return id;
        }

        @Override
        public @Nullable CircuitComponent getComponent(UUIDComposite binding) {
            return this;
        }

        @Override
        public GridReferent<?> getReferent() {
            return null;
        }

        @Override
        public int getDomainIndex() {
            return domainIndex;
        }

        @Override
        public void setDomainIndex(int domainIndex) {
            this.domainIndex = domainIndex;
        }
    }
}
