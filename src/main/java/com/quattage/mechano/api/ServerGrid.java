package com.quattage.mechano.api;

import java.text.SimpleDateFormat;
import java.util.Objects;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.MechanoBuildParameters;
import com.quattage.mechano.api.grid.functional.PerfectConductor;
import com.quattage.mechano.api.grid.functional.PerfectInsulator;
import com.quattage.mechano.api.grid.solver.MNAIndexer;
import com.quattage.mechano.api.grid.solver.NodalSolver;
import com.quattage.mechano.api.grid.solver.NodalSolver.ConvergenceStatus;
import com.quattage.mechano.api.grid.solver.NodalSolver.ConvergenceStatusHolder;
import com.quattage.mechano.api.grid.solver.NodeUnionSet;
import com.quattage.mechano.api.grid.solver.StabilizedBiconjucateSolver;
import com.quattage.mechano.api.grid.topology.Circuit;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.grid.topology.CircuitComponent.StampingComponent;
import com.quattage.mechano.api.grid.topology.ComponentLink;
import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.api.grid.topology.vertex.Node.GroundedJoint;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.infrastructure.MemoryAnalyzer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public final class ServerGrid extends Grid {

    private GroundedJoint commonGround;    
    protected final NodeUnionSet unionizer = new NodeUnionSet();
    private final MNAIndexer indexer = new MNAIndexer();
    private NodalSolver solver = new StabilizedBiconjucateSolver();
    protected final ConvergenceStatusHolder status = new ConvergenceStatusHolder();

    private DMatrixSparseCSC A;
    private DMatrixRMaj x, b;
    private boolean isMatrixDirty = false;
    private boolean hasUnsavedChanges = false;

    protected ServerGrid(Level world) {
        super(world);
    }

    @Override
    protected void onLoad() {
        
    }

    @Override
    protected void onUnload() {
        solver.reset();
        clearAll();
        unionizer.reset();
        status.set(ConvergenceStatus.IDLE);
        indexer.clear();
    }

    @Override
    public void tick() {
        if(!!indexer.hasStampers()) {
            this.status.set(ConvergenceStatus.IDLE);
            A = null; x = null; b = null;
            indexer.clear();
            return;
        }
        if(isMatrixDirty) dirtyTick();
        if(!indexer.hasStampers()) return;
        this.status.set(ConvergenceStatus.COMPUTING);
        ConvergenceStatus newStatus = solver.run(this);
        if(newStatus == null || newStatus == ConvergenceStatus.NONE) {
            warn("Failed to retrieve status for tick, solver run returned no status!");
            newStatus = ConvergenceStatus.NONE;
        }
        this.status.set(newStatus);
    }

    private void dirtyTick() {
        int size = (unionizer.rootCount() - 1) + indexer.size();
        status.set(ConvergenceStatus.REFRESHING_TOPOLOGY);
        A = new DMatrixSparseCSC(size, size, 10 * unionizer.rootCount());
        x = createWorkingVector();
        b = createWorkingVector();
        solver.initialize(this);
        A.sortIndices(null);
        unionizer.assignIndices();
        isMatrixDirty = false;
    }

    @Override
    public GridAction addLink(ComponentLink<?> link) {
        GridAction output = super.addLink(link);
        CircuitComponent component = link.getOrCreateInternalComponent();
        if(component instanceof PerfectInsulator) {
            link.forgetInternalComponent();
            return GridAction.RESPONSE_SUCCESS;
        }
        if(output.getActionType().indicatesSuccess()) {
            mark(link.getStartNode().getParentComponent());
            mark(link.getEndNode().getParentComponent());
            isMatrixDirty = true;
            hasUnsavedChanges = true;
        }
        if(component instanceof PerfectConductor) {
            link.forgetInternalComponent();
            unionizer.union(link.getStartNode(), link.getEndNode());
        }
        return output;
    }

    @Override
    public GridAction removeLink(ComponentLink<?> link) {
        GridAction output = super.removeLink(link);
        if(output.getActionType().indicatesSuccess()) {
            unmark(link.getStartNode().getParentComponent());
            unmark(link.getEndNode().getParentComponent());
            isMatrixDirty = true;
            hasUnsavedChanges = true;
        }
        return output;
    }

    private void mark(CircuitComponent component) {
        if(component == null || !component.isSignificant()) return;
        component.forEachNode(node -> { unionizer.add(node); });
        if(component instanceof StampingComponent stamper) 
            indexer.allocate(stamper);
        else if(component instanceof Circuit circuit) {
            circuit.forEachComponent(comp -> {
                if(comp instanceof StampingComponent stamper)
                    indexer.allocate(stamper);
            });
        }
    }

    private void unmark(CircuitComponent component) {
        if(component == null) return;
        component.forEachNode(node -> { 
            unionizer.remove(node); 
        });
        if(component instanceof StampingComponent stamper) 
            indexer.forget(stamper);
        else if(component instanceof Circuit circuit) {
            circuit.forEachComponent(comp -> {
                if(comp instanceof StampingComponent stamper)
                    indexer.forget(stamper);;
            });
        }
    }

    public MNAIndexer indexer() {
        return indexer;
    }

    public GroundedJoint getCommonGround() {
        if(commonGround == null) commonGround = new GroundedJoint();
        return commonGround;
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
    public void stampA(int row, int col, double v) {
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
    public void stampB(int index, double value) {
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
    public ConvergenceStatus getConvergenceStatus() {
        return status.get();
    }

    /**
     * Returns a formatted string containing a comprehensive summary
     * of this grid's current state and internal data.
     */
    public String writeManifest(@Nullable Entity requester) {
        StopWatch timer = StopWatch.createStarted();
        String out =  "\n▙▚▘▘\t\t\t\tMechano GridAPI manifest\t\t\t\t▝▝▞▟\n\n";
            out += ""
                + "API " + MechanoBuildParameters.asString() + "\n"
                + "Requested at [" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(System.currentTimeMillis()) + "]"
                + " by '" + (requester == null ? "n/a" : requester.getName().getString()) + "' in " + getDimensionName() + "\n"
                + "Solver method: " + solver.describeSelf() + "\n"
                + "Lifecycle status: " + status + "\n"
                + "Unsaved changes for this session? " + (hasUnsavedChanges ? "yes" : "no") + "\n"
                + "Ground: " + (commonGround == null ? "n/a" : commonGround.hashCode()) + "\n"
                + "Memory footprint analysis: " + MemoryAnalyzer.estimateFootprint(this) + "\n--\n"
                + "Grid Contents:\n" + collectNodes() + "\n"
                + "  ♨ github.com/quattage/mechano\n"
                + "  ☎ discord.gg/85ufgRwy2g\n";
        return out += "\n\n▛▞▖▖\t\t\t      Manifest generated in " + DurationFormatUtils.formatDuration(timer.getTime(), "ss.SSS") + "s      \t\t\t▗▗▚▜ \n";
    }

    private String collectNodes() {
        String out = "";
        if(unionizer.allRoots().isEmpty()) return "Empty";
        for(Node node : unionizer.allRoots()) {
            out += node.toFullString(unionizer) + "\n";
        }
        return out;
    }

    public MinecraftServer getServer() {
        MinecraftServer server = ((ServerLevel)getWorld()).getServer();
        if(server == null)
            server = Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer(), "Cannot send clientbound payloads on the client");
        return server;
    }
}
