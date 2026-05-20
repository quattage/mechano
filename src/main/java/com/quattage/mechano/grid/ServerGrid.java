package com.quattage.mechano.grid;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.Griddable;
import com.quattage.mechano.api.transmitter.TransmitterType;
import com.quattage.mechano.api.transmitter.TransmitterType.UnionFactory;
import com.quattage.mechano.foundation.Disposable;
import com.quattage.mechano.foundation.numeric.EsoMath;
import com.quattage.mechano.grid.solver.BiCGStabStochastic;
import com.quattage.mechano.grid.solver.ConvergenceStatus;
import com.quattage.mechano.grid.solver.SolverAlgorithm;
import com.quattage.mechano.grid.topology.AncillaryNode;
import com.quattage.mechano.grid.topology.AncillaryPair;
import com.quattage.mechano.grid.topology.ComponentLink;
import com.quattage.mechano.grid.topology.core.CircuitComponent;
import com.quattage.mechano.grid.topology.core.MutableComponentReference;
import com.quattage.mechano.grid.topology.core.Node;
import com.quattage.mechano.grid.topology.core.NodePair;
import com.quattage.mechano.grid.topology.core.StampingComponent;
import com.quattage.mechano.grid.topology.core.StampingComponent.NeedsPostProcessing;
import com.quattage.mechano.grid.topology.core.StampingComponent.StampsDynamically;
import com.quattage.mechano.grid.topology.core.Terminal;
import com.quattage.mechano.infrastructure.ActionLog;
import com.quattage.mechano.infrastructure.ReflectionWizard.DoNotAnalyze;
import com.quattage.mechano.switchboard.RemovalLedger;
import com.quattage.mechano.switchboard.RemovalLedger.RemovalEntry;
import com.quattage.mechano.switchboard.action.GridAction;
import com.quattage.mechano.switchboard.action.GridAction.ActionSync;
import com.quattage.mechano.switchboard.task.DeferredTask;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.createmod.catnip.data.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

public final class ServerGrid extends Grid<AncillaryNode<?>> {

    public static final ActionLog ACTION_LOG = new ActionLog();
    private final SolverAlgorithm solver = new BiCGStabStochastic();
    private @Nullable ManifestProcessor activeManifest;
    private @Nullable Object2ObjectOpenHashMap<ChunkPos, ObjectOpenHashSet<AncillaryNode<?>>> chunkOccupation = new Object2ObjectOpenHashMap<>();
    private @Nullable ObjectArrayList<Netlist> netlists = new ObjectArrayList<>();
    private @Nullable Object2IntOpenHashMap<Node> netlistIndices;
    private @Nullable PriorityQueue<DeferredTask> taskQueue;
    private boolean hasUnsavedChanges = false;

    protected ServerGrid(Level world) {
        super(world);
        ServerGrid.ACTION_LOG.clear().enableOnto(this);
    }

    @Override
    protected void read(LevelReader world, CompoundTag contents, Provider provider) {
        
    }

    @Override
    protected void write(LevelReader world, CompoundTag contents, Provider provider) {

    }

    @Override
    public void load() {
        assertNotDisposed();
        for(Netlist netlist : netlists)
            netlist.setStatus(ConvergenceStatus.IDLE);
    }

    @Override
    public void tick() {
        if(hasBeenDisposed()) return;
        tickManifest();
        processQueuedTasks();
        for(int x = 0; x < netlists.size(); x++) {
            Netlist netlist = netlists.get(x);
            ServerGrid.ACTION_LOG.line("Ticking " + describeNetlist(netlist, false) + " (index " + x + ")");
            netlist.prepareAndStamp(this, x);
            netlist.runSolver(this, solver);
            for(StampingComponent sc : netlist.stampers)
                if(sc instanceof NeedsPostProcessing pp) 
                    pp.postProcess(this, netlist);
            ServerGrid.ACTION_LOG.divider();
        }
    }

    @Override
    public void dispose() {
        assertNotDisposed();
        for(Netlist netlist : netlists)
            netlist.dispose();
        netlists = null;
        if(activeManifest != null)
            activeManifest.dispose();
        taskQueue = null;
        links = null;
    }

    public Netlist getNetlist(int index) {
        if(index < 0 || index >= netlists.size()) 
            throw new ArrayIndexOutOfBoundsException("Index " + index + " is out of bounds for a grid with " + netlists.size() + " netlists");
        Netlist out = netlists.get(index);
        if(out == null) throw new NullPointerException("Failed while getting netlist at index " + index + " - This netlist is null!");
        if(out.hasBeenDisposed()) throw new IllegalStateException("Failed while getting netlist at index " + index + " - This netlist has been disposed!");
        return out;
    }

