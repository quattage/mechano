package com.quattage.mechano.api;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.jetbrains.annotations.ApiStatus;

import com.quattage.mechano.api.grid.solver.NodalSolver;
import com.quattage.mechano.api.grid.solver.NodalSolver.ConvergenceStatus;
import com.quattage.mechano.api.grid.solver.NodeUnionSet;
import com.quattage.mechano.api.grid.solver.StabilizedBiconjucateSolver;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.grid.topology.ComponentLink;
import com.quattage.mechano.api.switchboard.action.GridAction;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public final class ServerGrid extends Grid {

    private static final NodalSolver solver = new StabilizedBiconjucateSolver();
    protected final NodeUnionSet unionizer = new NodeUnionSet();
    protected final AtomicReference<ConvergenceStatus> status = new AtomicReference<>(ConvergenceStatus.UNFINISHED_UNPOPULATED);
    protected final Object2IntOpenHashMap<CircuitComponent> sourceIndices = new Object2IntOpenHashMap<>();

    private DMatrixSparseCSC A;
    private DMatrixRMaj x, b;
    private int sourceCount, size = 0;
    private boolean dirty = false;

    protected ServerGrid(Level world) {
        super(world);
    }

    public void beginAssembly(ServerGrid grid) {
        this.size = (unionizer.rootCount() - 1) + sourceCount;
        this.A = new DMatrixSparseCSC(size, size, 10 * unionizer.rootCount());
        this.x = new DMatrixRMaj(size);
        this.b = new DMatrixRMaj(size);
        this.A.sortIndices(null);
    }

    @Override
    protected void tick() {
        if(dirty) dirtyTick();
    }

    private void dirtyTick() {

    }

    @Override
    public GridAction addLink(ComponentLink<?> link) {
        GridAction output = super.addLink(link);
        if(output.getActionType().indicatesSuccess()) {
            mark(link.getStartNode().getParentComponent());
            mark(link.getEndNode().getParentComponent());
            dirty = true;
        }
        return output;
    }

    @Override
    public GridAction removeLink(ComponentLink<?> link) {
        GridAction output = super.removeLink(link);
        this.unmark(link);
        if(output.getActionType().indicatesSuccess()) {
            unmark(link.getStartNode().getParentComponent());
            unmark(link.getEndNode().getParentComponent());
            dirty = true;
        }
        return output;
    }

    public void mark(CircuitComponent component) {
        if(component == null || !component.isSignificant()) return;
        component.forEachNode(node -> { unionizer.add(node); });
        sourceCount += Math.max(0, component.getContributionFactor());
    }

    public void unmark(CircuitComponent component) {
        if(component == null || !component.isSignificant()) return;
        component.forEachNode(node -> { unionizer.remove(node); });
        sourceCount -= Math.max(0, component.getContributionFactor());
    }

    @Override
    protected void onLoad() {
        
    }

    @Override
    protected void onUnload() {
        ServerGrid.solver.reset();
        clearAll();
        unionizer.reset();
        status.set(ConvergenceStatus.UNFINISHED_UNPOPULATED);
        sourceIndices.clear();
    }

    /**
     * Resets the dynamic (continually-changing)
     * values of this grid's matrix.
     * @see #clearAll
     */
    public void clearDynamic() {
        b.zero();
    }

    /**
     * Resets all terms, dynamic or otherwise,
     * contained by this matrix.
     * @see #clearDynamic
     */
    public void clearAll() {
        A.zero();
        b.zero();
    }

    /**
     * <code>Ax=b</code><p>
     * The <code>A</code> term in MNA represents the current matrix structure
     * made of all known stamped voltages.
     * @return <code>A (Matrix [n * n])</code>
     */
    public DMatrixSparseCSC matrixTermA() {
        return A;
    }

    /**
     * <code>Ax=b</code><p>
     * <code>x</code> term in MNA represents the solved voltage matrix for the
     * current frame. The {@link NodalSolver} solves for this
     * value to acquire voltages at every root node.
     * @return <code>x (Vector[n])</code>
     * @see #getSolverStatus
     */
    public DMatrixRMaj matrixTermX() {
        return x;
    }

    /**
     * <code>Ax=b</code><p>
     * The <code>b</code> term in MNA represents the external forces
     * being applied to the matrix. In practical terms,
     * this method returns a vector which maps externally-induced 
     * current (e.g. battery or alternator) to node 
     * index.
     * @return <code>b (Vector[n])</code>
     */
    public DMatrixRMaj matrixTermB() {
        return b;
    }

    /**
     * <code>Ax=b</code><p>
     * Calls to this methid manipulate matrix <code>A</code> by
     * accumulating voltage <code>v</code> at <code>[row, col]</code>.
     * @param row X axis value
     * @param col Y axis value
     * @param value voltage to stamp
     */
    public void stampMatrix(int row, int col, double v) {
        if(row < 0 || col < 0) return;
        A.unsafe_set(row, col, A.get(row, col) + v);
    }

    /**
     * <code>Ax=b</code><p>
     * Sets the value at <code>index</code> of the conductance
     * vector <code>b</code> to the provided value.
     * @param index
     * @param value
     */
    public void stampRHS(int index, double value) {
        if(index < 0) return;
        b.set(index, value);
    }

    /**
     * Used by {@link NodalSolver solvers} to quickly create 
     * and return a one-dimensional array 
     * @return A {@link DMAtrixRMaj vector} whose length is the number of rows in the current matrix.
     */
    public DMatrixRMaj createWorkingVector() {
        return new DMatrixRMaj(A.getNumRows());
    }

    /**
     * Indicates whether or not the matrix is solved. This method is
     * useful to ensure that logic running on Minecraft's render thread
     * doesn't ingest matrix values that are outdated.
     * @return {@link ConvergenceStatus}
     */
    public ConvergenceStatus getSolverStatus() {
        return status.get();
    }

    /**
     * To be called only by the currently active nodal solver.
     */
    @ApiStatus.Internal
    public ServerGrid setStatus(ConvergenceStatus status) {
        this.status.set(status);
        return this;
    }

    public MinecraftServer getServer() {
        MinecraftServer server = ((ServerLevel)getWorld()).getServer();
        if(server == null)
            server = Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer(), "Cannot send clientbound payloads on the client");
        return server;
    }
}
