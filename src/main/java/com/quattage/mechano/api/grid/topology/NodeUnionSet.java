
package com.quattage.mechano.api.grid.topology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.GridTracking;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.topology.landmark.Node;
import com.quattage.mechano.api.grid.topology.landmark.link.NodePair;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.createmod.catnip.data.Pair;
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
        stripAndTrim(orphans, domain);
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

    /**
     * the opposite of adjacentJoin
     */
    private void adjacentDisjoin(Node a, Node b) {
        Set<Node> branchA = adjacencyTree.get(a);
        if(branchA != null) branchA.remove(b);
        Set<Node> branchB = adjacencyTree.get(b);
        if(branchB != null) branchB.remove(a);
    }

    /**
     * An optimized removal method that allows the removal of singular nodes as well as 
     * the ability to sever connections between node pairs. Bulk removal is performed all
     * at once to avoid extraneous DFS runs.
     * @param nodes A collection of nodes to remove.
     * @param nodes A collection of {@link NodePair} objects representing unions to remove.
     * @see #removeAll
     */
    public void massRemove(ServerGrid grid, @Nullable Collection<Node> nodesToRemove, @Nullable Collection<NodePair> unionsToRemove) {
        final Map<Node, Pair<Node, Node[]>> affected = new HashMap<>();
        if(nodesToRemove != null && !nodesToRemove.isEmpty()) {
            for(Node removed : nodesToRemove) {
                if(removed == null) continue;
                Node root = find(removed);
                if(root == null) continue;
                affected.put(root, Pair.of(removed, null));
            }
        }
        if(unionsToRemove != null && !unionsToRemove.isEmpty()) {
            for(NodePair pair : unionsToRemove) {
                Node root = find(pair.getNodeA());
                if(root == null) continue;
                affected.put(root, Pair.of(root, null));
            }
        }
        for(Map.Entry<Node, Pair<Node, Node[]>> entry : affected.entrySet()) {
            Node removed = entry.getValue().getFirst();
            entry.setValue(Pair.of(removed, NodalCluster.getConstituents(this, entry.getKey())));
        }
        if(unionsToRemove != null && !unionsToRemove.isEmpty()) {
            for(NodePair pair : unionsToRemove) {
                adjacentDisjoin(pair.getNodeA(), pair.getNodeB());
                if(!hasConnections(pair.getNodeB())) remove(pair.getNodeA());
                if(!hasConnections(pair.getNodeA())) remove(pair.getNodeB());
            }
        }
        if(nodesToRemove != null && !nodesToRemove.isEmpty()) {
            for(Node toRemove : nodesToRemove) {
                List<NodePair> removed = removeNodeAndGet(grid, toRemove);
                if(removed != null) unionsToRemove.addAll(removed);
            }
        }
        for(Map.Entry<Node, Pair<Node, Node[]>> entry : affected.entrySet())
            NodalCluster.applyPatches(this, entry.getValue().getFirst(), NodalCluster.ofClusters(this, entry.getValue().getSecond()));
        // i totally forgot what this does or why it exists but the tests fail if i remove it
        for(Node rerooted : affected.keySet()) {
            Node cyclicRoot = relations.get(rerooted);
            if(cyclicRoot == null || rerooted != cyclicRoot)
                transitiveTree.remove(rerooted);
            if(!hasConnections(rerooted)) {
                transitiveTree.remove(rerooted);
                adjacencyTree.remove(rerooted);
            }
        }
    }


    /**
     * Removes multiple nodes from this NodeUnionSet at once.
     * The affected branches are collected and processed 
     * intelligently to save on compute time. This method
     * is significantly faster than multiple calls to {@link
     * #remove}.
     * @param nodes A collection of nodes to remove.
     * @param patch (Optional, defaults to <code>true</code>) - 
     * If <code>true</code>, this call will partially reconstruct
     * this NodeUnionSet's transitive acceleration structure. 
     * For API users, leave this enabled unless you intend to 
     * perform some special operation where you patch it yourself
     * later.
     * @see #remove
     */
    public void removeAll(Collection<Node> nodes) {
        removeAll(nodes, true);
    }

    /**
     * Removes multiple nodes from this NodeUnionSet at once.
     * The affected branches are collected and processed 
     * intelligently to save on compute time. This method
     * is significantly faster than multiple calls to {@link
     * #remove}.
     * @param nodes A collection of nodes to remove.
     * @param patch (Optional, defaults to <code>true</code>) - 
     * If <code>true</code>, this call will partially reconstruct
     * this NodeUnionSet's transitive acceleration structure. 
     * For API users, leave this enabled unless you intend to 
     * perform some special operation where you patch it yourself
     * later.
     * @see #remove
     */
    public void removeAll(Collection<Node> nodes, boolean patch) {
        Objects.requireNonNull(nodes);
        if(nodes.isEmpty()) return;
        final Map<Node, Pair<Node, Node[]>> affected = new HashMap<>();
        for(Node removed : nodes) {
            if(removed == null) continue;
            Node root = find(removed);
            if(root == null) continue;
            affected.computeIfAbsent(root, r -> Pair.of(removed, NodalCluster.getConstituents(this, r)));
        }
        for(Node toRemove : nodes) removeNode(toRemove);
        if(!patch) return;
        for(Map.Entry<Node, Pair<Node, Node[]>> entry : affected.entrySet())
            NodalCluster.applyPatches(this, entry.getValue().getFirst(), NodalCluster.ofClusters(this, entry.getValue().getSecond()));
        for(Node rerooted : affected.keySet()) {
            Node cyclicRoot = relations.get(rerooted);
            if(cyclicRoot == null || rerooted != cyclicRoot)
                transitiveTree.remove(rerooted);
        }
    }

    /**
     * Removes a single node from this NodeUnionSet.
     * @param node A node to remove
     * @param patch (Optional, defaults to <code>true</code>) - 
     * If <code>true</code>, this call will partially reconstruct
     * this NodeUnionSet's transitive acceleration structure. 
     * For API users, leave this enabled unless you intend to 
     * perform some special operation where you patch it yourself
     * later.
     * @see #removeAll
     */
    public void remove(Node node) {
        remove(node, true);
    }

    /**
     * Removes a single node from this NodeUnionSet.
     * @param node A node to remove
     * @param patch (Optional, defaults to <code>true</code>) - 
     * If <code>true</code>, this call will partially reconstruct
     * this NodeUnionSet's transitive acceleration structure. 
     * For API users, leave this enabled unless you intend to 
     * perform some special operation where you patch it yourself
     * later.
     * @see #removeAll
     */
    public void remove(Node node, boolean patch) {
        Objects.requireNonNull(node);
        Node root = find(node);
        if(root == null) return;
        Node[] splitBranch = NodalCluster.getConstituents(this, node);
        removeNode(node);
        if(!patch) return;
        NodalCluster.applyPatches(this, root, NodalCluster.ofClusters(this, splitBranch));
    }

    /**
     * performs all the immediate actions associated with removing a single node, but this method
     * will leave the transitive tree in an outdated state
     */
    private void removeNode(Node node) {
        relations.remove(node);
        Set<Node> connected = adjacencyTree.remove(node);
        if(connected == null) return;
        for(Node adjNode : connected) {
            Set<Node> inverseConnected = adjacencyTree.get(adjNode);
            if(inverseConnected != null)
                inverseConnected.remove(node);
            if(inverseConnected.isEmpty()) adjacencyTree.put(adjNode, null);
        }
    }

    private List<NodePair> removeNodeAndGet(ServerGrid grid, Node node) {
        relations.remove(node);
        Set<Node> connected = adjacencyTree.remove(node);
        if(connected == null) return null;
        List<NodePair> removed = new ArrayList<>(connected.size());
        for(Node adjNode : connected) {
            removed.add(new NodePair(node, adjNode));
            Set<Node> inverseConnected = adjacencyTree.get(adjNode);
            if(inverseConnected != null) {
                if(inverseConnected.remove(node))
                    removed.add(new NodePair(adjNode, node));
            }
            else adjacencyTree.put(adjNode, null);
        }
        return removed;
    }

    /**
     * @param node
     * @return <code>true</code> if this node is a root that exists in the transitive tree.
     */
    public boolean isRoot(Node node) {
        return node != null && (relations.get(node) == node || find(node) == node);
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

    public boolean addAll(Collection<Node> nodes) {
        boolean changed = false;
        for(Node node : nodes)
            if(add(node)) changed = true;
        return changed;
    }

    /**
     * Gets a node by its {@link CircuitComponent#getComponentID component id}.
     * Requires iteration over this NodeUnionSet's entire transitive tree.
     * @param id ID to find - cannot be null or blank.
     * @return The first occurence of a node called <code>id</code>, or <code>null</code>.
     */
    public Node getByComponentID(String id) {
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
     * @see #relations()
     * @see #adjacency()
     */
    public Set<Node> transitivity() {
        return Collections.unmodifiableSet(transitiveTree.keySet());
    }

    /**
     * The adjacency map contains the data you'd expect
     * to see in a standard undirected adjacency matrix. 
     * @return An immutable view of this NodeUnionSet's adjacency map
     * @see #transitivity()
     * @see #relations()
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
    public Map<Node, Node> relations() {
        return Collections.unmodifiableMap(relations);
    }

    public Set<Node> connectedTo(Node node) {
        Objects.requireNonNull(node);
        Set<Node> output = adjacencyTree.get(node);
        return output == null ? Collections.emptySet() : output;
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

    // used by finalizeTopology() to remove orphans 
    private void stripAndTrim(Set<Node> toRemove, @Nullable GridDomain domain) {
        if(!toRemove.isEmpty()) {
            for(Node n : toRemove) {
                transitiveTree.remove(n);
                adjacencyTree.remove(n);
                relations.remove(n);
                if(domain != null) 
                    domain.indexer().remove(n);
                n.setDomainIndex(-2);
            }
        }
        trim(); 
    }

    /**
     * Finds all clusters in this NodeUnionSet and updates this object's
     * internal transitivity structure to match what is currently present
     * in the adjacency data. This method can be used when making sweeping
     * changes to this NodeUnionSet. It's better to avoid constant rebuilding
     * after each change in favour of one big rebuild at the end, so you can
     * manually invoke that here.
     * Use this method sparingly, since it can be computationally expensive.
     */
    public void patchAndTrim() {
        Node[] nodesAsArray = relations.keySet().toArray(new Node[relations.size()]);
        List<NodalCluster> clusters = NodalCluster.ofClusters(this, nodesAsArray);
        NodalCluster.applyPatches(this, clusters);
        trim();
    }

    public boolean isEmpty() {
        return size() <= 0;
    }

    public void reset() {
        relations = new Object2ObjectOpenHashMap<>();
        transitiveTree = new Object2ObjectOpenHashMap<>();
        adjacencyTree = new Object2ObjectOpenHashMap<>();
    }

    public void ensureCapacity(int cap) {
        adjacencyTree.ensureCapacity(cap);
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
    
    public void forEachTransitive(BiConsumer<Node, Set<Node>> branchCons) {
        for(Map.Entry<Node, ObjectOpenHashSet<Node>> entry : transitiveTree.entrySet()) {
            Node head = entry.getKey();
            Set<Node> branch = entry.getValue();
            if(head == null) throw new IllegalStateException("Encountered bad topology while traversing transitive branch - The branch returned a null head!");
            if(branch == null) throw new IllegalStateException("Encountered bad topology while traversing transitive branch at " + head + " - This branch's contents set is null!");
            if(branch.isEmpty()) throw new IllegalStateException("Encountered bad topology while traversing transitive branch at " + head + " - This branch has no contents!");
            branchCons.accept(head, branch);
        }
    }

    @Override
    public String toString() {
        String out = "";
        for(Map.Entry<Node, ObjectOpenHashSet<Node>> entry : transitiveTree.entrySet()) {
            out += ">> " + entry.getKey().getComponentID() + "\n";
            for(Node node : entry.getValue())
                out += "   - " + node.getComponentID() + "\n";
        }
        return out;
    }

    private String summarizeNode(@Nullable Node node, NetlistIndexer indexer) {
        return node == null ? "n/a" : "'" + node.getComponentID() + "' (" + indexer.get(node) + ",  #" + node.hashCode() + ")";
    }
}
