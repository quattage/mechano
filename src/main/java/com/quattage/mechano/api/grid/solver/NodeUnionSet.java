package com.quattage.mechano.api.grid.solver;

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
        if(p == null) return null;
        if(!p.equals(n)) relations.put(n, find(p));
        return p;
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
            node.setIndex(node.isGrounded() ? -1 : index++);
    }

    public boolean add(Node n) {
        if(relations.containsKey(n)) return false;
        relations.put(n, n);
        ranks.put(n, n.isGrounded() ? -1 : 0);
        roots.add(n);
        return true;
    }

    public void remove(Node n) {
        relations.remove(n);
        ranks.removeInt(n);
    }

    public int rootCount() {
        return roots.size();
    }

    public int uniqueCount() {
        return ranks.size();
    }

    public void forEachRoot(Consumer<Node> cons) {
        roots.forEach(cons);
    }

    public void reset() {
        relations = new Object2ObjectOpenHashMap<>();
        ranks = new Object2IntOpenHashMap<>();
        roots = new ObjectOpenHashSet<>();
    }
}
