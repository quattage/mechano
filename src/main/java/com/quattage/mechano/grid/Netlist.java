package com.quattage.mechano.grid;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.Disposable;
import com.quattage.mechano.grid.solver.ConvergenceStatus;
import com.quattage.mechano.grid.solver.SolverAlgorithm;
import com.quattage.mechano.grid.topology.core.Node;
import com.quattage.mechano.grid.topology.core.NodePair;
import com.quattage.mechano.grid.topology.core.StampingComponent;
import com.quattage.mechano.grid.topology.core.StampingComponent.StampsDynamically;
import com.quattage.mechano.switchboard.RemovalLedger.RemovalEntry;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class Netlist implements Disposable {

    protected @Nullable Object2ObjectOpenHashMap<Node, ObjectOpenHashSet<Node>> adjacencyTree;    // adjacency matrix (undirected, non-cyclic)
    protected @Nullable Object2ObjectOpenHashMap<Node, ObjectOpenHashSet<Node>> transitiveTree;   // union-find transitive access (semi-cyclic)
    protected @Nullable Object2ObjectOpenHashMap<Node, Node> relations;                           // union-find discoverability/path compression (cyclic)
    
    protected Set<StampingComponent> stampers = new ObjectOpenHashSet<>();
    protected Object2IntOpenHashMap<StampsDynamically> sourceCoodinates = new Object2IntOpenHashMap<>();
    protected Object2IntOpenHashMap<Node> nodeCoordinates = new Object2IntOpenHashMap<>();
    protected int indexerCursor = 0;

    private @Nullable Node referenceNode;
    private @Nullable DMatrixSparseCSC matrixA;
    private @Nullable DMatrixRMaj matrixX, matrixB;
    private long lastStatusUpdateTime = -1L;
    protected ConvergenceStatus status = ConvergenceStatus.UNLOADED;

    protected @Nullable Set<Node> orphans;

    protected Netlist(int size) {
        relations = new Object2ObjectOpenHashMap<>(size);
        transitiveTree = new Object2ObjectOpenHashMap<>(size);
        adjacencyTree = new Object2ObjectOpenHashMap<>(size);
    }

    public Node findTransitiveRoot(Node node) {
        assertNotDisposed();
        return findTransitiveRoot(node, 0);
    }

    private Node findTransitiveRoot(Node node, int iter) {
        if(iter > 2048) {
            ServerGrid.ACTION_LOG.line("    !!! overloaded path compression at " + iter + " iterations !!!");
            return node;
        }
        Node p = relations.get(node);
        if(node == p) return node;
        Node root = findTransitiveRoot(p, iter + 1);
        if(relations.get(node) != root) {
            Set<Node> branch = transitiveTree.get(p);
            if(branch != null) branch.remove(node);
            relations.put(node, root);
            Set<Node> rr = transitiveTree.get(root);
            if(rr != null) rr.add(node);
        }
        return root;
    }

    public void union(Node a, Node b) {
        assertNotDisposed();
        addNode(a); addNode(b);
        Node aP = findTransitiveRoot(a);
        Node bP = findTransitiveRoot(b);
        if(aP != bP) {
            Node primary = Node.choosePrimary(aP, bP);
            Node secondary = primary == aP ? bP : aP;
            relations.put(secondary, primary);
            ObjectOpenHashSet<Node> branchRoot = transitiveTree.get(primary);
            ObjectOpenHashSet<Node> branchChild = transitiveTree.remove(secondary);
            if(branchRoot == null) {
                branchRoot = new ObjectOpenHashSet<>();
                transitiveTree.put(primary, branchRoot);
            }
            branchRoot.add(secondary);
            if(branchChild != null && !branchChild.isEmpty())
                branchRoot.addAll(branchChild);
        }
        joinAdjacents(a, b);
        joinAdjacents(b, a);
    }

    private boolean addNode(Node node) {
        Objects.requireNonNull(node);
        if(relations.containsKey(node)) return false;
        relations.put(node, node);
        transitiveTree.put(node, null);
        adjacencyTree.put(node, null);
        return true;
    }

    private void joinAdjacents(Node primary, Node secondary) {
        ObjectOpenHashSet<Node> branch = adjacencyTree.get(primary);
        if(branch == null) {
            branch = new ObjectOpenHashSet<>();
            adjacencyTree.put(primary, branch);
        }
        branch.add(secondary);
    }

    protected void forgetUnion(ServerGrid grid, RemovalEntry removal, NodePair union) {
        Node directA = relations.get(union.getNodeB());
        Node directB = relations.get(union.getNodeA());
        if(union.getNodeA().equals(directA)) {
            Node removed = relations.remove(union.getNodeA());
            if(removed != null) ServerGrid.ACTION_LOG.line("  - removed relation to " + grid.describe(removed));
        }
        if(union.getNodeB().equals(directB)) {
            Node removed = relations.remove(union.getNodeB());
            if(removed != null) ServerGrid.ACTION_LOG.line("  - removed relation to " + grid.describe(removed));
        }
        deLink(grid, removal, union);
        deLink(grid, removal, union.inverse());
    }

    private void deLink(ServerGrid grid, RemovalEntry removal, NodePair union) {
        Set<Node> transitive = transitiveTree.get(union.getNodeA());
        if(transitive != null) transitive.remove(union.getNodeB());
        Set<Node> connected = adjacencyTree.get(union.getNodeA());
        if(connected == null || connected.isEmpty()) {
            ServerGrid.ACTION_LOG.line("  - skipped de-linking " + grid.describe(union) + " since this node's primary doesn't have an adjacency presence");
            if(!removal.contains(union.getNodeA()))
                markOrphan(grid, union.getNodeA());
            return;
        }
        if(connected.remove(union.getNodeB())) {
            ServerGrid.ACTION_LOG.line("  - de-linked union " + grid.describe(union) + " from UF adjacency");
            if(connected.isEmpty()) {
                adjacencyTree.remove(union.getNodeA());
                if(!removal.contains(union.getNodeA()))
                    markOrphan(grid, union.getNodeA());
            }
        }
    }

    private void markOrphan(ServerGrid grid, Node node) {
        if(orphans == null) orphans = new HashSet<>();
        boolean modified = orphans.add(node);
        if(modified) ServerGrid.ACTION_LOG.line("  - marked " + grid.describe(node) + " as an orphan");
    }

    protected void forgetNode(ServerGrid grid, RemovalEntry removal, Node node) {
        ServerGrid.ACTION_LOG.line("  - deleting " + grid.describe(node));
        ObjectOpenHashSet<Node> transitives = transitiveTree.remove(node);
        if(transitives != null) {
            ServerGrid.ACTION_LOG.line("  - deleted root cluster containing " + transitives.size() + " node(s) at " + grid.describe(node));
            transitives.remove(node);
            if(!transitives.isEmpty()) {
                Node newRoot = Node.choosePrimary(transitives, 10);
                ServerGrid.ACTION_LOG.line("  - retargeting cluster to new root " + grid.describe(newRoot));
                transitiveTree.put(newRoot, transitives);
            }
        }

        Node root = findTransitiveRoot(node);
        transitives = transitiveTree.get(root);
        if(transitives != null && transitives.remove(node)) {
            ServerGrid.ACTION_LOG.line("  - deleted " + grid.describe(node) + " from cluster belonging to " + grid.describe(root));
        } else ServerGrid.ACTION_LOG.line("  - skipped transitive deallocation for " + grid.describe(node) + " from nonexistant cluster at " + grid.describe(root));
        
        Node direct = relations.remove(node);
        if(direct != null) ServerGrid.ACTION_LOG.line("  - removed relation to " + grid.describe(direct));

        Set<Node> connected = adjacencyTree.get(node);
        if(connected == null) return;
        for(Node adjNode : connected) {
            NodePair toRemove = new NodePair(node, adjNode);
            removal.markUnion(toRemove);
            ServerGrid.ACTION_LOG.line("  - marked related union " + grid.describe(toRemove));
        }
    }

    /**
     * Uses recursive flood-fill across this netlist's adjacency matrix to populate
     * the transitive tree with the given nodes. This is used to break up discontinuities
     * that show up in the transitive tree after nodes are removed from it.
     * @param grid The grid that called this method on this netlist
     * @param head An arbitrary node to start from. This should usually previous transitive
     * mapping that the contents of <code>branch</code> belonged to.
     * @param branch The nodes to patch. These must exist in this netlist's adjacency matrix
     * for this method to produce proper results.
     */
    protected void applyTransitivity(ServerGrid grid, @Nullable Node head, Set<Node> branch) {

        Set<Node> oldBranch = transitiveTree.remove(head);
        if(oldBranch != null) ServerGrid.ACTION_LOG.line("  - removed old branch with " 
            + oldBranch.size() + " node(s)");
        ServerGrid.ACTION_LOG.line("  - initiating flood-fill");

        final Set<Node> visited = new HashSet<>(relations.size());
        final Map<Node, ObjectOpenHashSet<Node>> patches = new HashMap<>(2);
        final ObjectOpenHashSet<Node> workingAdjacents = null;
        ObjectOpenHashSet<Node> outputCluster = null;

        if(head != null) ServerGrid.ACTION_LOG.add(" at " + grid.describe(head));
        head = null;
        for(Node node : branch) {
            if(visited.contains(node)) continue;
            outputCluster = new ObjectOpenHashSet<>(branch.size());
            head = ffr(grid, head, node, workingAdjacents, outputCluster, visited);
            if(outputCluster.size() < 1) continue;
            outputCluster.trim();
            ServerGrid.ACTION_LOG.line("  - Marked a new cluster at " + grid.describe(head) + " containing " + outputCluster.size() + " node(s)");
            patches.put(head, outputCluster);
            head = null; // <- very important since we can't weigh the head of a new branch against a previous one
        }

        // whether or not we need to iterate over a list of patches instead
        // of applying them during the initial flood fill is up in the air
        for(Map.Entry<Node, ObjectOpenHashSet<Node>> entry : patches.entrySet()) {
            entry.getValue().remove(entry.getKey());
            if(entry.getValue().isEmpty()) continue;
            ServerGrid.ACTION_LOG.line("  - spawning fresh cluster with " 
                + entry.getValue().size() + " children at " + grid.describe(entry.getKey()));
            relations.put(entry.getKey(), entry.getKey());
            transitiveTree.put(entry.getKey(), entry.getValue());
            for(Node n : entry.getValue()) relations.put(n, entry.getKey());
        }
    }

    private Node ffr(ServerGrid grid, @Nullable Node head, Node iteration, @Nullable Set<Node> workingAdjacents, Set<Node> outputCluster, Set<Node> visited) {
        visited.add(iteration);
        ServerGrid.ACTION_LOG.line("    - flood-fill visiting " + grid.describe(iteration));
        head = Node.choosePrimary(head, iteration);
        workingAdjacents = adjacencyTree.get(iteration); // <- we use the recycled instance to avoid continuous re-allocation
        if(workingAdjacents == null || workingAdjacents.isEmpty())
            throw new FloodFillException(grid, head, iteration, outputCluster, visited);
        outputCluster.add(iteration);
        for(Node adjacent : workingAdjacents) {
            if(!visited.contains(adjacent))
                head = ffr(grid, head, adjacent, workingAdjacents, outputCluster, visited);
        }
        return head;
    }

    public int indexOf(StampingComponent component) {
        assertNotDisposed();
        Objects.requireNonNull(component);
        if(sourceCoodinates == null) return -2;
        return sourceCoodinates.getOrDefault(component, -2);
    }

    /**
     * Updates this netlist's matrix dimensions and 
     * finalizes the netlist's indices for each node.
     * Stamps the influence of each node and component
     * so that the solver can process them later.
     * This method only needs to be called when the
     * topology changes.
     * @param netlistIndex
     */
    protected void prepareAndStamp(ServerGrid grid, int netlistIndex) {
        assertNotDisposed();
        if(status != ConvergenceStatus.CHANGES_QUEUED) {
            ServerGrid.ACTION_LOG.line("  - skipped complex reallocations since topology hasn't changed");
            return;
        }
        setStatus(ConvergenceStatus.PREPASSING);
        int nodalIndex = 0;
        for(Map.Entry<Node, ObjectOpenHashSet<Node>> branch : transitiveTree.entrySet()) {
            Node head = branch.getKey();
            Set<Node> branchTopo = branch.getValue();
            grid.allocate(head, netlistIndex, nodalIndex);
            if(head.isGrounded()) {
                grid.allocate(branchTopo, netlistIndex, nodalIndex);
                continue;
            }
            grid.allocate(branchTopo, netlistIndex, nodalIndex);
            grid.allocate(head, netlistIndex, nodalIndex);
            nodalIndex++;
        }
        int size = solverSize();
        if(size < 2) {
            matrixA = null;
            matrixX = null;
            matrixB = null;
            setStatus(ConvergenceStatus.IDLE);
            return;
        }
        matrixA = new DMatrixSparseCSC(size, size);
        matrixX = new DMatrixRMaj(size, 1);
        matrixB = new DMatrixRMaj(size, 1);
        setStatus(ConvergenceStatus.STAMPING);
        if(stampers == null) return;
        for(StampingComponent component : stampers)
            component.stamp(grid, this);
        setStatus(ConvergenceStatus.IDLE);
    }

    /**
     * Solves this netlist for voltages at every node.
     * @param solver {@link SolverAlgorithm solver method} to use
     * @return This ConvergenceCoordinator, modified as a result of this call
     */
    protected void runSolver(ServerGrid grid, SolverAlgorithm solver) {
        assertNotDisposed();
        Objects.requireNonNull(grid);
        Objects.requireNonNull(solver);
        if(isEmpty() || solverSize() < 2 || matrixA == null || matrixX == null || matrixB == null)
            return;
        setStatus(ConvergenceStatus.SOLVING);
        solver.initialize(this);
        ConvergenceStatus newStatus = solver.run(this);
        Objects.requireNonNull(newStatus);
        if(!newStatus.indicatesSuccess()) {
            grid.warn("A solver run returned status '" + newStatus + ".'" 
                + ". This likely means that the solver has exited in an invlaid state - proceed with caution.");
        }
        setStatus(newStatus);
    }

    protected void setStatus(ConvergenceStatus newStatus) {
        Objects.requireNonNull(newStatus);
        assertNotDisposed();
        if(status == newStatus) return;
        this.status = newStatus;
        this.lastStatusUpdateTime = System.currentTimeMillis();
        ServerGrid.ACTION_LOG.line("  - Set status of netlist @" + this.hashCode() + " to '" + newStatus + "' at ").addTime();
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
        assertNotDisposed();
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
        assertNotDisposed();
        if(index < 0) return;
        matrixB.set(index, 0, value);
    }

    /**
     * <code>Ax = b, n = indexer length + netlist length</code><p>
     * @return <code>A (Matrix[n][n])</code>
     */
    public DMatrixSparseCSC matrix() {
        assertNotDisposed();
        return matrixA;
    }

    /**
     * <code>Ax = b, n = indexer length + netlist length</code><p>
     * @return <code>x (Vector[n][1])</code>
     */
    public DMatrixRMaj solution() {
        assertNotDisposed();
        return matrixX;
    }

    /**
     * <code>Ax = b, n = indexer length + netlist length</code><p>
     * @return <code>b (Vector[n][1])</code>
     */
    public DMatrixRMaj terms() {
        assertNotDisposed();
        return matrixB;
    }

    public Map<Node, Set<Node>> transitivityView() {
        assertNotDisposed();
        return Collections.unmodifiableMap(transitiveTree);
    }

    public int transitiveSize() {
        return transitiveTree == null ? 0 : transitiveTree.size();
    }

    public int deepSize() {
        return relations == null ? 0 : relations.size();
    }

    public int solverSize() {
        int output = sourceCount() + nodeCount();
        if(referenceNode != null) output--;
        return output;
    }

    public int sourceCount() {
        return sourceCoodinates == null ? 0 : sourceCoodinates.size();
    }

    public int nodeCount() {
        return nodeCoordinates == null ? 0 : nodeCoordinates.size();
    }

    public boolean isEmpty() {
        return transitiveTree == null || transitiveTree.isEmpty();
    }

    /**
     * Used by {@link SolverAlgorithm solvers} to quickly create 
     * and return a one-dimensional array pre-configured to the correct size.
     * @return A {@link DMAtrixRMaj vector} whose length is the number of rows in this netlist's matrix.
     */
    public DMatrixRMaj createWorkingVector() {
        assertNotDisposed();
        if(matrixA == null) throw new IllegalStateException("Working vector cannot be created for an uninitialized or idling GridDomain (This netlist's underlying matrix is null!)");
        return new DMatrixRMaj(matrixA.getNumRows(), 1);
    }

    @Override
    public void dispose() {
        setStatus(ConvergenceStatus.DISPOSED);
        if(referenceNode != null) referenceNode.dispose();
        transitiveTree = null;
        adjacencyTree = null;
        relations = null;
        stampers = null;
        sourceCoodinates = null;
        nodeCoordinates = null;
        indexerCursor = 0;
        matrixA = null;
        matrixX = null;
        matrixB = null;
        referenceNode = null;
        if(orphans != null) Mechano.LOGGER.warn("A netlist was disposed with unprocessed orphans. This likely indicates a lifecycle issue.");
        orphans = null;
    }

    @Override
    public boolean hasBeenDisposed() {
        return status == ConvergenceStatus.DISPOSED;
    }

    private static class FloodFillException extends RuntimeException {
        public FloodFillException(ServerGrid grid, @Nullable Node head, Node iteration, Set<Node> workingCluster, Set<Node> visited) {
            super(FloodFillException.makeMessage(grid, head, iteration, workingCluster, visited));
        }

        private static String makeMessage(ServerGrid grid, @Nullable Node head, Node iteration, Set<Node> workingCluster, Set<Node> visited) {
            String output = "Failed while performing flood-fill at " + grid.describe(head) + ", " + grid.describe(iteration) + "\n";
            output += "cluster:\n" + grid.describe(workingCluster);
            output += "visited:\n" + grid.describe(visited);
            return output;
        }
    }
}
