package com.quattage.mechano.api.grid.topology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.component.StampingComponent;
import com.quattage.mechano.api.grid.component.StampingComponent.NeedsPostProcessing;
import com.quattage.mechano.api.grid.solver.NodalSolver;
import com.quattage.mechano.api.grid.solver.NodalSolver.ConvergenceStatus;
import com.quattage.mechano.api.grid.topology.landmark.Node;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;

public class GridDomain {

    private @Nullable Node referenceNode;
    private MNAIndexer indexer = new MNAIndexer();
    private NodeUnionSet netlist = new NodeUnionSet();

    private @Nullable DMatrixSparseCSC matrixA;
    private @Nullable DMatrixRMaj matrixX, matrixB;
    private boolean isDirty = false;

    public GridDomain() {}

    public void mergeWith(GridDomain other, int idx) {
        Objects.requireNonNull(other);
        if(this == other) return;
        this.netlist = NodeUnionSet.concatenate(this.netlist, other.netlist);
        this.indexer = MNAIndexer.concatenate(this.indexer, other.indexer);
        this.referenceNode = null;
        other.clear();
        markDirty();
    }

    /**
     * Creates a list of fresh GridDomain instances that contain the contents
     * of this one, but split 
     * @param world
     * @return
     */
    public List<GridDomain> deriveFromSplits(LevelReader world) {
        if(netlist.isEmpty()) return Collections.emptyList();
        netlist.trim();
        List<NodalCluster> clusters = NodalCluster.ofDiscontinuities(world, netlist);
        if(clusters.isEmpty()) return Collections.emptyList();
        List<GridDomain> outputDomains = new ArrayList<>(clusters.size());
        for(NodalCluster cluster : clusters) {
            GridDomain newDomain = cluster.spawnFrom(this);
            outputDomains.add(newDomain);
        }
        return outputDomains;
    }

    /**
     * Updates this domain's matrix dimensions and 
     * finalizes the netlist's indices for each node.
     * This method only needs to be called when the 
     * shape of this domain changes.
     * @param grid {@link ServerGrid} that this domain belongs to
     * @param idx
     */
    public void preSolve(ServerGrid grid, int idx) {
        netlist.finalizeTopology(this, idx);
        int size = indexer.sourceCount() + netlist.size();
        if(referenceNode != null) size--;
        matrixA = new DMatrixSparseCSC(size, size);
        matrixX = new DMatrixRMaj(size, 1);
        matrixB = new DMatrixRMaj(size, 1);
        indexer.stamp(grid, this);
        isDirty = false;
    }

    /**
     * Solves this domain for voltages at every node.
     * Updates the matrices within this grid to
     * reflect the current status.
     * @param grid {@link ServerGrid} that this domain belongs to
     * @param solver {@link NodalSolver solver method} to use
     */
    public void solve(ServerGrid grid, NodalSolver solver) {
        ConvergenceStatus newStatus = solver.run(grid, this);
        if(newStatus == null || newStatus == ConvergenceStatus.UNLOADED) {
            grid.warn("Failed to retrieve status for tick, solver run returned no status!");
            newStatus = ConvergenceStatus.NONE;
        }
    }

    /**
     * Handle components within this domain that require
     * {@link NeedsPostProcessing post processing}. This 
     * method is intended to be run after a call to 
     * {@link #solve}.
     * @param grid {@link ServerGrid} that this domain belongs to
     */
    public void postSolve(ServerGrid grid) {
        for(StampingComponent sc : indexer.getStampers())
            if(sc instanceof NeedsPostProcessing pp) pp.postProcess(grid, this);
    }

    public NodeUnionSet netlist() {
        return netlist;
    }

    public MNAIndexer indexer() {
        return indexer;
    }

    /**
     * Used by {@link NodalSolver solvers} to quickly create 
     * and return a one-dimensional array pre-configured to the correct size.
     * @return A {@link DMAtrixRMaj vector} whose length is the number of rows in the current matrix.
     */
    public DMatrixRMaj createWorkingVector() {
        if(matrixA == null) throw new IllegalStateException("Working vector cannot be created for an uninitialized or idling GridDomain (This domain's underlying matrix is null!)");
        return new DMatrixRMaj(matrixA.getNumRows(), 1);
    }

    /**
     * <code>Ax = b, n = indexer length + netlist length</code><p>
     * @return <code>A (Matrix[n][n])</code>
     */
    public DMatrixSparseCSC matrix() {
        return matrixA;
    }