    public GridAction removeComponent(CircuitComponent component, @Nullable Entity modifier) {
        queueTask(GridAction.TASK_COMPONENT_DESTROY, MutableComponentReference.of(component), modifier);
        return GridAction.RESPONSE_SUCCESS;
    }

    public GridAction removeComponentDeferred(CircuitComponent component, RemovalLedger outdated) {
        component.forEachNode(node -> outdated.mark(this, node));
        Disposable.disposeOf(component);
        return GridAction.RESPONSE_SUCCESS;
    }

    @Override
    public GridAction addLink(AncillaryPair link, @Nullable Entity modifier) {
        queueTask(GridAction.TASK_LINK_CREATE, MutableComponentReference.of(link), modifier);
        return GridAction.RESPONSE_SUCCESS;
    }

    public GridAction createLinkDeferred(AncillaryPair link, @Nullable Entity modifier) {
        assertNotDisposed();
        ServerGrid.ACTION_LOG.line("Linking " + describe((NodePair)link));
        if(modifier != null) ServerGrid.ACTION_LOG.add(" " + modifier.getName());
        GridAction output = super.addLinkAsymmetric(link.getStartAncillary(), link, true);
        if(!output.getActionType().indicatesSuccess()) {
            ServerGrid.ACTION_LOG.line("  - !! early return failure state '" + output + "'");
            ServerGrid.ACTION_LOG.divider();
            return output;
        }
        super.addLinkAsymmetric(link.getEndAncillary(), link.flippedCopy(), true);
        Pair<Netlist, Integer> netlist = getOrMergeNetlists(indexOf(link.getStartNode()), indexOf(link.getEndNode()));
        allocate(link.getStartAncillary(), netlist.getSecond(), 0);
        allocate(link.getEndAncillary(), netlist.getSecond(), 0);
        Griddable<?> startSource = GridTracking.getReferentOrThrow(link.getStartAncillary());
        Griddable<?> endSource =  GridTracking.getReferentOrThrow(link.getEndAncillary());
        TransmitterType trns = null;
        if(link instanceof ComponentLink<?> cl) {
            CircuitComponent component = cl.makeComponent(this, netlist.getFirst(), netlist.getSecond());
            // TODO stamp link sub-component
            trns = cl.getTransmitter();
        } else UnionFactory.perfectConductor(netlist.getFirst(), link);
        initiateTask(GridAction.TASK_LINK_CREATE)
            .targeting(startSource, endSource)
            .withArguments(link.getStartID(), link.getEndID(), trns, modifier == null ? null : modifier.getUUID(), null)
            .executeOnClients();
        netlist.getFirst().setStatus(ConvergenceStatus.CHANGES_QUEUED);
        ServerGrid.ACTION_LOG.divider();
        return output;
    }

