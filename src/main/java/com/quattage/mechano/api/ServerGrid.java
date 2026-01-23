package com.quattage.mechano.api;

import java.util.HashSet;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.component.StampingComponent;
import com.quattage.mechano.api.grid.component.StampingComponent.NeedsPostProcessing;
import com.quattage.mechano.api.grid.solver.MNAIndexer;
import com.quattage.mechano.api.grid.solver.NodalSolver;
import com.quattage.mechano.api.grid.solver.NodalSolver.ConvergenceStatus;
import com.quattage.mechano.api.grid.solver.NodalSolver.ConvergenceStatusHolder;
import com.quattage.mechano.api.grid.solver.StabilizedBiconjucateSolver;
import com.quattage.mechano.api.grid.topology.AncillaryPair;
import com.quattage.mechano.api.grid.topology.netlist.NodeUnionSet;
import com.quattage.mechano.api.grid.topology.netlist.NodeUnionSet.NodePair;
import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.api.grid.topology.vertex.Node.GroundNode;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.foundation.Disposable;
import com.quattage.mechano.foundation.numeric.EsoMath;
import com.quattage.mechano.infrastructure.EnqueuedGridManifest;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public final class ServerGrid extends Grid {

    private @Nullable EnqueuedGridManifest activeManifest;
    private @Nullable GroundNode commonGround;    
    private final MNAIndexer indexer = new MNAIndexer();
    protected final NodeUnionSet netlist = new NodeUnionSet();
    private NodalSolver solver = new StabilizedBiconjucateSolver();
    protected final ConvergenceStatusHolder status = new ConvergenceStatusHolder();

    private @Nullable DMatrixSparseCSC matrixA;
    private @Nullable DMatrixRMaj matrixX, matrixB;

    private final TopologyProcessQueue processQueue = new TopologyProcessQueue();

    protected ServerGrid(Level world) {
        super(world);
        status.set(ConvergenceStatus.UNLOADED);
    }

    @Override
    public void load() {
        
    }

    @Override
    public void unload() {
        status.set(ConvergenceStatus.REFRESHING_TOPOLOGY);
        clearAll();
        solver.reset();
        netlist.reset();
        indexer.clear();
        processQueue.clear();
        status.set(ConvergenceStatus.UNLOADED);
    }

    @Override
    public void tick() {
        tickManifest();
        if(processQueue.hasChanges())
            preProcess();

        if(shouldSolve()) {
            runSolver();
            postProcess();
        }
    }

    private void preProcess() {
        status.set(ConvergenceStatus.REFRESHING_TOPOLOGY);
        processQueue.applyTo(this);
        if(netlist.isEmpty()) return;
        int size = (netlist.size() - 1) + indexer.size();
        matrixA = new DMatrixSparseCSC(size, size, 10 * netlist.size());
        matrixX = createWorkingVector();
        matrixB = createWorkingVector();
        solver.initialize(this);
        matrixA.sortIndices(null);
        netlist.assignIndices();
    }

    private boolean shouldSolve() {
        if(!indexer.hasStampers()) {
            this.status.set(ConvergenceStatus.IDLE);
            matrixA = null; matrixX = null; matrixB = null;
            indexer.clear();
            return false;
        }
        return true;
    }

    private void runSolver() {
        // this.status.set(ConvergenceStatus.COMPUTING);
        // ConvergenceStatus newStatus = solver.run(this);
        // if(newStatus == null || newStatus == ConvergenceStatus.UNLOADED) {
        //     warn("Failed to retrieve status for tick, solver run returned no status!");
        //     newStatus = ConvergenceStatus.UNLOADED;
        // }
        // this.status.set(newStatus);
    }

    private void postProcess() {
        for(StampingComponent sc : indexer.getStampers()) {
            if(!(sc instanceof NeedsPostProcessing pp)) continue;
            pp.postProcess(this);
        }
    }

    public GridAction removeLinkDeferred(AncillaryPair link) {
        Objects.requireNonNull(link);
        processQueue.add(this, GridAction.TASK_LINK_DESTROY, link);
        return GridAction.RESPONSE_SUCCESS;
    }

    public GridAction removeComponent(CircuitComponent component) {
        Objects.requireNonNull(component);
            processQueue.add(this, GridAction.TASK_COMPONENT_DESTROY, component);
        return GridAction.RESPONSE_SUCCESS;
    }

    public GridAction addComponent(CircuitComponent component) {
        Objects.requireNonNull(component);
        processQueue.add(this, GridAction.TASK_COMPONENT_CREATE, component);
        return GridAction.RESPONSE_SUCCESS;
    }

    public GridAction addLinkDeferred(AncillaryPair link) {
        Objects.requireNonNull(link);
        processQueue.add(this, GridAction.TASK_LINK_CREATE, link);
        return GridAction.RESPONSE_SUCCESS;
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
    public DMatrixRMaj getVoltages() {
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
        return new DMatrixRMaj(matrixA.getNumRows());
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
            grid.netlist.massRemove(removedNodes, disjoints);
            if(brokeEarly) wrapper.run(grid, removedNodes, disjoints);
            while(!queue.isEmpty()) {
                wrapper = queue.poll();
                wrapper.run(grid, removedNodes, disjoints);
            }
            clear();
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
            GridAction output = this.action.getTask().executeTopological(grid, removedNodes, disjoints, args);
            return output == null ? GridAction.NONE : output;
        }

        public boolean creates() {
            return action == GridAction.TASK_LINK_CREATE || action == GridAction.TASK_COMPONENT_CREATE;
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