    /**
     * <code>Ax = b, n = indexer length + netlist length</code><p>
     * @return <code>x (Vector[n][1])</code>
     */
    public DMatrixRMaj solution() {
        return matrixX;
    }

    /**
     * <code>Ax = b, n = indexer length + netlist length</code><p>
     * @return <code>b (Vector[n][1])</code>
     */
    public DMatrixRMaj terms() {
        return matrixB;
    }

    public @Nullable Node reference() {
        return referenceNode;
    }

    public void markDirty() {
        markDirty(true);
    }

    public void markDirty(boolean isDirty) {
        this.isDirty = isDirty;
    }

    public boolean isTopologyOutdated() {
        return this.isDirty;
    }

    /**
     * <code>Ax=b</code><p>
     * Calls to this method manipulate matrix <code>A</code> by
     * accumulating <code>value</code> at <code>[row, col]</code>.
     * @param row X axis value
     * @param col Y axis value
     * @param value value to stamp
     */
    public void stampA(int row, int col, double value) {
        if(row < 0 || col < 0) return;
        matrixA.unsafe_set(row, col, matrixA.get(row, col) + value);
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
        matrixB.set(index, 0, value);
    }

    public String matrixAsString() {
        String out = "";
        if(matrixA == null) return "null";
        for(int x = 0; x < matrixA.numRows; x++) {
            for(int y = 0; y < matrixA.numCols; y++) {
                out += "" + matrixA.get(x, y) + " ";
            }
            out += "\n";
        }
        return out.substring(0, out.length() - 1);
    }

    public String termsAsString() {
        String out = "";
        if(matrixB == null) return "null";
        for(int x = 0; x < matrixB.numRows; x++) {
            out += matrixB.get(x, 0) + " ";
        }
        return out.substring(0, out.length() - 1);
    }

    public String solutionAsString() {
        String out = "";
        if(matrixX == null) return "null";
        for(int x = 0; x < matrixX.numRows; x++) {
            out += matrixX.get(x, 0) + " ";
        }
        return out.substring(0, out.length() - 1);
    }

    public String stateAsString() {
        String out = "domain state:\n";
        out += "A:\n"  + matrixAsString() + "\n\n";
        out += "B:\n" + termsAsString() + "\n\n";
        out += "X:\n" + solutionAsString() + "\n\n";
        out += "idxr:\n" + indexer.toString() + "\n\n";
        return out;
    }

    public void clear() {
        idle();
        referenceNode = null;
        indexer.clear();
        netlist.reset();
    }

    public void idle() {
        matrixA = null; matrixX = null; matrixB = null;
    }

    public CompoundTag write(ServerGrid instantiator, Provider provider) {
        // ListTag output = new ListTag(netlist.size());
        // contents.put("netlist", writeLinks());
        return null;
    }


    public void read(ServerGrid instantiator, CompoundTag contents, Provider provider) {

    }

    // private ListTag writeLinks() {
    //     ListTag output = new ListTag(netlist.size());
    //     if(netlist.isEmpty()) return output;
    //     for(Map.Entry<AncillaryNode<?>, List<AncillaryPair>> branch : lookup.all()) {
    //         AncillaryNode<?> key = branch.getKey();
    //         List<AncillaryPair> contents = branch.getValue();
    //         if(contents.isEmpty()) continue;
    //         CompoundTag label = new CompoundTag();
    //         GridUUID<?> id = GridTracking.getAddress(key.getProviderSource(), key);
    //         ListTag connections = new ListTag(contents.size());
    //         for(AncillaryPair link : contents) {
    //             CompoundTag newTag = new CompoundTag();
    //             AncillaryNode<?> node = link.getEndAncillary();
    //             GridUUID<?> newID = GridTracking.getAddress(node.getProviderSource(), node);
    //             newID.write(newTag);
    //             int trnsType = -1;
    //             if(link instanceof ComponentLink<?> cl)
    //                 trnsType = Mechano.REGISTRATE.getTransmitterRegistry().getId(cl.getTransmitter());
    //             newTag.putInt("trns", trnsType);
    //             connections.add(newTag);
    //         }
    //         id.write(label);
    //         label.put("conns", connections);    
    //         output.add(label);
    //     }
    //     return output;
    // }

    public static String getIndexInfo(GridDomain domain, Node node) {
        int nodalIndex = domain.indexer().get(node);
        int domainIndex = node.getDomainIndex();
        return "[node #" + System.identityHashCode(node) + ", NI: " + nodalIndex + " DI: " + domainIndex + "]";
    }
}