    protected Pair<Netlist, Integer> getOrMergeNetlists(int a, int b) {
        assertNotDisposed();
        ServerGrid.ACTION_LOG.line(">> Getting or merging netlists at element indices (" + a + ", " + b + ")");
        if(b < 0 && a >= 0) {
            Netlist output = netlists.get(a);
            ServerGrid.ACTION_LOG.line("  - returning pre-existing " + describeNetlist(output, false) + " (index " + a + ")");
            return Pair.of(output, a);
        }
        if(a < 0 && b >= 0) {
            Netlist output = netlists.get(b);
            ServerGrid.ACTION_LOG.line("  - returning pre-existing " + describeNetlist(output, false) + " (index " + b + ")");
            return Pair.of(output, b);
        }
        if(b < 0 && a < 0) {
            Netlist output = new Netlist(16);
            int idx = netlists.size();
            this.netlists.add(output);
            ServerGrid.ACTION_LOG.line("  - returning new " + describeNetlist(output, false) + " (index " + idx + ")");
            return Pair.of(output, idx);
        }
        if(a == b) {
            Netlist output = netlists.get(a);
            ServerGrid.ACTION_LOG.line("  - returning pre-existing " + describeNetlist(output, false) + " (index " + a + ")");
            return Pair.of(netlists.get(a), a);
        }
        Netlist anet = netlists.get(a), bnet = netlists.remove(b);
        ServerGrid.ACTION_LOG.line("  - collapsing contents onto pre-existing " + describeNetlist(anet, false) + " (index " + a + ")");
        ServerGrid.ACTION_LOG.line("    - removed " + describeNetlist(bnet, false) + " (index " + b + ")");
        anet.adjacencyTree = EsoMath.selectiveMerge(anet.adjacencyTree, bnet.adjacencyTree);
        anet.transitiveTree = EsoMath.selectiveMerge(anet.transitiveTree, bnet.transitiveTree);
        anet.stampers = EsoMath.selectiveMerge(anet.stampers, bnet.stampers);
        anet.nodeCoordinates = EsoMath.selectiveMerge(anet.nodeCoordinates, bnet.nodeCoordinates);
        anet.sourceCoodinates = EsoMath.selectiveMerge(anet.sourceCoodinates, bnet.sourceCoodinates);
        anet.indexerCursor += bnet.indexerCursor;
        anet.relations.ensureCapacity(anet.relations.size() + bnet.relations.size());
        bnet.relations.entrySet().forEach(entry -> {
            int prev = netlistIndices.put(entry.getKey(), a);
            if(prev != a) ServerGrid.ACTION_LOG.line("    - reassigned " + describe(entry.getKey()) + " to index " + a);
            prev = netlistIndices.put(entry.getValue(), a);
            if(prev != a) ServerGrid.ACTION_LOG.line("    - reassigned " + describe(entry.getValue()) + " to index " + a);
            anet.relations.put(entry.getKey(), entry.getValue());
        });
        EsoMath.selectiveMerge(anet.relations, bnet.relations);
        anet.setStatus(ConvergenceStatus.CHANGES_QUEUED);
        Disposable.disposeOf(bnet);
        for(int x = b; x < netlists.size(); x++) {
            Netlist nextAffected = getNetlist(x);
            nextAffected.setStatus(ConvergenceStatus.CHANGES_QUEUED);
            ServerGrid.ACTION_LOG.line("  - netlist removal affected adjacent " + describeNetlist(nextAffected, false) + " at index " + x);
        }
        return Pair.of(anet, a);
    }

    protected void allocate(Node node, int netlistIndex, int nodalIndex) {
        Objects.requireNonNull(node);
        assertNotDisposed();
        if(netlistIndex < 0 || netlistIndex >= netlistCount()) 
            throw new IndexOutOfBoundsException("Can't allocate node " + describe(node) + " at invalid netlist index " + netlistIndex);
        if(nodalIndex < 0) throw new IndexOutOfBoundsException("Can't allocate node " + describe(node) + " at invalid nodal index " + nodalIndex + " - The nodal index must be >= 0!");
        allocateUnsafe(node, netlistIndex, nodalIndex);
    }

    protected void allocate(Collection<Node> branch, int netlistIndex, int nodeCoordinate) {
        Objects.requireNonNull(branch);
        assertNotDisposed();
        if(netlistIndex < 0 || netlistIndex >= netlistCount()) 
            throw new IndexOutOfBoundsException("Can't allocate a collection of nodes at invalid index " + netlistIndex);
        if(netlistIndices == null) netlistIndices = new Object2IntOpenHashMap<>();
        if(nodeCoordinate < 0) throw new IndexOutOfBoundsException("Can't allocate a collection of nodes at invalid coordinate" + nodeCoordinate + " - The coordinate must be >= 0!");
        if(branch.size() <= 0) throw new IllegalArgumentException("Failed to allocate a collection of nodes because the provided collection was empty!");
        for(Node leaf : branch)
            allocateUnsafe(leaf, netlistIndex, nodeCoordinate);
    }

    private void allocateUnsafe(Node node, int netlistIndex, int nodeCoordinate) {
        ServerGrid.ACTION_LOG.line("  - allocating " + describe(node) + " at (" + netlistIndex + ", " + nodeCoordinate + ")");
        if(node.isGrounded()) return;
        if(node instanceof AncillaryNode an) {
            Griddable<?> source = GridTracking.getReferentOrThrow(an);
            ChunkPos cp = source.getChunkPos();
            if(!source.canMoveDynamically() && cp != null) {
                ObjectOpenHashSet<AncillaryNode<?>> preexisting = chunkOccupation.get(cp);
                if(preexisting == null) {
                    preexisting = new ObjectOpenHashSet<>();
                    chunkOccupation.put(cp, preexisting);
                }
                preexisting.add(an);
            }
            ServerGrid.ACTION_LOG.line("     -- in chunk (" + cp.getRegionX() + ", " + cp.getRegionZ() + ")");
            node = an.getAssociatedNode();
            if(node == null) throw new NullPointerException("Failed while allocating " + describe(an) + " - This ancillary's parent node is null!");
            node.assertNotDisposed();
            ServerGrid.ACTION_LOG.line("     -- as parent " + describe(node));
        } 
        if(netlistIndices == null) netlistIndices = new Object2IntOpenHashMap<>();
        netlistIndices.put(node, netlistIndex);
        Netlist netlist = getNetlist(netlistIndex);
        netlist.nodeCoordinates.put(node, nodeCoordinate);
    }

