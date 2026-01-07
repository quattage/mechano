package com.quattage.mechano.api.grid.topology.netlist;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.GridReferent.SourceIdentifier;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.grid.topology.vertex.Node;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;

/**
 * A strange amalgam of a union-find and an adjacency matrix stored as a
 * pair of tree maps. This class is statically typed to store {@link Node}
 * instances (using natural ordering) in a domain-aware connectivity cache 
 * that allows for the kind of flood-fill and pathfinding optimizations 
 * you'd normally see in an adjacency matrix. <p>
 * With the {@link #find} method, you may also access transitive roots
 * like a traditional disjoint set with path compression. Unlike a normal
 * disjoint set, however, this class supports a proper {@link #remove} 
 * operation without having to rebuild the entire data structure.
 */
public class NodeUnionSet {

    protected Object2ObjectOpenHashMap<Node, ObjectOpenHashSet<Node>> transitiveTree;  // union-find transitive access (semi-cyclic)
    protected Object2ObjectOpenHashMap<Node, ObjectOpenHashSet<Node>> adjacencyTree;    // undirected graph (undirected, non-cyclic)
    protected Object2ObjectOpenHashMap<Node, Node> relations;                           // union-find discoverability (cyclic)

    public NodeUnionSet() {
        relations = new Object2ObjectOpenHashMap<>(8);
        transitiveTree = new Object2ObjectOpenHashMap<>(32);
        adjacencyTree = new Object2ObjectOpenHashMap<>(16);
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
        Node p = relations.get(node);
        if(node == p) return node;
        Node root = find(p);
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
     * 
     * @param a node
     * @param b node
     */
    public void union(Node a, Node b) {
        add(a); add(b);
        Node aP = find(a);
        Node bP = find(b);
        if(aP == bP) return;
        Node primary = Node.choosePrimary(aP, bP);
        Node secondary = primary == aP ? bP : aP;
        relations.put(secondary, primary);
        transitiveJoin(primary, secondary);
        adjacentJoin(a, b);
        adjacentJoin(b, a);
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
        Map<Node, Pair<Node, Node[]>> affected = new HashMap<>();
        for(Node removed : nodes) {
            if(removed == null) continue;
            Node root = find(removed);
            if(root == null) continue;
            affected.computeIfAbsent(root, r -> Pair.of(removed, NodalCluster.getConstituents(this, r)));
        }
        for(Node toRemove : nodes) removeNode(toRemove);
        if(!patch) return;
        for(Map.Entry<Node, Pair<Node, Node[]>> entry : affected.entrySet())
            NodalCluster.applyPatches(this, entry.getValue().getFirst(), NodalCluster.ofDiscontinuities(this, entry.getValue().getSecond()));
        for(Node rerooted : affected.keySet()) {
            Node cyclicRoot = relations.get(rerooted);
            if(cyclicRoot != null && rerooted != cyclicRoot)
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
        NodalCluster.applyPatches(this, root, NodalCluster.ofDiscontinuities(this, splitBranch));
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

    public void assignIndices() {
        if(isEmpty()) return;
        int index = -1;
        final Iterator<Map.Entry<Node, ObjectOpenHashSet<Node>>> nodeIter = transitiveTree.entrySet().iterator();
        while(nodeIter.hasNext()) {
            Map.Entry<Node, ObjectOpenHashSet<Node>> branch = nodeIter.next();
            Set<Node> branchTopo = branch.getValue();
            Node head = branch.getKey();
            if(head == null) {
                nodeIter.remove();
                continue;
            }
            // remove orphans
            if(branchTopo == null || branchTopo.isEmpty()) {
                nodeIter.remove();
                relations.remove(head);
                continue;
            }
            head.setNodalIndex(index++);
            for(Node leaf : branchTopo)
                leaf.setNodalIndex(head.getNodalIndex());
        }
        relations.trim();
        transitiveTree.trim();
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
     */
    public Set<Node> roots() {
        return Collections.unmodifiableSet(transitiveTree.keySet());
    }

    /**
     * The relations map contains transitive node discoverability
     * represented as a key-value pairs. This map is compressed so that
     * transitive lookups are fast, but adjacency information is lost in
     * the process.
     * @return An immutable view of this UnionSet's relations map
     * @see #roots()
     */
    public Map<Node, Node> relations() {
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

    public boolean isEmpty() {
        return size() <= 0;
    }

    public void reset() {
        relations = new Object2ObjectOpenHashMap<>();
        transitiveTree = new Object2ObjectOpenHashMap<>();
        adjacencyTree = new Object2ObjectOpenHashMap<>();
    }

    @Override
    public String toString() {
        String out = "Transitivity:\n" + asString(transitiveTree, true) + "\n";
        out += "Adjacency:\n" + asString(adjacencyTree, false) + "\n";
        return out.substring(0, out.length() - 1);
    }

    private String asString(Object2ObjectOpenHashMap<Node, ObjectOpenHashSet<Node>> tree, boolean showRelative) {
        String out = "";
        for(Map.Entry<Node, ObjectOpenHashSet<Node>> branch : tree.entrySet()) {
            out += "-- " + summarizeNode(branch.getKey(), showRelative);
            Set<Node> branchTopo = branch.getValue();
            if(branchTopo == null || branchTopo.isEmpty()) {
                out += "   [Stub]";
                continue;
            }
            for(Node leaf : branchTopo)
                out += "   * " + summarizeNode(leaf, showRelative);
        }
        return out;
    }

    private String summarizeNode(Node node, boolean showRelative) {
        String out = "";
        Node relativeRoot = showRelative ? relations.get(node) : null;
            out += node.getComponentID() + " (" + node.getNodalIndex() 
                + (relativeRoot != null ? ", " + relativeRoot.getComponentID() : "") + ")\n";
        return out;
    }

    public String toFullString(ServerGrid grid) {
        String out = "";
        Griddable<?> src = null;
        for(Map.Entry<Node, ObjectOpenHashSet<Node>> entry : transitiveTree.entrySet()) {
            Node root = entry.getKey();
            Set<Node> contents = entry.getValue();
            out += "\n\t▸" + summarizeNodeFull(root);
            src = SourceIdentifier.getSourceFor(root);
            if(src != null) {
                BlockPos bp = src.getBlockPos();
                out += "\n\t\tOwned by " + src.getClass().getSimpleName() + " at [" + bp.getX() + ", " + bp.getY() + ", " + bp.getZ() + "]";
            } else out += "\n\t\tOwned by anonymous source";
            out += "\n\t\tParented to: " + summarizeNodeFull(relations.get(root));
            if(contents == null || contents.isEmpty()) {
                out += "\n\t!! Stub !!";
                continue;
            }
            out += "\n\t\t" + contents.size() + " children:";
            for(Node child : contents) {
                out += "\n\t\t\t" + summarizeNodeFull(child);
                src = SourceIdentifier.getSourceFor(root);
                if(src != null) {
                    BlockPos bp = src.getBlockPos();
                    out += "\n\t\t\t\tOwned by " + src.getClass().getSimpleName() + " at [" + bp.getX() + ", " + bp.getY() + ", " + bp.getZ() + "]";
                } else out += "\n\t\t\t\tOwned by anonymous source";
                out += "\n\t\t\t\tParented to: " + summarizeNodeFull(relations.get(child));
            }
        }
        return out;
    }

    private String summarizeNodeFull(@Nullable Node node) {
        return node == null ? "n/a" : "'" + node.getComponentID() + "' (" + node.getNodalIndex() + ",  #" + node.hashCode() + ")";
    }
}
