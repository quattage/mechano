package com.quattage.mechano.api.grid.solver;

import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.api.grid.topology.vertex.Terminal;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class NodeUnionSet {

    private Object2ObjectOpenHashMap<Node, Node> relations;
    private Object2IntOpenHashMap<Node> rank;

    /**
     * Transfers the contents of <code>nodeB</code> onto
     * <code>nodeA</code> and disposes of <code>nodeB</code>
     * <p>
     * Subsequent access to a node that has been disposed of
     * will throw errors.
     * @param nodeA
     * @param nodeB
     * @return
     */
    public static Node collapse(Node nodeA, Node nodeB) {
        if(nodeA == nodeB) return nodeA;
        for(Terminal term : nodeB.getTerminals()) {
            term.setConnectedTo(nodeA);
            if(term.getJoint() == nodeB) continue;
            nodeA.getTerminals().add(term);
        }
        nodeB.dispose();
        return nodeA;
    }

    public NodeUnionSet() {
        relations = new Object2ObjectOpenHashMap<>();
        rank = new Object2IntOpenHashMap<>();
    }

    public void add(Node n) {
        relations.putIfAbsent(n, n);
        rank.putIfAbsent(n, 0);
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
        int rankA = rank.getInt(aP);
        int rankB = rank.getInt(bP);
        if(rankA < rankB) relations.put(aP, bP);
        else if(rankA > rankB) relations.put(bP, aP);
        else {
            relations.put(bP, aP); 
            rank.put(aP, rankA + 1);
        }   
    }

    public void remove(Node n) {
        relations.remove(n);
        rank.removeInt(n);
    }

    public void reset() {
        relations = new Object2ObjectOpenHashMap<>();
        rank = new Object2IntOpenHashMap<>();
    }
}