    protected void allocate(StampingComponent component, int netlistIndex, int nodeCoordinate) {
        Objects.requireNonNull(component);
        assertNotDisposed();
        if(netlistIndex < 0 || netlistIndex >= netlistCount()) 
            throw new IndexOutOfBoundsException("Can't allocate stamper " + describe(component) + " at invalid netlist index " + netlistIndex);
        Netlist netlist = getNetlist(netlistIndex);
        if(nodeCoordinate < 0) throw new IndexOutOfBoundsException("Can't allocate stamper " + describe(component) + " at invalid coordinate " + nodeCoordinate + " - The coordinate must be >= 0!");
        netlist.stampers.add(component);
        if(!(component instanceof StampsDynamically sd)) return;
        int size = sd.getAllocations();
        if(size <= 0) throw new IllegalArgumentException("Failed while allocating stamper " + describe(component) + " - This component supplied an invalid indexer size of " + size);
        netlist.sourceCoodinates.put(sd, netlist.indexerCursor);
        netlist.indexerCursor += size - 1;
    }

    @Override
    public GridAction removeLink(AncillaryPair link, @Nullable Entity modifier) {
        queueTask(GridAction.TASK_LINK_DESTROY, MutableComponentReference.of(link), modifier);
        return GridAction.RESPONSE_SUCCESS;
    }

    @Override
    public GridAction removeLink(AncillaryNode<?> a, AncillaryNode<?> b, @Nullable Entity modifier) {
        AncillaryPair link = getLinkMatching(a, b);
        if(link == null) return GridAction.RESPONSE_FAIL_MISSING;
        return removeLink(link, modifier);
    }

    public void queueTask(GridAction task, MutableComponentReference reference, @Nullable Entity caller) {
        Objects.requireNonNull(task);
        ServerGrid.ACTION_LOG.line("Queuing '" + task + "' to the process queue");
        assertNotDisposed();
        if(!task.isTask()) {
            error("Attempted to queue action '" + task + "' but this action is not a task type.");
            return;
        }
        if(!task.isWrappable()) {
            error("Attempted to queue action '" + task + "' but this task is forbidden from being queued.");
            return;
        }
        if(taskQueue == null) taskQueue = new PriorityQueue<>(16);
        DeferredTask toAdd = new DeferredTask(task, reference, caller, taskQueue.size());
        taskQueue.add(toAdd);
        this.hasUnsavedChanges = true;
        ServerGrid.ACTION_LOG.line("  - targeting " + describe(reference));
        if(caller != null) ServerGrid.ACTION_LOG.line("  - invoked by " + caller.getName());
        else ServerGrid.ACTION_LOG.line("  - invoked anonymously");
        ServerGrid.ACTION_LOG.divider();
    }

    private void processQueuedTasks() {
        if(taskQueue == null || taskQueue.isEmpty()) return;
        final RemovalLedger removals = new RemovalLedger();
        boolean brokeEarly = false;
        DeferredTask working = null;
        while(!taskQueue.isEmpty()) {
            working = taskQueue.poll();
            if(working.creates()) {
                brokeEarly = true;
                break;
            }
            working.run(this, removals);
        }
        ActionSync synchronizer = new ActionSync().in(this).action(GridAction.TASK_LINK_DESTROY);
        removeOutdated(synchronizer, removals);
        Disposable.disposeOf(removals);
        if(brokeEarly) working.run(this, removals);
        while(!taskQueue.isEmpty()) {
            working = taskQueue.poll();
            working.run(this, removals);
        }
        taskQueue = null;
    }

