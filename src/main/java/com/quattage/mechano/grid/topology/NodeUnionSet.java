
package com.quattage.mechano.grid.topology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.GridDomain;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.grid.GridTracking;
import com.quattage.mechano.grid.Griddable;
import com.quattage.mechano.grid.api.component.CircuitComponent;
import com.quattage.mechano.grid.topology.link.NodePair;
import com.quattage.mechano.switchboard.RemovalLedger.RemovalEntry;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;

/**
 * This class is, at its core, a union-find / disjoint set, augmented
 * with the addition of an undirected adjacency matrix to allow for the 
 * lowest possible locality on operations that would otherwise be 
 * expensive. In no particular order, this class is:
 * <ul>
 * <li>
 * statically typed to store {@link Node node} instances 
 * (using natural ordering) in a subdomain-aware connectivity cache
 * </li>
 * <li>
 * capable of {@link #remove removing} individual nodes without having 
 * to rebuild the entire transitive/relational acceleration structure
 * </li>
 * <li>
 * optimized for traditional adjacency operations like depth-first-search and 
 * A* using the {@link NodalCluster}
 * </li>
 * <li>
 * ideal for multi-nodal analysis, since nodes are 
 * {@link #finalizeTopology indexed} per transitive root, rather than per node. 
 * This dramatically reduces the size of the solver matrices. 
 * </li>
 * </ul>
 */
public class NodeUnionSet {

    protected Object2ObjectOpenHashMap<Node, ObjectOpenHashSet<Node>> transitiveTree;   // union-find transitive access (semi-cyclic)
    protected Object2ObjectOpenHashMap<Node, ObjectOpenHashSet<Node>> adjacencyTree;    // adjacency matrix (undirected, non-cyclic)
    protected Object2ObjectOpenHashMap<Node, Node> relations;                           // union-find discoverability (cyclic)

    public NodeUnionSet() {
        this(32);
    }

    public NodeUnionSet(int size) {
        relations = new Object2ObjectOpenHashMap<>(size);
        transitiveTree = new Object2ObjectOpenHashMap<>(size);
        adjacencyTree = new Object2ObjectOpenHashMap<>(size);
    }

