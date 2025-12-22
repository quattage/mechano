package com.quattage.mechano.api.grid.solver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import com.quattage.mechano.api.grid.topology.vertex.Node;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class NodeUnionSet {

    private Object2ObjectOpenHashMap<Node, Node> relations;
    private Object2IntOpenHashMap<Node> ranks;
    private ObjectOpenHashSet<Node> roots;

    public NodeUnionSet() {
        relations = new Object2ObjectOpenHashMap<>();
        ranks = new Object2IntOpenHashMap<>();
        roots = new ObjectOpenHashSet<>();
    }

    public Node find(Node n) {
        Node p = relations.get(n);
        if(n == p) return n;
        Node root = find(p);
        relations.put(n, root);
        return root;
    }

    public void union(Node a, Node b) {
        add(a); add(b);
        Node aP = find(a);
        Node bP = find(b);
        if(aP.equals(bP)) return;
        int rankA = ranks.getInt(aP);
        int rankB = ranks.getInt(bP);
        if(a.isGrounded() && !b.isGrounded()) {
            relations.put(bP, aP);
            roots.remove(bP);
            return;
        }
        if(b.isGrounded() && !a.isGrounded()) {
            relations.put(aP, bP);
            roots.remove(aP);
            return;
        }
        if(rankA < rankB) {
            relations.put(aP, bP);
            roots.remove(aP);
            return;
        }
        if(rankA > rankB) {
            relations.put(bP, aP);
            roots.remove(bP);
            return;
        }
        relations.put(bP, aP); 
        ranks.put(aP, rankA + 1);
        roots.remove(bP);
    }

    public void assignIndices() {
        int index = 0;
        for(Node node : roots)
            node.setNodalIndex(node.isGrounded() ? -1 : index++);
        for(Node node : relations.keySet()) {
            Node root = find(node);
            node.setNodalIndex(root.getNodalIndex());
        }
    }

    public boolean add(Node n) {
        Objects.requireNonNull(n);
        if(relations.containsKey(n)) return false;
        relations.put(n, n);
        ranks.put(n, n.isGrounded() ? -1 : 0);
        roots.add(n);
        return true;
    }


    public void remove(Node n) {
        Objects.requireNonNull(n);
        relations.remove(n);
        ranks.removeInt(n);
    }

    public int uniqueCount() {
        return ranks.size();
    }

    public int rootCount() {
        return roots.size();
    }

    public void forEachRoot(Consumer<Node> cons) {
        roots.forEach(cons);
    }

    public Set<Node> allRoots() {
        return roots;
    }

    /**
     * Get a list of every node that has been folded under
     * the ownership of <code>root</code>.
     * Do not use this except for rare circumstances where
     * no other alternative is feasible, since this method
     * requires iteration of the entire tree.
     * @param root
     * @return A list of nodes. Will never be null.
     */
    public List<Node> getAllChildren(Node root) {
        if(!roots.contains(root)) return Collections.emptyList();
        List<Node> result = new ArrayList<>();
        for(Node node : relations.keySet()) {
            if(node.equals(root)) continue;
            if(find(node) == root) result.add(node);
        }
        return result;
    }

    public String getRootsAsString() {
        String out = "";
        for(Node node : roots) {
            out += "\t" + node.getNodalIndex() + ": " + node + "\n";
        }
        return out.substring(0, out.length() - 2);
    }

    public void reset() {
        relations = new Object2ObjectOpenHashMap<>();
        ranks = new Object2IntOpenHashMap<>();
        roots = new ObjectOpenHashSet<>();
    }
}
