package com.quattage.mechano.api;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.component.GridConstruct;
import com.quattage.mechano.api.grid.component.StampingComponent;
import com.quattage.mechano.api.grid.component.StampingComponent.NeedsPostProcessing;
import com.quattage.mechano.api.grid.solver.MNAIndexer;
import com.quattage.mechano.api.grid.solver.NodalSolver;
import com.quattage.mechano.api.grid.solver.NodalSolver.ConvergenceStatus;
import com.quattage.mechano.api.grid.solver.NodalSolver.ConvergenceStatusHolder;
import com.quattage.mechano.api.grid.solver.StabilizedBiconjucateSolver;
import com.quattage.mechano.api.grid.topology.AncillaryPair;
import com.quattage.mechano.api.grid.topology.ComponentLink;
import com.quattage.mechano.api.grid.topology.netlist.NodeUnionSet;
import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.api.grid.topology.vertex.Node.GroundNode;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.foundation.Disposable;
import com.quattage.mechano.infrastructure.EnqueuedGridManifest;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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
    private boolean isMatrixDirty = false;
    private boolean hasUnsavedChanges = false;

    private @Nullable Collection<CircuitComponent> reducedComponents;

    protected ServerGrid(Level world) {
        super(world);
        status.set(ConvergenceStatus.UNLOADED);
    }

    @Override
    protected void load() {
        
    }

    @Override
    protected void unload() {
        clearAll();
        solver.reset();
        netlist.reset();
        indexer.clear();
        status.set(ConvergenceStatus.UNLOADED);
    }

    @Override
    public void tick() {
        tickManifest();
        if(isMatrixDirty) preProcess();
        if(shouldSolve()) {
            runSolver();
            postProcess();
        }
    }

    private void preProcess() {
        status.set(ConvergenceStatus.REFRESHING_TOPOLOGY);
        final ObjectOpenHashSet<Node> emptyNodes = new ObjectOpenHashSet<>();
        if(reducedComponents != null && !reducedComponents.isEmpty()) {
            emptyNodes.ensureCapacity(reducedComponents.size() * 3);
            for(CircuitComponent reduced : reducedComponents) {
                StampingComponent.asStamperDo(this, reduced, stamper -> indexer.forget(stamper));
                reduced.forEachNode(node -> {
                    emptyNodes.add(node);
                });
                Disposable.dispose(reduced);
            }
        }
        netlist.removeAll(emptyNodes);
        int size = (netlist.size() - 1) + indexer.size();
        matrixA = new DMatrixSparseCSC(size, size, 10 * netlist.size());
        matrixX = createWorkingVector();
        matrixB = createWorkingVector();
        solver.initialize(this);
        matrixA.sortIndices(null);
        netlist.assignIndices();
        reducedComponents = null;
        isMatrixDirty = false;
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
        for(StampingComponent sc : indexer.getAllStampers()) {
            if(!(sc instanceof NeedsPostProcessing pp)) continue;
            pp.postProcess(this);
        }
    }

    @Override
    public GridAction addLink(AncillaryPair link) {
        GridAction result = super.addLink(link);
        if(!result.getActionType().indicatesSuccess()) return result;
        if(link instanceof ComponentLink cl) {
            CircuitComponent component = cl.apply(this);
            if(component != null) {
                if(component instanceof GridConstruct gc)
                    gc.updateOwnership(cl, -1);
                mark(component);
            }
        }
        mark(link.getStartNode().getParentConstruct());
        mark(link.getEndNode().getParentConstruct());
        isMatrixDirty = true;
        hasUnsavedChanges = true;
        return result;
    }

    @Override
    public GridAction removeLink(AncillaryPair link) {
        int preSize = getLinkCount();
        GridAction output = super.removeLink(link);
        if(!output.getActionType().indicatesSuccess()) return output;
        if(link instanceof ComponentLink cl) {
            CircuitComponent component = cl.get();
            if(component != null)
                unmark(component);
            cl.invalidate();
        }
        if(getLinkCount() != preSize) {
            unmark((CircuitComponent)link.getStartNode().getParentConstruct());
            unmark((CircuitComponent)link.getEndNode().getParentConstruct());
        }
        isMatrixDirty = true;
        hasUnsavedChanges = true;
        return output;
    }

    private void mark(CircuitComponent component) {
        Objects.requireNonNull(component);
        component.forEachNode(node -> netlist.add(node));
        StampingComponent.asStamperDo(this, component, stamper -> indexer.allocate(stamper));
    }

    private void unmark(CircuitComponent component) {
        Objects.requireNonNull(component);
        if(reducedComponents == null) reducedComponents = new HashSet<>();
        reducedComponents.add(component);
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
        matrixA.zero();
        matrixB.zero();
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

    public NodeUnionSet getNetlist() {
        return netlist;
    }

    public NodalSolver getSolver() {
        return solver;
    }

    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
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
}