    private void removeOutdated(@Nullable ActionSync synchronizer, RemovalLedger removals) {
        removals.forEach((netlistIndex, outdated) -> {
            Netlist netlist = getNetlist(netlistIndex);
            ServerGrid.ACTION_LOG.line("Processing removals for " + describeNetlist(netlist, false) + " (index " + netlistIndex + ")");
            Map<Node, @Nullable Set<Node>> branches = processRemoval(netlist, netlistIndex, outdated);
            netlist.setStatus(ConvergenceStatus.CHANGES_QUEUED);
            outdated.forEachUnion(union -> finallyDeleteUnion(synchronizer, union));
            consumeOrphans(netlist, netlistIndex, outdated);
            ServerGrid.ACTION_LOG.line("Declusterizing " + describeNetlist(netlist, false) + " (index " + netlistIndex + ")");
            for(Map.Entry<Node, @Nullable Set<Node>> entry : branches.entrySet())
                netlist.applyTransitivity(this, entry.getKey(), entry.getValue());
            ServerGrid.ACTION_LOG.divider();
        });
    }

    private Map<Node, @Nullable Set<Node>> processRemoval(Netlist netlist, int netlistIndex, RemovalEntry outdated) {
        final Map<Node, @Nullable Set<Node>> affectedBranches = new HashMap<>();
        outdated.forEachAffected(node -> {
            if(node == null) throw new NullPointerException("Iterated over a RemovalEntry with a null node!");
            Node root = netlist.findTransitiveRoot(node);
            if(root == null) throw new NullPointerException("Iterated over RemovalEntry " 
                + describe(root) + " but a transitive root couldn't be located.");
            if(affectedBranches.containsKey(root)) return;
            Set<Node> branch = netlist.transitiveTree.get(root);
            if(branch == null) throw new NullPointerException("Iterated over removal entry "
                + describe(root) + " but this transitive root didn't contain a branch in the netlist for some reason.");
            affectedBranches.put(root, branch);
            ServerGrid.ACTION_LOG.line("  - flagged root " + describe(root) + " with " + branch.size() + " constituent(s)");
        });

        ServerGrid.ACTION_LOG.line("Processing " + outdated.nodeCount() + " node(s)");
        outdated.forEachNode(node -> forgetNode(netlist, node, outdated, false));

        ServerGrid.ACTION_LOG.line("Processing " + outdated.unionCount() + " union(s)");
        outdated.forEachUnion(union -> netlist.forgetUnion(this, outdated, union));
        return affectedBranches;
    }

    /**
     * quatworks ltd does not condone the consumption of orphans in any context other than this one
     */
    private void consumeOrphans(Netlist netlist, int netlistIndex, RemovalEntry outdated) {
        if(netlist.orphans != null) {
            ServerGrid.ACTION_LOG.line("Consuming " + netlist.orphans.size() + " orphan node(s) for " + describeNetlist(netlist, false) + " (index " + netlistIndex + ")");
            for(Node node : netlist.orphans) forgetNode(netlist, node, outdated, true);
            netlist.orphans = null;
        } else ServerGrid.ACTION_LOG.line("  - removal produced 0 orphans");
    }

    private void forgetNode(Netlist netlist, Node node, RemovalEntry outdated, boolean dispose) {
        netlist.forgetNode(this, outdated, node);
        if(node instanceof AncillaryNode an) {
            Griddable<?> source = GridTracking.getReferentOrThrow(an);
            ChunkPos pos = source.getChunkPos();
            if(!source.canMoveDynamically() && pos != null && chunkOccupation != null) {
                Set<AncillaryNode<?>> preexisting = chunkOccupation.get(pos);
                if(preexisting != null) preexisting.remove(an);
                if(chunkOccupation.isEmpty()) chunkOccupation = null;
            }
        }
        netlistIndices.removeInt(node);
        netlist.nodeCoordinates.removeInt(node);
        ServerGrid.ACTION_LOG.line("  - de-indexed " + describe(node));
        if(dispose) {
            ServerGrid.ACTION_LOG.line("  - disposed orphaned " + describe(node));
            Disposable.disposeOf(node);
        }
    }

    private GridAction finallyDeleteUnion(@Nullable ActionSync synchronizer, NodePair pair) {
        assertNotDisposed();
        ServerGrid.ACTION_LOG.line("  - finally deleting link " + describe(pair));
        if(pair instanceof AncillaryPair link)
            return finallyDeleteAP(synchronizer, link);
        List<AncillaryNode<?>> aAnc = pair.getNodeA().getAncillaries();
        List<AncillaryNode<?>> bAnc = pair.getNodeB().getAncillaries();
        if(aAnc == null || aAnc.isEmpty()) {
            error("Skipped deleting link " + pair + " - The starting node in this pair had no ancillaries to search from.");
            return GridAction.RESPONSE_FAIL_START_MISSING;
        }
        if(bAnc == null || bAnc.isEmpty()) {
            error("Skipped deleting link " + pair + " - The ending node in this pair had no ancillaries to search from.");
            return GridAction.RESPONSE_FAIL_END_MISSING;
        }
        for(AncillaryNode<?> an : aAnc) {
            for(AncillaryNode<?> bn : bAnc) {
                AncillaryPair toRemove = getLinkMatching(bn, an);
                finallyDeleteAP(synchronizer, toRemove);
            }
        }
        return GridAction.RESPONSE_SUCCESS;
    }

