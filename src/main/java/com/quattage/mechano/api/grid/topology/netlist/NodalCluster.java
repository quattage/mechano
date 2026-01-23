package com.quattage.mechano.api.grid.topology.netlist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.foundation.Disposable;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class NodalCluster implements Disposable {

    /**
     * Gets an array containing every node that belongs to 
     * the same branch as <code>node</code> in the given
     * NodeUnionSet using a flood-fill. The returned branch 
     * will always include the provided node, unless this 
     * NodeUnionSet doesn't contain any mappings for
     * <code>node</code>. This method is a lighter weight
     * version of {@link #ofBranch} for cases where you don't
     * need a NodalCluster instance. <p> The returned array
     * is a shallow-copy of the one that backs the set 
     * contained within <code>origin</code>, so this array 
     * is modification-safe. Changes made to it will not 
     * inadvertently affect <code>origin</code> itself.
     * @param origin The NodeUnionet that <code>root</code> comes from
     * @param node Node to get the branch for
     * @return An array of nodes. Will never be null.
     * @see #getBranch
     */
    public static Node[] getConstituents(NodeUnionSet origin, Node root) {
        if(origin.isEmpty() || !origin.relations.containsKey(root)) 
            return new Node[0];
        root = origin.find(root);
        ObjectSet<Node> branch = origin.transitiveTree.get(root);
        if(branch == null || branch.isEmpty()) 
            return new Node[0];
        Node[] output = branch.toArray(new Node[branch.size() + 1]);
        output[branch.size()] = root;
        return output;
    }

    /**
     * Creates a new NodalClister instance containing all
     * transitive connections to <code>root</code> in the provided
     * {@link NodeUnionSet}. If the provided node does not exist in 
     * <code>origin</code>, the returned object will contain nothing
     * but <code>node</code>.
     * @param node Node to get the branch for
     * @return A new {@link NodalCluster} instnace.
     * @see #getConstituents
     */
    public NodalCluster ofBranch(NodeUnionSet origin, Node root) {
        if(origin.isEmpty() || origin.relations.containsKey(root)) 
            return NodalCluster.ofEmpty(root);
        root = origin.find(root);
        ObjectOpenHashSet<Node> branch = origin.transitiveTree.get(root);
        if(branch == null || branch.isEmpty()) 
            return NodalCluster.ofEmpty(root);
        return new NodalCluster(root, branch);
    }


    /**
     * Given a root node and its origin set, this method returns a list of new
     * NodalCluster instances which each contain a section of the root branch.
     * @param origin The NodeUnionSet to acquire adjacency from
     * @param root The root node to start with
     * @param exclusions Any number of nodes to exclude when searching
     * @return A new list of NodalClusters
     * @see #ofDiscontinuities(NodeUnionSet, Node[], Node...)
     */
    public static List<NodalCluster> ofDiscontinuities(NodeUnionSet origin, Node root, Node... exclusions) {
        return NodalCluster.ofDiscontinuities(origin, NodalCluster.getConstituents(origin, root), exclusions);
    }

    /**
     * Given a branch and the NodeUnionSet it came from, this method returns a list 
     * of new NodalCluster instances which each contain a section of the root branch.
     * The resulting NodalClusters will be split 
     * @param origin The NodeUnionSet to acquire adjacency from
     * @param branch The branch to find discontinuities in
     * @param exclusions (Optional) any number of nodes to exclude when searching. These nodes 
     * will function as boundaries that prevent the search from proceeding in that direction.
     * @return A new list of NodalClusters
     * @see #ofDiscontinuities(NodeUnionSet, Node, Node...)
     */
    public static List<NodalCluster> ofDiscontinuities(NodeUnionSet origin, Node[] branch, Node... exclusions) {
        if(branch.length <= 0) return Collections.emptyList();
        final Set<Node> visited = new HashSet<>(branch.length + (exclusions == null ? 0 : (exclusions.length * 2)));
        if(exclusions != null) {
            for(int x = 0; x < exclusions.length; x++) {
                Node exclude = exclusions[x];
                if(exclude == null) continue;
                visited.add(exclude);
            }
        }
        final List<NodalCluster> output = new ArrayList<>(branch.length);
        for(Node node : branch) {
            if(visited.contains(node)) continue;
            NodalCluster cluster = new NodalCluster(branch.length);
            cluster.ffr(origin, node, visited);
            if(!cluster.isStub()) {
                cluster.trim();
                output.add(cluster);
            }
        }
        return output;
    }

    public static void applyPatches(NodeUnionSet target, Collection<NodalCluster> clusters) {
        NodalCluster.applyPatches(target, null, clusters);
    }

    /**
     * Runs {@link #patchOnto} for every cluster in the provied collection. 
     * Also cleans the old root that will contain redundant data after this call.
     * @param target The NodeUnionSet to apply patches to
     * @param oldRoot (Optional) The original transitive root node that will be 
     * @param clusters A collection of {@link NodalCluster} objects. The patches are 
     * applied the order of this colleciton.
     */
    public static void applyPatches(NodeUnionSet target, @Nullable Node oldRoot, Collection<NodalCluster> clusters) {
        Objects.requireNonNull(clusters);
        Objects.requireNonNull(target);
        if(target.isEmpty()) return;
        if(clusters.isEmpty()) return;
        boolean keepOldRoot = false;
        for(NodalCluster cluster : clusters) {
            if(cluster.head == oldRoot) keepOldRoot = true;
            cluster.patch(target);
        }
        if(!keepOldRoot && oldRoot != null)
            target.transitiveTree.remove(oldRoot);
    }

    public static NodalCluster ofEmpty(Node head) {
        return new NodalCluster(head, new ObjectOpenHashSet<>(3));
    }

    private @Nullable Node head;
    private ObjectOpenHashSet<Node> contents;

    public NodalCluster(Node head, ObjectOpenHashSet<Node> contents) { set(head, contents); }

    public NodalCluster(int preload) {
        contents = new ObjectOpenHashSet<>(preload);
    }

    /**
     * Sets this NodalCluster's <code>head</code> and <code>contents</code> 
     * to the provided instances. This method is useful for recycling
     * NodalCluster instances during iterative operations.
     * @param head The new head. Cannot be null.
     * @param contents The contents of this cluster. Try not to pass a set
     * that's actively being modified, as it could cause issues later.
     */
    public void set(Node head, ObjectOpenHashSet<Node> contents) {
        Objects.requireNonNull(contents);
        this.head = head;
        this.contents = contents;
    }

    // recursive flood-fill for populating this cluster
    private void ffr(NodeUnionSet origin, Node iteration, Set<Node> visited) {
        visited.add(iteration);
        this.head = Node.choosePrimary(head, iteration);
        Set<Node> adjacents = origin.adjacencyTree.get(iteration);
        if(adjacents == null || adjacents.isEmpty()) return;
        this.contents.add(iteration);
        for(Node adjacent : adjacents) {
            if(!visited.contains(adjacent))
                ffr(origin, adjacent, visited);
        }
    }

    /**
     * Applies the contents if this cluster to the provided
     * NodeUnionSet. This method will patch <code>target</code>'s
     * transitivity tree and relations map so that it matches
     * the contents of this cluster. <p>
     * Once this method has completed its primary operation, this
     * NodalCluster instance will be {@link #dispose disposed}
     * and cannot be reused unless a call is made to {@link #set}.
     * @param target The NodeUnionSet that this cluster should apply patches to
     */
    public void patch(NodeUnionSet target) {
        assertNotDisposed();
        if(head == null)
            throw new IllegalStateException("Couldn't patch union transitives from a NodalCluster with no head node!");
        forEach(node -> { target.relations.put(node, head); });
        target.relations.put(head, head);
        target.transitiveTree.put(head, contents);
        dispose();
    }

    public boolean contains(Node n) {
        assertNotDisposed();
        return n == head || contents.contains(n);
    }

    public @Nullable Node head() {
        assertNotDisposed();
        return head;
    }

    public Set<Node> contents() {
        assertNotDisposed();
        return contents;
    }

    public int size() {
        return (contents == null ? 0 : contents.size()) + (head == null ? 0 : 1);
    }

    public boolean isStub() {
        return contents != null && contents.size() <= 1;
    }

    public boolean isEmpty() {
        return head == null && (contents == null || contents.isEmpty());
    }

    public void forEach(Consumer<Node> cons) {
        assertNotDisposed();
        cons.accept(head);
        contents.forEach(cons);
    }

    public void trim() {
        assertNotDisposed();
        if(this.head != null && !this.contents.isEmpty())
            this.contents.remove(this.head);
        contents.trim();
    }

    @Override
    public void dispose() {
        this.head = null;
        this.contents = null;
    }

    @Override
    public boolean hasBeenDisposed() {
        return head == null && contents == null;
    }

    public static String asString(List<NodalCluster> clusters) {
        Objects.requireNonNull(clusters);
        if(clusters.size() == 1) 
            return clusters.get(0).toString();
        String out = "";
        for(int x = 0; x < clusters.size(); x++)
            out += "-- Cluster " + x + ":\n" + clusters.get(x).toString() + "\n";
        return out;
    }

    @Override
    public String toString() {
        String out = head == null ? "null head" : head.getComponentID();
        if(contents == null || contents.size() <= 0) return out += "\n - Empty";
        for(Node node : contents)
            out += "\n - " + node.getComponentID();
        return out;
    }
}
