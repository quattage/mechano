package com.quattage.mechano.api;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.grid.GridTracking;
import com.quattage.mechano.api.grid.GridUUID;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.component.StampingComponent;
import com.quattage.mechano.api.grid.component.StampingComponent.NeedsPostProcessing;
import com.quattage.mechano.api.grid.solver.NodalSolver;
import com.quattage.mechano.api.grid.solver.NodalSolver.ConvergenceStatus;
import com.quattage.mechano.api.grid.solver.NodalSolver.ConvergenceStatusHolder;
import com.quattage.mechano.api.grid.solver.StabilizedBiconjucateSolver;
import com.quattage.mechano.api.grid.topology.MNAIndexer;
import com.quattage.mechano.api.grid.topology.NetlistLookup.ServerNetlistLookup;
import com.quattage.mechano.api.grid.topology.NodeUnionSet;
import com.quattage.mechano.api.grid.topology.NodeUnionSet.NodePair;
import com.quattage.mechano.api.grid.topology.landmark.AncillaryNode;
import com.quattage.mechano.api.grid.topology.landmark.AncillaryPair;
import com.quattage.mechano.api.grid.topology.landmark.ComponentLink;
import com.quattage.mechano.api.grid.topology.landmark.Node;
import com.quattage.mechano.api.grid.topology.landmark.Node.GroundNode;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.api.switchboard.action.GridAction.ActionRunner;
import com.quattage.mechano.foundation.Disposable;
import com.quattage.mechano.foundation.numeric.EsoMath;
import com.quattage.mechano.infrastructure.EnqueuedGridManifest;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public final class ServerGrid extends Grid {

    private @Nullable EnqueuedGridManifest activeManifest;
    private @Nullable GroundNode commonGround;    
    private final MNAIndexer indexer = new MNAIndexer();
    private final ServerNetlistLookup lookup = new ServerNetlistLookup();
    protected final NodeUnionSet netlist = new NodeUnionSet();
    private NodalSolver solver = new StabilizedBiconjucateSolver();
    protected final ConvergenceStatusHolder status = new ConvergenceStatusHolder();

    private @Nullable DMatrixSparseCSC matrixA;
    private @Nullable DMatrixRMaj matrixX, matrixB;

    private final TopologyProcessQueue processQueue = new TopologyProcessQueue();

    protected ServerGrid(Level world) {
        super(world);
    }

    @Override
    protected void read(LevelReader world, CompoundTag contents, Provider provider) {
        if(contents.isEmpty()) return;
        ListTag netlistTag = contents.getList("netlist", Tag.TAG_COMPOUND);
        if(netlistTag != null) readLinks(netlistTag);
    }

    @Override
    protected void write(LevelReader world, CompoundTag contents, Provider provider) {
        if(processQueue.hasChanges())
            processQueue.applyTo(this);
        contents.put("netlist", writeLinks());
        debug("Saved " + netlist.deepSize() + " nodes");
    }

    private ListTag writeLinks() {
        ListTag output = new ListTag(netlist.size());
        if(netlist.isEmpty()) return output;
        for(Map.Entry<AncillaryNode<?>, List<AncillaryPair>> branch : lookup.all()) {
            AncillaryNode<?> key = branch.getKey();
            List<AncillaryPair> contents = branch.getValue();
            if(contents.isEmpty()) continue;
            CompoundTag label = new CompoundTag();
            GridUUID<?> id = GridTracking.getAddress(key.getProviderSource(), key);
            ListTag connections = new ListTag(contents.size());
            for(AncillaryPair link : contents) {
                CompoundTag newTag = new CompoundTag();
                AncillaryNode<?> node = link.getEndAncillary();
                GridUUID<?> newID = GridTracking.getAddress(node.getProviderSource(), node);
                newID.write(newTag);
                int trnsType = -1;
                if(link instanceof ComponentLink<?> cl)
                    trnsType = Mechano.REGISTRATE.getTransmitterRegistry().getId(cl.getTransmitter());
                newTag.putInt("trns", trnsType);
                connections.add(newTag);
            }
            id.write(label);
            label.put("conns", connections);    
            output.add(label);
        }
        return output;
    }

    private void readLinks(ListTag tag) {
        this.status.set(ConvergenceStatus.LOADING);
        netlist.reset();
        netlist.ensureCapacity(tag.size());
        lookup.reset();
        for(int x = 0; x < tag.size(); x++) {
            CompoundTag label = tag.getCompound(x);
            GridUUID<?> startID = GridTracking.read(label);
            CircuitComponent startComp = GridTracking.getComponent(this, startID);
            if(!(startComp instanceof AncillaryNode start)) {
                warn("Skipped loading node at " + startID + " - No node could be found at this ID");
                continue;
            }
            ListTag connections = label.getList("conns", Tag.TAG_COMPOUND);
            if(connections == null || connections.isEmpty()) {
                warn("Skipped loading node at " + startID + " - This node didn't have any serialized connections");
                continue;
            }
            for(int y = 0; y < connections.size(); y++) {
                CompoundTag endLabel = connections.getCompound(y);
                GridUUID<?> endID = GridTracking.read(endLabel);
                int trnsType = endLabel.getInt("trns");
                CircuitComponent endComp = GridTracking.getComponent(this, endID);
                if(!(endComp instanceof AncillaryNode end)) continue;
                AncillaryPair addedLink = null;
                if(netlist.loadUnion(start.getAssociatedNode(), end.getAssociatedNode())) {
                    addedLink = trnsType > -1 
                        ? new ComponentLink<>(Mechano.REGISTRATE.getTransmitterRegistry().byId(trnsType), startID, start, endID, end)
                        : new AncillaryPair(start, end);
                }
                lookup.add(this, addedLink);
                addedLink.MNAAllocate(this);
            }
        }
        netlist.patchAndTrim();
        preProcess();
    }

    @Override
    public void load() {
        
    }

    @Override
    public void unload() {
        status.set(this, ConvergenceStatus.REFRESHING_TOPOLOGY);
        clearAll();
        solver.reset();
        lookup.reset();
        netlist.reset();
        indexer.clear();
        processQueue.clear();
        status.set(this, ConvergenceStatus.UNLOADED);
    }

    @Override
    public void tick() {
        tickManifest();
        if(status.isUnloaded()) return;
        if(processQueue.hasChanges())
            preProcess();
        if(indexer.hasStampers()) {
            if(matrixX != null) {
                runSolver();
                postProcess();
            }
        } else setIdle();
    }

    private void preProcess() {
        status.set(this, ConvergenceStatus.REFRESHING_TOPOLOGY);
        processQueue.applyTo(this);
        netlist.finalizeTopology(indexer);
        setMatrices();
        matrixA.sortIndices(null);
        solver.initialize(this);
    }

    private void setMatrices() {
        int size = indexer.size();
        matrixA = new DMatrixSparseCSC(size, size);
        matrixX = new DMatrixRMaj(size, 1);
        matrixB = new DMatrixRMaj(size, 1);
    }

    private void setIdle() {
        this.status.set(this, ConvergenceStatus.IDLE);
        matrixA = null; matrixX = null; matrixB = null;
        indexer.clear();
    }

    private void runSolver() {
        this.status.set(this, ConvergenceStatus.COMPUTING_SOLUTION);
        ConvergenceStatus newStatus = solver.run(this);
        if(newStatus == null || newStatus == ConvergenceStatus.UNLOADED) {
            warn("Failed to retrieve status for tick, solver run returned no status!");
            newStatus = ConvergenceStatus.UNLOADED;
        }
        this.status.set(this, newStatus);
    }

    private void postProcess() {
        for(StampingComponent sc : indexer.getStampers())
            if(sc instanceof NeedsPostProcessing pp) pp.postProcess(this);
    }

    public GridAction removeLinkDeferred(AncillaryPair link, @Nullable Entity modifier) {
        Objects.requireNonNull(link);
        processQueue.add(this, GridAction.TASK_LINK_DESTROY, link, modifier);
        return GridAction.RESPONSE_SUCCESS;
    }

    public GridAction removeComponent(CircuitComponent component, @Nullable Entity modifier) {
        Objects.requireNonNull(component);
            processQueue.add(this, GridAction.TASK_COMPONENT_DESTROY, component, modifier);
        return GridAction.RESPONSE_SUCCESS;
    }

    public GridAction addLinkDeferred(AncillaryPair link, @Nullable Entity modifier) {
        Objects.requireNonNull(link);
        processQueue.add(this, GridAction.TASK_LINK_CREATE, link, modifier);
        return GridAction.RESPONSE_SUCCESS;
    }

    @Override
    public ServerNetlistLookup lookup() {
        return lookup;
    }

    public MNAIndexer indexer() {
        return indexer;
    }

    public GroundNode getOrCreateCommonGround() {
        if(commonGround == null) commonGround = new GroundNode();
        return commonGround;
    }

    public @Nullable GroundNode getCommonGround() {
        return commonGround;
    }

    /**
     * Resets the dynamic (continually-changing)
     * values of this grid's matrix.
     * @see #clearAll
     */
    public void clearDynamic() {
        matrixB.zero();
    }

    /**
     * Resets all terms, dynamic or otherwise,
     * contained by this matrix.
     * @see #clearDynamic
     */
    public void clearAll() {
        if(matrixA != null) matrixA.zero();
        if(matrixB != null) matrixB.zero();
    }

    /**
     * <code>Ax=b</code><p>
     * The <code>A</code> term in MNA represents the current matrix structure
     * made of all known stamped voltages.
     * @return <code>A (Matrix [n * n])</code>
     */
    public DMatrixSparseCSC getMatrix() {
        return matrixA;
    }

    /**
     * <code>Ax=b</code><p>
     * <code>x</code> term in MNA represents the solved voltage matrix for the
     * current frame. The {@link NodalSolver} solves for this
     * value to acquire voltages at every root node.
     * @return <code>x (Vector[n])</code>
     * @see #getSolverStatus
     */
    public DMatrixRMaj getSolution() {
        return matrixX;
    }

    /**
     * <code>Ax=b</code><p>
     * The <code>b</code> term in MNA represents the external forces
     * being applied to the matrix. In practical terms,
     * this method returns a vector which maps externally-induced 
     * voltage (e.g. battery or alternator) to node 
     * index.
     * @return <code>b (Vector[n])</code>
     */
    public DMatrixRMaj getTerms() {
        return matrixB;
    }

    /**
     * <code>Ax=b</code><p>
     * Calls to this methid manipulate matrix <code>A</code> by
     * accumulating voltage <code>v</code> at <code>[row, col]</code>.
     * @param row X axis value
     * @param col Y axis value
     * @param value voltage to stamp
     */
    public void stampA(int row, int col, double v) {
        if(row < 0 || col < 0) return;
        matrixA.unsafe_set(row, col, matrixA.get(row, col) + v);
    }

    /**
     * <code>Ax=b</code><p>
     * Sets the value at <code>index</code> of the conductance
     * vector <code>b</code> to the provided value.
     * @param index
     * @param value
     */
    public void stampB(int index, double value) {
        if(index < 0) return;
        matrixB.set(index, value);
    }

    /**
     * Used by {@link NodalSolver solvers} to quickly create 
     * and return a one-dimensional array 
     * @return A {@link DMAtrixRMaj vector} whose length is the number of rows in the current matrix.
     */
    public DMatrixRMaj createWorkingVector() {
        return new DMatrixRMaj(matrixA.getNumRows(), 1);
    }

    /**
     * Indicates whether or not the matrix is solved. This method is
     * useful to ensure that logic running on Minecraft's render thread
     * doesn't ingest matrix values that are outdated.
     * @return {@link ConvergenceStatus}
     */
    public ConvergenceStatusHolder getStatusHolder() {
        return status;
    }

    public NodeUnionSet netlist() {
        return netlist;
    }

    public NodalSolver getSolver() {
        return solver;
    }

    public void enqueueManifest(Entity requester) {
        Objects.requireNonNull(requester);
        activeManifest = new EnqueuedGridManifest(this, requester);
    }

    private void tickManifest() {
        if(activeManifest != null) { 
            if(activeManifest.isConsumed())
                activeManifest = null;
            else activeManifest.tick();
        };
    }

    public MinecraftServer getServer() {
        MinecraftServer server = ((ServerLevel)getWorld()).getServer();
        if(server == null)
            server = Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer(), "Cannot send clientbound payloads on the client");
        return server;
    }

    public TopologyProcessQueue getProcessQueue() {
        return processQueue;
    }


    public static class TopologyProcessQueue {

        private @Nullable PriorityQueue<TaskWrapper> queue;
        private boolean hasUnsavedChanges = false;
        private boolean justLoaded = false;

        public void applyTo(ServerGrid grid) {
            if(!hasChanges()) return;
            final Set<Node> removedNodes = new HashSet<>(queue.size() * 3);
            final Set<NodePair> disjoints = new HashSet<>(queue.size() * 3);
            boolean brokeEarly = false;
            TaskWrapper wrapper = null;
            while(!queue.isEmpty()) {
                wrapper = queue.poll();
                if(wrapper.creates()) {
                    brokeEarly = true;
                    break;
                }
                wrapper.run(grid, removedNodes, disjoints);
            }
            handleRemovals(grid, removedNodes, disjoints);
            if(brokeEarly) wrapper.run(grid, removedNodes, disjoints);
            while(!queue.isEmpty()) {
                wrapper = queue.poll();
                wrapper.run(grid, removedNodes, disjoints);
            }
            clear();
        }

        private void handleRemovals(ServerGrid grid, Set<Node> removedNodes, Set<NodePair> disjoints) {
            grid.netlist.massRemove(removedNodes, disjoints);
            if(disjoints.isEmpty()) return;
            ActionRunner runner = grid.initiateTask(GridAction.TASK_LINK_DESTROY);
            for(NodePair pair : disjoints)
                handleRemoval(grid, runner, pair);
            for(Node node : removedNodes) {
                Griddable<?> source = GridTracking.getSource(node);
                if(source == null) continue;
                CircuitComponent component = source.getComponent();
                if(component == null) continue;
                component.MNADeallocate(grid);
                if(component instanceof Disposable dp)
                    dp.dispose();
            }
        }

        private void handleRemoval(ServerGrid grid, ActionRunner runner, NodePair pair) {
            List<AncillaryNode<?>> aAnc = pair.a().getAncillaries();
            List<AncillaryNode<?>> bAnc = pair.b().getAncillaries();
            if(aAnc == null || aAnc.isEmpty()) return;
            if(bAnc == null || bAnc.isEmpty()) return;
            for(AncillaryNode<?> an : aAnc) {
                Griddable<?> aSource = GridTracking.getSource(an);
                GridUUID<?> aID = GridTracking.getAddress(aSource, an);
                for(AncillaryNode<?> bn : bAnc) {
                    Griddable<?> bSource = GridTracking.getSource(bn);
                    GridUUID<?> bID = GridTracking.getAddress(bSource, bn);
                    runner.targeting(aSource, bSource)
                        .withArguments(aID, bID)
                        .executeOnClients();
                    AncillaryPair removed = grid.lookup.pop(grid, an, bn);
                    if(removed == null) continue;
                    removed.MNADeallocate(grid);
                    if(removed instanceof Disposable dp)
                        dp.dispose();
                }
            }
        }

        public void add(@Nullable Grid grid, GridAction task, Object... args) {
            for(Object obj : args) {
                if(Disposable.hasBeenDisposed(obj))
                    if(grid != null) {
                        grid.warn("Skipped scheduling of topology restructuring task '" + task.getSerializedName() 
                            + "' - An argument (" + obj.getClass().getSimpleName() + ") was disposed already!");
                    }
            }
            if(this.queue == null)
                this.queue = new PriorityQueue<>(16);
            TaskWrapper toAdd = instantiateTask(grid, task, args);
            if(toAdd == null) return;
            this.queue.add(new TaskWrapper(task, queue.size(), args));
            this.hasUnsavedChanges = true;
        }

        public @Nullable TaskWrapper instantiateTask(@Nullable Grid grid, GridAction task, Object... args) {
            Objects.requireNonNull(grid);
            Objects.requireNonNull(task);
            if(!task.isTask()) {
                if(grid != null) grid.error("Attempted to queue action '" + task + "' but this action is not a task type.");
                return null;
            }
            if(!task.isWrappable()) {
                if(grid != null) grid.error("Attempted to queue action '" + task + "' but this task cannot be wrapped into the queue.");
                return null;
            }
            return new TaskWrapper(task, queue.size(), args);
        }

        /**
         * Used for testing
         */
        public void addRandom(ServerGrid grid, RandomSource random) {
            Objects.requireNonNull(grid);
            Objects.requireNonNull(random);
            int idx = EsoMath.randomInt(random, 0, 3);
            GridAction task = GridAction.values()[idx];
            add(grid, task, new Object[0]);
        }

        public boolean hasChanges() {
            return queue != null && !queue.isEmpty();
        }

        public boolean hasUnsavedChanges() {
            return hasUnsavedChanges;
        }

        public long size() {
            return !hasChanges() ? 0 : queue.size();
        }

        private void clear() {
            queue = null;
        }

        @Override
        public String toString() {
            String out =  "TopologyProcessQueue:\n";
            out += "  Unsaved changes: " + (hasUnsavedChanges ? "yes" : "no") + "\n";
            out += "  Topology:\n";
            if(!hasChanges()) return out + "    [Empty]";
            PriorityQueue<TaskWrapper> copy = new PriorityQueue<>(queue.size());
            while(!queue.isEmpty()) {
                TaskWrapper head = queue.poll();
                out += "    " + head + "\n";
                copy.add(head);
            }
            this.queue = copy;
            return out;
        }
    }

    private static class TaskWrapper implements Comparable<TaskWrapper> {

        private final GridAction action;
        private final Object[] args;
        private final int index;

        private TaskWrapper(GridAction task, int index, Object[] args) {
            Objects.requireNonNull(task);
            this.action = task;
            this.index = index;
            if(args == null) args = new Object[0];
            this.args = args;
        }

        private GridAction run(ServerGrid grid, Set<Node> removedNodes, Set<NodePair> disjoints) {
            if(GridAction.VERBOSE_LOGS) grid.debug("Initiating wrapped task (" + this.action.getTask().getClass().getSimpleName() + ")");
            GridAction output = this.action.getTask().executeTopological(grid, removedNodes, disjoints, args);
            return output == null ? GridAction.NONE : output;
        }

        public boolean creates() {
            return action == GridAction.TASK_LINK_CREATE;
        }

        private int getPriority() {
            return action.ordinal();
        }

		@Override
		public int compareTo(TaskWrapper that) {
            int priorityCompare = Integer.compare(this.getPriority(), that.getPriority());
            if(priorityCompare != 0) return priorityCompare;
            return Integer.compare(this.index, that.index);
		}

        @Override
        public String toString() {
            return action.getTask().getClass().getSimpleName() + " (index " + index + "), " + action.getTask().collectArgsAsString(args);
        }
    }
}