    private GridAction finallyDeleteAP(@Nullable ActionSync synchronizer, AncillaryPair link) {
        if(link == null) return GridAction.RESPONSE_FAIL_MISSING;
        AncillaryNode<?> start = link.getStartAncillary(), end = link.getEndAncillary();
        Griddable<?> startSource = GridTracking.getReferentOrThrow(start);
        Griddable<?> endSource = GridTracking.getReferentOrThrow(end);
        GridAction output = super.removeLinkAsymmetric(start, end);
        // TODO delink ancillarypair component
        super.removeLinkAsymmetric(end, start);
        if(synchronizer != null) {
            synchronizer.targeting(startSource, endSource)
            .withArguments(
                GridTracking.getAddress(startSource, start), 
                GridTracking.getAddress(endSource, end)
            ).executeOnClients();
        }
        Disposable.disposeOf(link);
        return output;
    }

    public int indexOf(Netlist netlist) {
        assertNotDisposed();
        Objects.requireNonNull(netlist);
        netlist.assertNotDisposed();
        if(netlists == null || netlists.isEmpty()) return -1;
        return netlists.indexOf(netlist);
    }

    public int indexOf(Node node) {
        assertNotDisposed();
        Objects.requireNonNull(node);
        node.assertNotDisposed();
        return netlistIndices == null ? -1 : netlistIndices.getOrDefault(node, -1);
    }

    public int indexOf(NodePair pair) {
        assertNotDisposed();
        Objects.requireNonNull(pair);
        int idx = netlistIndices == null ? -1 : netlistIndices.getOrDefault(pair.getNodeA(), -1);
        if(idx != netlistIndices.getOrDefault(pair.getNodeB(), -1))
            throw new IllegalStateException("Only one half of a NodePair was indexed");
        return idx;
    }

    public int coordOf(Node node) {
        assertNotDisposed();
        Objects.requireNonNull(node);
        node.assertNotDisposed();
        if(node.isGrounded()) return -1;
        int idx = indexOf(node);
        if(idx < 0) throw new IndexOutOfBoundsException("Can't get coodinate for " + describe(node) + " - This node returned a netlist index of " + idx + " (It probably doesn't exist!)");
        Netlist netlist = getNetlist(idx);
        idx = netlist.nodeCoordinates.getOrDefault(node, -2);
        return idx;
    }

    public int coordOf(Terminal terminal) {
        assertNotDisposed();
        Objects.requireNonNull(terminal);
        terminal.assertNotDisposed();
        Node attached = terminal.getAttachedNode();
        if(attached == null) throw new NullPointerException("Can't get coordinate for " + describe(terminal) + " - This terminal has no attached node!");
        return coordOf(attached);
    }

    public int coordOf(StampingComponent component) {
        assertNotDisposed();
        Objects.requireNonNull(component);
        component.assertNotDisposed();
        int netlistIndex = -1;
        for(int x = 0; x < component.getTerminals().length; x++) {
            Terminal term = component.getTerminals()[x];
            Objects.requireNonNull(term);
            term.assertNotDisposed();
            Node attached = term.getAttachedNode();
            if(attached == null) throw new NullPointerException("Can't get coordinate for " + describe(component) + " - This terminal has no attached node!");
            int idx = indexOf(attached);
            if(idx >= 0) {
                netlistIndex = idx;
                break;
            }
        }
        Netlist netlist = getNetlist(netlistIndex);
        return netlist.sourceCoodinates.getOrDefault(component, -1);
    }

    @Override
    public Stream<AncillaryPair> getLinksByChunk(ChunkPos pos) {
        assertNotDisposed();
        return Optional.ofNullable(chunkOccupation.get(pos))
            .stream()
            .flatMap(Set::stream)
            .flatMap(node -> 
                Optional.ofNullable(links.get(node))
                    .stream().flatMap(List::stream)
            );
    }

    @Override
    public int netlistCount() {
        return netlists == null ? 0 : netlists.size();
    }

    @Override
    public int linkCount() {
        if(links == null) return 0;
        int size = 0;
        for(List<AncillaryPair> links : this.links.values())
            size += links.size();
        return size;
    }

