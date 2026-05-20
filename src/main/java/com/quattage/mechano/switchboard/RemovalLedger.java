package com.quattage.mechano.switchboard;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.Disposable;
import com.quattage.mechano.grid.Netlist;
import com.quattage.mechano.grid.ServerGrid;
import com.quattage.mechano.grid.topology.core.MutableComponentReference;
import com.quattage.mechano.grid.topology.core.Node;
import com.quattage.mechano.grid.topology.core.NodePair;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class RemovalLedger implements Disposable {

    private Int2ObjectOpenHashMap<RemovalEntry> removals;

    public RemovalLedger() {
        removals = new Int2ObjectOpenHashMap<>();
    }

    public void mark(ServerGrid grid, MutableComponentReference audit) {
        assertNotDisposed();
        Objects.requireNonNull(audit);
        if(audit.isNode()) mark(grid, audit.asNode());
        else if(audit.isNodePair()) mark(grid, audit.asNodePair());
        throw new IllegalArgumentException("Can't mark " + grid.describe(audit) + " in removal ledger, since this reference's type is not allowed.");
    }

    public void mark(ServerGrid grid, Node node) {
        assertNotDisposed();
        Objects.requireNonNull(node);
        int idx = grid.indexOf(node);
        if(idx < 0) return;
        RemovalEntry at = removals.get(idx);
        if(at == null) {
            at = new RemovalEntry();
            removals.put(idx, at);
        }
        at.markNode(node);
        return;
    }

    public void mark(ServerGrid grid, NodePair link) {
        assertNotDisposed();
        Objects.requireNonNull(link);
        int idx = grid.indexOf(link);
        if(idx < 0) return;
        RemovalEntry at = removals.get(idx);
        if(at == null) {
            at = new RemovalEntry();
            removals.put(idx, at);
        }
        at.markUnion(link);
        return;
    }

    @Override
    public void dispose() {
        assertNotDisposed();
        removals = null;
    }

    @Override
    public boolean hasBeenDisposed() {
        return removals == null;
    }


    public void forEach(BiConsumer<Integer, RemovalEntry> cons) {
        Objects.requireNonNull(cons);
        for(Map.Entry<Integer, RemovalEntry> entry : removals.int2ObjectEntrySet())
            cons.accept(entry.getKey(), entry.getValue());
    }

    public static class RemovalEntry implements Disposable {

        private Set<Node> nodes;
        private Set<NodePair> unions;

        public static RemovalEntry of(@Nullable Set<Node> nodes) {
            return new RemovalEntry(nodes == null ? new HashSet<>() : nodes, new HashSet<>());
        }

        public static RemovalEntry of(@Nullable Set<Node> nodes, @Nullable Set<NodePair> unions) {
            return new RemovalEntry(nodes == null ? new HashSet<>() : nodes, unions == null ? new HashSet<>() : unions);
        }

        public static RemovalEntry of(@Nullable Collection<Node> nodesToRemove) {
            Set<Node> nodes = new HashSet<>();
            if(nodesToRemove != null) nodes.addAll(nodesToRemove);
            return new RemovalEntry(nodes, new HashSet<>());
        }

        public static RemovalEntry of(@Nullable Collection<Node> nodesToRemove, @Nullable Collection<NodePair> unionsToRemove) {
            Set<Node> nodes = new HashSet<>();
            if(nodesToRemove != null) nodes.addAll(nodesToRemove);
            Set<NodePair> unions = new HashSet<>();
            if(unionsToRemove != null) unions.addAll(unionsToRemove);
            return new RemovalEntry(nodes, unions);
        }

        public RemovalEntry() {
            this.nodes = new HashSet<>();
            this.unions = new HashSet<>();
        }

        private RemovalEntry(Set<Node> nodes, Set<NodePair> unions) {
            this.nodes = nodes;
            this.unions = unions;
        }

        public void markNode(Node node) {
            assertNotDisposed();
            Objects.requireNonNull(node);
            nodes.add(node);
        }

        public void markUnion(NodePair link) {
            assertNotDisposed();
            Objects.requireNonNull(link);
            unions.add(link);
        }

        public void markAllNodes(Collection<Node> nodes) {
            Objects.requireNonNull(nodes);
            this.nodes.addAll(nodes);
        }

        public void markAllUnions(Collection<NodePair> unions) {
            Objects.requireNonNull(unions);
            this.unions.addAll(unions);
        }

        public boolean containsNodes() {
            return nodes != null && !nodes.isEmpty();
        }

        public int nodeCount() {
            return nodes == null ? 0 : nodes.size();
        }

        public int unionCount() {
            return unions == null ? 0 : unions.size();
        }

        public boolean contains(Node node) {
            if(!containsNodes() || node == null) return false;
            return nodes.contains(node);
        }

        public void forEachNode(Consumer<Node> cons) {
            if(!containsNodes()) return;
            for(Node n : nodes)
                cons.accept(n);
        }

        public void forEachUnion(Consumer<NodePair> cons) {
            if(!containsUnions()) return;
            for(NodePair np : unions)
                cons.accept(np);
        }

        public void forEachAffected(Consumer<Node> cons) {
            forEachNode(cons);
            if(!containsUnions()) return;
            for(NodePair np : unions) {
                cons.accept(np.getNodeA());
                cons.accept(np.getNodeB());
            }
        }

        public boolean containsUnions() {
            return unions != null && !unions.isEmpty();
        }

        public boolean isEmpty() {
            return !containsNodes() && !containsUnions();
        }

        @Override
        public void dispose() {
            nodes = null;
            unions = null;
        }

        @Override
        public boolean hasBeenDisposed() {
            return unions == null || nodes == null;
        }

        public boolean process(ServerGrid grid, Netlist netlist) {
            return false;
            // if(hasBeenDisposed() || isEmpty()) {
            //     grid.warn("Skipped processing an empty removal entry");
            //     Disposable.disposeOf(this);
            //     return false;
            // }
            // int preSize = netlist.netlist().size();

            // StringBuilder removalManifest = new StringBuilder();
            // netlist.netlist().massRemove(grid, this, removalManifest);
            // // grid.warn("::::\n" + removalManifest);
            
            // ActionRunner runner = grid.initiateTask(GridAction.TASK_LINK_DESTROY);
            // for(NodePair pair : unions) deleteLink(netlist, runner, pair);
            // for(Node node : nodes) deleteNode(netlist, node);
            // if(netlist.netlist().size() > preSize) {
            //     throw new IllegalStateException("netlist grew in size after removal (" + preSize + " -> " 
            //         + netlist.netlist().size() + ") (how the hell did this happen lmao)");
            // }
            // Disposable.disposeOf(this);
            // return netlist.netlist().size() != preSize;
        }


        @Override
        public String toString() {
            if(hasBeenDisposed()) return "RemovalEntry[Disposed]";
            String out = "RemovalEntry[\n  > Nodes:\n";
            if(nodes == null || nodes.isEmpty())
                out += "    - n/a\n";
            else {
                for(Node n : nodes)
                    out += "    - " + n.getComponentID() + " @" + n.hashCode() + "\n";
            }
            out += "  > Links:\n";
            if(unions == null || unions.isEmpty())
                out += "    - n/a\n";
            else {
                for(NodePair np : unions){
                    out += "    (" + np.getNodeA().getComponentID() + " @" + np.getNodeA().hashCode() + " <-> ";
                    out += np.getNodeB().getComponentID() + " @" + np.getNodeB().hashCode() + ")\n";
                }
            }
            return out + "]";
        }
    }
}