    /**
     * Returns the combined result of <code>a</code> and <code>b</code>.
     * Does not guarantee continuity between the nodes in these two sets.
     * If you call this method, you must manually create at least one
     * link between a node in <code>a</code> and a node in <code>b</code>,
     * or this set's acceleration structure will fail to compress properly
     * and you'll run into performance issues. <p>
     * This method leaves <code>b</code> intact, but after concatenation 
     * it will share node references with <code>a</code>. If you don't need
     * it anymore, be sure to {@link #reset} <code>b</code> after this method 
     * call to prevent bad access later.
     * @param a The first NodeUnionSet
     * @param b The second NodeUnionSet
     * @return <code>a</code> with all of the contents of <code>b</code>
     * added to it
     */
    public static NodeUnionSet concatenate(NodeUnionSet a, NodeUnionSet b) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        a.adjacencyTree.putAll(b.adjacencyTree);
        a.transitiveTree.putAll(b.transitiveTree);
        a.relations.putAll(b.relations);
        return a;
    }

    public void finalizeTopology(GridDomain domain, int domainIndex) {
        if(isEmpty()) return;
        int nodalIndex = 0;
        final Set<Node> orphans = new HashSet<>(Math.max(2, transitiveTree.size() / 4));
        for(Map.Entry<Node, ObjectOpenHashSet<Node>> branch : transitiveTree.entrySet()) {
            Node head = branch.getKey();
            Set<Node> branchTopo = branch.getValue();
            if(branchTopo == null || branchTopo.isEmpty()) {
                orphans.add(head);
                continue;
            }
            head.setDomainIndex(domainIndex);
            if(head.isGrounded()) {
                if(nodalIndex > 0) {
                    throw new IllegalStateException("Encountered illegal topology while finalizing a NodeUnionSet " 
                        + " - This set contained a redundant ground node at root index " + nodalIndex + "!");
                }
                index(domain, branchTopo, -1, domainIndex);
                continue;
            }
            index(domain, branchTopo, nodalIndex, domainIndex);
            domain.indexer().add(head, nodalIndex);
            nodalIndex++;
        }
    }

    // assigns indices for a single branch
    private void index(GridDomain domain, Set<Node> branchTopo, int nodalIndex, int domainIndex) {
        for(Node leaf : branchTopo) {
            if(leaf == null) {
                throw new NullPointerException("Encountered illegal topology while finalizing a NodeUnionSet " 
                    + " - This set contained a null mapping!");
            }
            leaf.setDomainIndex(domainIndex);
            domain.indexer().add(leaf, nodalIndex);
        }
    }

    /**
     * Gets the transitive root node currently functioning
     * as <code>node</code>'s parent. This method is 
     * required when refering to nodes arbitrarily in code, 
     * since all nodal operations must be performed on the 
     * transitive root of any given node rather than on the
     * node itself. <p>
     * Calls to this method also modify this NodeUnionSet's 
     * {@link #relations} map (via path compression) 
     * such that successive calls take much less time to
     * compute a result.
     * @param node Node to find the root for
     * @return The root node that owns <code>node</code>
     */
    public Node find(Node node) {
        return find(node, 0);
    }

    private Node find(Node node, int iter) {
        if(iter > 1024) {
            Mechano.LOGGER.warn("overflow detected during path compression");
            return node;
        }
        Node p = relations.get(node);
        if(node == p) return node;
        Node root = find(p, iter + 1);
        if(relations.get(node) != root) {
            Set<Node> branch = transitiveTree.get(p);
            if(branch != null) branch.remove(node);
            relations.put(node, root);
            Set<Node> rr = transitiveTree.get(root);
            if(rr != null) rr.add(node);
        }
        return root;
    }

    /**
     * Joins <code>a</code> and <code>b</code>.
     * This method also adds these nodes if they
     * don't already exist within this NodeUnionSet.
     * <p>
     * @param a node
     * @param b node
     */
    public void union(Node a, Node b) {
        add(a); add(b);
        Node aP = find(a);
        Node bP = find(b);
        if(aP != bP) {
            Node primary = Node.choosePrimary(aP, bP);
            Node secondary = primary == aP ? bP : aP;
            relations.put(secondary, primary);
            transitiveJoin(primary, secondary);
        }
        adjacentJoin(a, b);
        adjacentJoin(b, a);
    }

    /**
     * Joins <code>a</code> and <code>b</code>.
     * This method doesn't do any transitivity joining
     * or path compression. <p>
     * 
     * <strong>Calling this method will leave this NodeUnionSet in a broken state!</strong><p>
     * This method is intended to be used when loading data from a serialized form. Path
     * and serialization compression mean that the transitivity structure must be re-created
     * from adjacency data on load. You must call {@link #patchAndTrim()} after this method 
     * to fix the now outdated transitivity structure.
     * @param a node 
     * @param b node
     */
    public boolean loadUnion(Node a, Node b) {
        relations.put(a, a);
        adjacentJoin(a, b);
        return true;
    }

    /**
     * A somewhat expensive method that manually
     * compresses all transitive paths in this NodeUnionSet.
     * This operation is usually done automatically over
     * the course of several calls to {@link #find}, but
     * you can do it here should such a thing be useful.
     * <p> This method was written primarily for unit testing,
     * but could also be used in the future for serialization.
     */
    public void compress() {
        for(Node n : relations.keySet())
            find(n);
    }

    /**
     * merges two nodes in the transitive tree and collapses
     * the result
     */
    private void transitiveJoin(Node primary, Node secondary) {
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

    /**
     * merges two nodes in the adjacency tree asymmetrically
     */
    private void adjacentJoin(Node primary, Node secondary) {
        ObjectOpenHashSet<Node> branch = adjacencyTree.get(primary);
        if(branch == null) {
            branch = new ObjectOpenHashSet<>();
            adjacencyTree.put(primary, branch);
        }
        branch.add(secondary);
    }

    public void massRemove(ServerGrid grid, RemovalEntry removals) {
        massRemove(grid, removals, null);
    }

    /**
     * An optimized removal method that allows the removal of singular nodes as well as 
     * the ability to sever connections between node pairs. Bulk removal is performed all
     * at once to avoid extraneous DFS runs.
     * @param removals A wrapper object containing nodes and unions to remove/de-link
     * @param nodes A collection of {@link NodePair} objects representing unions to remove.
     * @see #removeAll
     */
    public void massRemove(ServerGrid grid, RemovalEntry removals, @Nullable StringBuilder manifest) {
        if(manifest != null) {
            manifest.append("\n>> Netlist structure before process:\n" + this + "-- \nProcessing " + removals);
            manifest.append("\n--\n>> Beginning removal assessment:");
        }

        final Map<Node, @Nullable Set<Node>> affected = new HashMap<>(transitiveTree.size());
        removals.forEachAffected(node -> {
            if(node == null) return;
            Node root = find(node);
            if(root == null) return;
            if(!affected.containsKey(root))
                affected.put(root, transitiveTree.get(root));
            if(manifest != null)
                manifest.append("\n  - flagged transitive root (" + root.getComponentID() + " @" + root.hashCode() 
                    + ") as modified by removal of (" + node.getComponentID() + " @" + node.hashCode() + ")");
        });

        if(manifest != null) manifest.append("\n--\n>> Patching secondary acceleration structures:");
        removals.forEachUnion(union -> patchOut(removals, union, manifest));
        removals.forEachNode(node -> patchOut(removals, node, manifest));
        if(manifest != null) manifest.append("\n--\n>> Flooding discontinuities across " + affected.size() + " clusters:");

        for(Map.Entry<Node, Set<Node>> entry : affected.entrySet())
            splitDiscontinuitiesInBranch(entry.getKey(), entry.getValue(), manifest);

        if(manifest != null) manifest.append("\n--\n>> Removal complete:\n" + this + "--\n\n");
    }

    private void patchOut(RemovalEntry removal, Node node, @Nullable StringBuilder manifest) {
        Node direct = relations.remove(node);
        if(manifest != null) {
            manifest.append("\n - deleting (" + node.getComponentID() + " @" + node.hashCode() + ")");
            if(direct != null) manifest.append(", removed relation to (" + direct.getComponentID() + " @" + direct.hashCode() + ")");
        }
        Set<Node> transitives = transitiveTree.remove(node);
        if(transitives != null && manifest != null)
            manifest.append("\n - deleting root cluster containing " + transitives.size() + " nodes at (" + node.getComponentID() + " @" + node.hashCode() + ")");
        Set<Node> connected = adjacencyTree.remove(node);
        if(connected == null) return;
        for(Node adjNode : connected) {
            Set<Node> inverseConnected = adjacencyTree.get(adjNode);
            if(manifest != null)
                manifest.append("\n - de-linked adjacents (" + node.getComponentID() 
                    + " @" + node.hashCode() + ") <--> (" + adjNode.getComponentID() + " @" + adjNode.hashCode() + ")");
            if(inverseConnected != null) {
                inverseConnected.remove(node);
                if(manifest != null)
                    manifest.append("\n - de-linked adjacents (" + adjNode.getComponentID() + " @" 
                        + adjNode.hashCode() + ") <--> (" + node.getComponentID() + " @" + node.hashCode() + ")");
                removal.markUnion(new NodePair(node, adjNode));
            }
            if(inverseConnected.isEmpty()) 
                adjacencyTree.remove(adjNode);
        }
    }

    private void patchOut(RemovalEntry removal, NodePair union, @Nullable StringBuilder manifest) {
        Node directA = relations.get(union.getNodeB());
        Node directB = relations.get(union.getNodeA());
        if(union.getNodeA().equals(directA))
            relations.remove(union.getNodeA());
        if(union.getNodeB().equals(directB))
            relations.remove(union.getNodeB());
        ObjectOpenHashSet<Node> connected = adjacencyTree.get(union.getNodeA());
        if(connected == null || connected.isEmpty()) removal.markNode(union.getNodeA());
        else {
            boolean modified = connected.remove(union.getNodeB());
            if(modified) {
                if(manifest != null) manifest.append("\n - de-linked union targeting (" 
                    + union.getNodeB().getComponentID() + " @" + union.getNodeB().hashCode() + ")");
                if(connected == null || connected.isEmpty())
                    removal.markNode(union.getNodeA());
            }
        }
        connected = adjacencyTree.get(union.getNodeB());
        if(connected == null || connected.isEmpty()) removal.markNode(union.getNodeB());
        else {
            boolean modified = connected.remove(union.getNodeA());
            if(modified) {
                if(manifest != null) manifest.append("\n - de-linked union targeting (" 
                    + union.getNodeA().getComponentID() + " @" + union.getNodeA().hashCode() + ")");
                if(connected == null || connected.isEmpty())
                    removal.markNode(union.getNodeB());
            }
        } 
    }

    private void splitDiscontinuitiesInBranch(@Nullable Node head, Set<Node> branch, @Nullable StringBuilder manifest) {
        if(branch == null || branch.size() < 1) {
            if(manifest != null && head != null) manifest.append("\n - skipping branch discovery at (" 
                + head.getComponentID() + " @" + head.getComponentID() + ") because the supplied branch is empty");
            return;
        }

        if(manifest != null && head != null) manifest.append("\n - discovering ~" + branch.size() 
            + " neighbours at (" + head.getComponentID() + " @" + head.hashCode() + ")");

        final Set<Node> visited = new HashSet<>(relations.size());
        final Map<Node, ObjectOpenHashSet<Node>> patches = new HashMap<>(2);
        ObjectOpenHashSet<Node> workingCluster;
        for(Node node : branch) {
            if(visited.contains(node)) continue;
            workingCluster = new ObjectOpenHashSet<>(branch.size());
            head = ffr(head, node, workingCluster, visited);
            if(workingCluster.size() < 1) continue;
            workingCluster.trim();
            patches.put(head, workingCluster);
            head = null; // <- very important since we can't weigh the head of a new branch against a previous one
        }
        // whether or not we need to iterate over a list of patches instead of applying them during the initial
        // flood fill is up in the air
        for(Map.Entry<Node, ObjectOpenHashSet<Node>> entry : patches.entrySet()) {
            if(manifest != null) manifest.append("\n - spawning fresh cluster with " + entry.getValue().size() 
                + " children at (" + entry.getKey().getComponentID() + " @" + entry.getKey().hashCode() + ")");
            relations.put(entry.getKey(), entry.getKey());
            transitiveTree.put(entry.getKey(), entry.getValue());
            for(Node n : entry.getValue()) relations.put(n, entry.getKey());
        }
    }

    private Node ffr(@Nullable Node head, Node iteration, Set<Node> branch, Set<Node> visited) {
        visited.add(iteration);
        head = Node.choosePrimary(head, iteration);
        Set<Node> adjacents = adjacencyTree.get(iteration);
        if(adjacents == null || adjacents.isEmpty()) return head;
        branch.add(iteration);
        for(Node adjacent : adjacents) {
            if(!visited.contains(adjacent))
                ffr(head, adjacent, branch, visited);
        }
        return head;
    }

    public List<NodeUnionSet> splitByDomain(GridDomain origin) {
        final Set<Node> visited = new HashSet<>(relations.size());
        final Map<Node, ObjectOpenHashSet<Node>> patches = new HashMap<>(2);
        final List<NodeUnionSet> output = new ArrayList<>(2);

        for(Node node : adjacencyTree.keySet()) {
            
        }

        return output;
    }

    private void complexFFR(GridDomain origin, @Nullable Node head, Node iteration, Set<Node> branch, Set<Node> visited) {
        visited.add(iteration);
        head = Node.choosePrimary(head, iteration);
        ObjectOpenHashSet<Node> adjacents = adjacencyTree.get(iteration);
        Griddable<?> source = GridTracking.getReferentOrThrow(origin.getWorld(), iteration);
        if(source != null) {
            CircuitComponent component = source.getComponent();
            if(component == null) origin.getHostGrid().warn("Skipped indexing node (" + iteration.getComponentID() + " @" + iteration.hashCode() + ") since it failed to provide CircuitComponent.");
            else if(component.nodeCount() <= 0) visited.add(iteration);
            else {
                adjacents.ensureCapacity(component.nodeCount());
                component.forEachNode(node -> adjacents.add(node));
            }
        }
        if(adjacents == null || adjacents.isEmpty()) return;
        branch.add(iteration);
        for(Node adjacent : adjacents) {
            if(!visited.contains(adjacent))
                complexFFR(origin, head, adjacent, branch, visited);
        }
    }

    public boolean hasConnections(Node node) {
        Set<Node> connected = adjacencyTree.get(node);
        return connected != null && !connected.isEmpty();
    }

    /**
     * Add <code>node</code> directly. Calling this method 
     * isn't reccomended unless you know what you're doing, since
     * this method doesn't guarantee that <code>node</code> is not
     * orphaned. This method is automatically invoked by {@link #union()}
     * @param node node to add
     * @return <code>true</code> if this NodeUnionSet didn't 
     * already contain a mapping for <code>node</code>
     */
    public boolean add(Node node) {
        Objects.requireNonNull(node);
        if(relations.containsKey(node)) return false;
        relations.put(node, node);
        transitiveTree.put(node, null);
        adjacencyTree.put(node, null);
        return true;
    }

    /**
     * Gets a node by its {@link CircuitComponent#getComponentID component id}.
     * Requires iteration over this NodeUnionSet's entire transitive tree.
     * This is primarily used for testing, since named components' IDs are 
     * logically insignificant and this set isn't optimized against them.
     * @param id ID to find - cannot be null or blank.
     * @return The first occurence of a node called <code>id</code>, or <code>null</code>.
     */
    public @Nullable Node getByComponentID(String id) {
        if(id == null) throw new NullPointerException("Couldn't find Node by ID - The provided ID is null!");
        if(id.isBlank()) throw new NullPointerException("Couldn't find Node by ID - The provided ID is blank!");
        for(Map.Entry<Node, ObjectOpenHashSet<Node>> branch : transitiveTree.entrySet()) {
            Node label = branch.getKey();
            if(label != null && label.getComponentID().equals(id)) 
                return label;
            Set<Node> branchTopo = branch.getValue();
            if(branchTopo == null || branchTopo.isEmpty())
                continue;
            for(Node leaf : branchTopo)
                if(leaf != null && leaf.getComponentID().equals(id))
                    return leaf;
        }
        return null;
    }

    /**
     * The roots set provides readonly access to the raw contents of the
     * transtive tree's keyset. Every node currently functioning as a root
     * will be available in this set.
     * @return An immutable view of this UnionSet's transitive keyset
     * @see #relativity()
     * @see #adjacency()
     */
    public Map<Node, Set<Node>> transitivity() {
        return Collections.unmodifiableMap(transitiveTree);
    }

    /**
     * The adjacency map contains the data you'd expect
     * to see in a standard undirected adjacency matrix. 
     * @return An immutable view of this NodeUnionSet's adjacency map
     * @see #transitivity()
     * @see #relativity()
     */
    public Map<Node, Set<Node>> adjacency() {
        return Collections.unmodifiableMap(adjacencyTree);
    }

    /**
     * The relations map contains transitive node discoverability
     * represented as a key-value pairs. This map is compressed so that
     * transitive lookups are fast, but adjacency information is lost in
     * the process.
     * @return An immutable view of this UnionSet's relations map
     * @see #transitivity()
     * @see #adjacency()
     */
    public Map<Node, Node> relativity() {
        return Collections.unmodifiableMap(relations);
    }

    /**
     * @return The total amount of nodes in this NodeUnionSet
     * @see #size
     */
    public int deepSize() {
        return relations.size();
    }

    /**
     * @return The amount of unique transitive branches in this NodeUnionSet
     * @see #deepSize
     */
    public int size() {
        return transitiveTree.size();
    }

    /**
     * Trims internal hash tables to minimize memory footprint
     * @param toRemove Optional set of nodes that will be iteratively stripped from this
     * set as a result of this call
     */
    public void trim() {
        transitiveTree.trim();
        adjacencyTree.trim();
        relations.trim();
    }

    public boolean isEmpty() {
        return size() <= 0;
    }

    public void reset() {
        relations = new Object2ObjectOpenHashMap<>();
        transitiveTree = new Object2ObjectOpenHashMap<>();
        adjacencyTree = new Object2ObjectOpenHashMap<>();
    }

    public String toFullString(ServerGrid grid, GridDomain domain) {
        if(isEmpty()) return "\n  Empty";
        String out = "";
        Griddable<?> src = null;
        for(Map.Entry<Node, ObjectOpenHashSet<Node>> entry : transitiveTree.entrySet()) {
            Node root = entry.getKey();
            Set<Node> contents = entry.getValue();
            out += "\n  ▸" + summarizeNode(root, domain.indexer());
            src = GridTracking.getReferentOrThrow(grid.getWorld(), root);
            if(src != null) {
                BlockPos bp = src.getBlockPos();
                out += "\n    Owned by " + src.getClass().getSimpleName() + " at [" + bp.getX() + ", " + bp.getY() + ", " + bp.getZ() + "]";
            } else out += "\n    Owned by anonymous source";
            out += "\n    Parented to: " + summarizeNode(relations.get(root), domain.indexer());
            if(contents == null || contents.isEmpty()) {
                out += "\n    0 children [[!! Stub !!]]";
                continue;
            }
            out += "\n    " + contents.size() + " children:";
            for(Node child : contents) {
                out += "\n      " + summarizeNode(child, domain.indexer());
                src = GridTracking.getReferentOrThrow(grid.getWorld(), child);
                if(src != null) {
                    BlockPos bp = src.getBlockPos();
                    out += "\n        Owned by " + src.getClass().getSimpleName() + " at [" + bp.getX() + ", " + bp.getY() + ", " + bp.getZ() + "]";
                } else out += "\n        Owned by anonymous source";
                out += "\n        Parented to: " + summarizeNode(relations.get(child), domain.indexer());
            }
        }
        return out;
    }

    @Override
    public String toString() {
        String out = "transitive:\n";
        for(Map.Entry<Node, ObjectOpenHashSet<Node>> entry : transitiveTree.entrySet()) {
            out += "⬥ " + entry.getKey().getComponentID() + " @" + entry.getKey().hashCode() + "\n";
            for(Node node : entry.getValue())
                out += "  ┕ " + node.getComponentID() + " @" + node.hashCode() + "\n";
        }
        out += "adjacent:\n";
        for(Map.Entry<Node, ObjectOpenHashSet<Node>> entry : adjacencyTree.entrySet()) {
            out += "⬥ " + entry.getKey().getComponentID() + " @" + entry.getKey().hashCode() + "\n";
            for(Node node : entry.getValue())
                out += "  ┕ " + node.getComponentID() + " @" + node.hashCode() + "\n";
        }
        out += "relative:\n";
        String roots = "";
        for(Map.Entry<Node, Node> entry : relations.entrySet()) {
            if(entry.getKey().equals(entry.getValue())) {
                roots += "* root (" + entry.getKey().getComponentID() + " @" + entry.getKey().hashCode() + ") ↺ \n";
                continue;
            }
            out += "* (" + entry.getKey().getComponentID() + " @" + entry.getKey().hashCode() + ") → (" 
                + entry.getValue().getComponentID() + " @" + entry.getValue().hashCode() + ")\n";
        }
        return out + roots;
    }

    private String summarizeNode(@Nullable Node node, NetlistIndexer indexer) {
        return node == null ? "n/a" : "'" + node.getComponentID() + "' (" + indexer.get(node) + ", " + node.getDomainIndex() + ", @" + node.hashCode() + ")";
    }
}