    public int nodeCount() {
        return netlistIndices == null ? 0 : netlistIndices.size();
    }

    public List<Netlist> netlists() {
        return netlists;
    }

    public SolverAlgorithm solver() {
        return solver;
    }

    private void tickManifest() {
        if(activeManifest != null) { 
            boolean consumed = activeManifest.tick();
            if(consumed) activeManifest = null;
        };
    }





    /* 
        The rest of the stuff in this class is just for creating formatted debug strings,
        so if you're paroosing this class trying to untagle this inscrutible mess, just 
        know that the code under this comment isn't strictly required nor is it relevent
        to the function of the Grid API.
    */


    public String describe(@Nullable CircuitComponent component) {
        if(component == null) return "(null)";
        return component.getComponentID() + " @" + component.hashCode();
    }

    public String describe(@Nullable NodePair pair) {
        if(pair == null) return "(null)";
        return "[" + describe(pair.getNodeA()) + " → " + describe(pair.getNodeB()) + "]";
    }

    public String describe(MutableComponentReference reference) {
        if(reference == null) return "(null)";
        if(reference.isNodePair()) return describe(reference.asNodePair());
        if(reference.isCircuitComponent()) return describe(reference.asComponent());
        if(reference.isNode()) return describe(reference.asNode());
        return "what? the fuck??";
    }

    public String describe(Set<Node> nodes) {
        if(nodes == null) return "(null)";
        String output = "";
        for(Node node : nodes)
            output += describe(node) + "\n";
        return output;
    }

    public String describeNetlist(@Nullable Netlist netlist, boolean includeDetails) {
        if(netlist == null) return "(null)";
        String out = "(netlist @" + netlist.hashCode() + ")";
        if(!includeDetails) return out;

        out += ":\n";
        if(netlist.hasBeenDisposed())
            return out + " (!! disposed)";
        out += "  transitive:\n";
        if(netlist.transitiveTree.isEmpty())
            out += "   (empty)\n";
        else {
            for(Map.Entry<Node, ObjectOpenHashSet<Node>> entry : netlist.transitiveTree.entrySet()) {
                out += "    ⬥ " + describe(entry.getKey()) + "\n";
                for(Node node : entry.getValue())
                    out += "     ┕ " + (node == entry.getKey() ? "self ↺" : describe(node)) + "\n";
            }
        }
        out += "  adjacent:\n";
        if(netlist.adjacencyTree.isEmpty())
            out += "   (empty)\n";
        for(Map.Entry<Node, ObjectOpenHashSet<Node>> entry : netlist.adjacencyTree.entrySet()) {
            out += "    ⬥ " + describe(entry.getKey()) + "\n";
            if(entry.getValue().isEmpty()) {
                out += "       ┕ !! orphan !!\n";
                continue;
            }
            for(Node node : entry.getValue())
                out += "       ┕ " + describe(node) + "\n";
        }
        out += "  relative:\n";
        if(netlist.relations.isEmpty())
            out += "   (empty)\n";
        String roots = "";
        for(Map.Entry<Node, Node> entry : netlist.relations.entrySet()) {
            if(entry.getKey().equals(entry.getValue())) {
                roots += "    ⬥ root (" + describe(entry.getKey()) + ") ↺ \n";
                continue;
            }
            out += "    ⬥ " + describe(new NodePair(entry.getKey(), entry.getValue())) + "\n";
        }
        return out + roots + "--";
    }

    public String describeIndices() {
        String out = "Indexer:\n";
        if(netlistIndices == null || netlistIndices.isEmpty())
            return out + " - empty";
        for(Map.Entry<Node, Integer> entry : netlistIndices.object2IntEntrySet())
            out += " - " + describe(entry.getKey()) + "  → " + entry.getValue() + "\n";
        return out + "--";
    }

    public String describeLinks() {
        String out = "Lookup:\n";
        if(links == null || links.isEmpty())
            return out + " - empty\n";
        for(Map.Entry<AncillaryNode<?>, List<AncillaryPair>> entry : links.entrySet()) {
            out += " - " + describe(entry.getKey());
            ChunkPos pos = null;
            for(Map.Entry<ChunkPos, ObjectOpenHashSet<AncillaryNode<?>>> posEntry : chunkOccupation.entrySet()) {
                if(posEntry.getValue().contains(entry.getKey()))
                    pos = posEntry.getKey();
            }
            if(pos == null) out += ", (no chunk)";
            else out += ", (chunk " + pos.getRegionX() + ", " + pos.getRegionZ() + ")";
            out += ":\n";
            if(entry.getValue() == null || entry.getValue().isEmpty()) {
                out += "   - empty\n";
                continue;
            }
            for(AncillaryPair pair : entry.getValue())
                out += "   - " + describe((NodePair)pair) + "\n";
        }
        return out + "--";
    }

    /**
     * Writes a new manifest String for this grid and 
     * returns the string immediately without off-thread execution
     * or file writing. This operation will take quite a long time
     * to complete, so avoid calling this in production code.
     * @param grid Grid to get a manifest for
     * @return A formatted string containing manifest data
     * @see {@link #makeManifestAsync the async version of this method}
     */
    public String makeManifest() {
        ManifestProcessor manifest = new ManifestProcessor(this, null);
        manifest.generate();
        Disposable.disposeOf(manifest);
        return manifest.toString();
    }

    public ManifestProcessor makeManifestAsync(@Nullable Entity requester) {
        ManifestProcessor manifest = new ManifestProcessor(this, requester);
        manifest.generator = CompletableFuture.runAsync(manifest::generate)
        .orTimeout(30L, TimeUnit.SECONDS).whenComplete((result, ex) -> {
            if(ex != null) {
                ex.printStackTrace();
                manifest.setText((ex instanceof TimeoutException 
                    ? "⚠ Grid manifest request timed out." 
                    : "⚠ Unknown error compiling grid manifest! (See console)")
                );
                return;
            }
        });
        Disposable.disposeOf(manifest);
        return manifest;
    }

    @DoNotAnalyze
    private static class ManifestProcessor extends ActionLog implements Disposable {

        private Throbber throbber = new Throbber();
        private @Nullable ServerGrid grid;
        private @Nullable Entity requester;
        private Future<Void> generator;
        private long tickTime = 0L;

        private ManifestProcessor(ServerLevel world, Entity requester) {
            this(Grid.server(world), requester);
        }

        private void setText(String text) {
            this.text = text;
        }

        private ManifestProcessor(ServerGrid grid, @Nullable Entity requester) {
            this.grid = grid;
            this.requester = requester;
        }

        private void generate() {
            text = "";
            text += grid.describeLinks() + "\n";
            text += grid.describeIndices() + "\n";
            for(int x = 0; x < grid.netlistCount(); x++) {
                Netlist netlist = grid.getNetlist(x);
                text += "[▧ " + x + "] " + grid.describeNetlist(netlist, true);
            }
        }

        private boolean tick() {
            if(hasBeenDisposed()) return false;
            if(generator.isCancelled()) {
                clear();
                super.enable();
                return false;
            }
            if(generator.isDone()) {
                save(ActionLog.makeFileLocation("dumps\\grid_manifest.log"));
                tryPromptRequester();
                Disposable.disposeOf(this);
                return true;
            }
            if(requester instanceof ServerPlayer sp) {
                long now = System.currentTimeMillis();
                if(now - tickTime >= 150L) {
                    tickTime = now;
                    sp.sendSystemMessage(Component.literal("Compiling grid manifest " + throbber.throb())
                        .withStyle(style -> style.withColor(ChatFormatting.AQUA)), true);
                }
            }
            return true;
        }

        private void tryPromptRequester() {
            if(!(requester instanceof ServerPlayer sp)) 
                return;
            if(text.startsWith("⚠")) {
                sp.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.RED), true);
                return;
            }
            String dir = ActionLog.makeFileLocation("dumps\\grid_manifest.log");
            sp.sendSystemMessage(Component.literal(""), true);
                sp.sendSystemMessage(Component.literal("Manifest saved to")
                    .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(" [").withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY).withBold(true)))
                        .append(Component.literal(dir)
                            .withStyle(style -> style
                                .withColor(ChatFormatting.GREEN)
                                .withUnderlined(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, dir))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy")))
                            ))
                        .append(Component.literal("] ").withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY).withBold(true)))
                    , false);
        }

        @Override
        public void dispose() {
            if(generator != null)
                generator.cancel(true);
            this.grid = null;
            this.throbber = null;
            this.requester = null;
            this.generator = null;
            tickTime = 0;
        }

        @Override
        public boolean hasBeenDisposed() {
            return grid == null;
        }

        private static class Throbber {

            private int index = -1;
            private static final String chars = " ▁▂▃▅▆▇▆▅▃▂";

            protected char throb() {
                index++;
                if(index >= Throbber.chars.length()) index = 0;
                return Throbber.chars.charAt(index);
            }

            protected void reset() {
                index = -1;
            }
        }
    }
}