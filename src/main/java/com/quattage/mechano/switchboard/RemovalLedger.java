package com.quattage.mechano.switchboard;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.GridDomain;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.foundation.Disposable;
import com.quattage.mechano.grid.GridTracking;
import com.quattage.mechano.grid.GridUUID;
import com.quattage.mechano.grid.Griddable;
import com.quattage.mechano.grid.solver.ConvergenceStatus;
import com.quattage.mechano.grid.topology.AncillaryNode;
import com.quattage.mechano.grid.topology.Node;
import com.quattage.mechano.grid.topology.link.AncillaryPair;
import com.quattage.mechano.grid.topology.link.NodePair;
import com.quattage.mechano.switchboard.action.GridAction;
import com.quattage.mechano.switchboard.action.GridAction.ActionRunner;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class RemovalLedger implements Disposable {

    private Int2ObjectOpenHashMap<RemovalEntry> removals;

    public RemovalLedger() {
        removals = new Int2ObjectOpenHashMap<>();
    }

    public void mark(Node node) {
        assertNotDisposed();
        Objects.requireNonNull(node);
        int idx = node.getDomainIndex();
        if(idx < 0) return;
        RemovalEntry at = removals.get(idx);
        if(at == null) {
            at = new RemovalEntry();
            removals.put(idx, at);
        }
        at.markNode(node);
        return;
    }

    public void mark(NodePair link) {
        assertNotDisposed();
        Objects.requireNonNull(link);
        int idx = link.getNodeA().getDomainIndex();
        if(idx < 0) return;
        RemovalEntry at = removals.get(idx);
        if(at == null) {
            at = new RemovalEntry();
            removals.put(idx, at);
        }
        at.markUnion(link);
        return;
    }

    public void apply(ServerGrid grid) {
        assertNotDisposed();
        final Int2ObjectOpenHashMap<GridDomain> toReduce = new Int2ObjectOpenHashMap<>(removals.size());
        for(Map.Entry<Integer, RemovalEntry> entry : removals.int2ObjectEntrySet()) {
            GridDomain domain = grid.getDomain(entry.getKey());
            boolean wasReduced = entry.getValue().process(grid, domain);
            if(!wasReduced) continue;
            domain.solver().setStatus(ConvergenceStatus.CHANGES_QUEUED);
            toReduce.put((int)entry.getKey(), domain);
        }
        for(Map.Entry<Integer, GridDomain> entry : toReduce.int2ObjectEntrySet()) {
            reduceSingleDomain(grid, entry.getValue(), entry.getKey());
        }
        Disposable.disposeOf(this);
    }

    private void reduceSingleDomain(ServerGrid grid, GridDomain domain, int idx) {
        List<GridDomain> reduceResult = domain.deriveFromSplits();
        int clusterCount = reduceResult.size();
        if(clusterCount == 1) {
            GridDomain newResult = reduceResult.get(0);
            grid.domains().set(idx, newResult);
            if(newResult != domain) Disposable.disposeOf(domain);
            return;
        }
        if(clusterCount > 1) {
            grid.domains().set(idx, reduceResult.get(0));
            for(int x = 1; x < reduceResult.size(); x++) {
                GridDomain reducedDomain = reduceResult.get(x);
                if(reducedDomain.netlist().isEmpty())
                    throw new IllegalStateException("Attempted to add a reduced domain containing an empty netlist");
                reducedDomain.solver().setStatus(ConvergenceStatus.CHANGES_QUEUED);
                int destination = idx + x;
                if(destination < grid.domains().size())
                    grid.domains().add(destination, reducedDomain);
                else grid.domains().add(reducedDomain);
            }
        }
        else {
            GridDomain removed = grid.domains().remove(idx);
            Disposable.disposeOf(removed);
        }
    }

    @Override
    public void dispose() {
        assertNotDisposed();
        for(RemovalEntry entry : removals.values())
            entry.dispose();
        removals = null;
    }

    @Override
    public boolean hasBeenDisposed() {
        return removals == null;
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

        public boolean process(ServerGrid grid, GridDomain domain) {
            if(hasBeenDisposed() || isEmpty()) {
                grid.warn("Skipped processing an empty removal entry");
                Disposable.disposeOf(this);
                return false;
            }
            int preSize = domain.netlist().size();

            StringBuilder removalManifest = new StringBuilder();
            domain.netlist().massRemove(grid, this, removalManifest);
            // grid.warn("::::\n" + removalManifest);
            
            ActionRunner runner = grid.initiateTask(GridAction.TASK_LINK_DESTROY);
            for(NodePair pair : unions) deleteLink(domain, runner, pair);
            for(Node node : nodes) deleteNode(domain, node);
            if(domain.netlist().size() > preSize) {
                throw new IllegalStateException("netlist grew in size after removal (" + preSize + " -> " 
                    + domain.netlist().size() + ") (how the hell did this happen lmao)");
            }
            Disposable.disposeOf(this);
            return domain.netlist().size() != preSize;
        }

        private void deleteLink(GridDomain domain, ActionRunner runner, NodePair pair) {
            // if the pair is a link it is deleted straight away
            if(pair instanceof AncillaryPair link) {
                GridAction removal = domain.getHostGrid().remove(domain.getHostGrid(), link);
                link.MNADeallocate(domain);
                AncillaryNode<?> start = link.getStartAncillary(), end = link.getEndAncillary();
                start.setDomainIndex(-2);
                end.setDomainIndex(-2);
                Griddable<?> ss = GridTracking.getReferentOrThrow(start), es = GridTracking.getReferentOrThrow(end);
                runner.targeting(ss, es)
                    .withArguments(
                        GridTracking.getAddress(ss, start), 
                        GridTracking.getAddress(es, end)
                    ).executeOnClients();
                if(!removal.getActionType().indicatesSuccess()) {
                    domain.getHostGrid().error("Failed to delete link via direct acquisition (" + pair 
                        + ") - No active ancillary pair sharing these mappings could be located in the grid.");
                }
                Disposable.disposeOf(link);
                return;
            }
            // if the pair isnt a link the closest match is searched for
            List<AncillaryNode<?>> aAnc = pair.getNodeA().getAncillaries();
            List<AncillaryNode<?>> bAnc = pair.getNodeB().getAncillaries();
            if(aAnc == null || aAnc.isEmpty()) {
                domain.getHostGrid().error("Skipped deleting link " + pair + " - The starting node in this pair had no ancillaries to search from.");
                return;
            }
            if(bAnc == null || bAnc.isEmpty()) {
                domain.getHostGrid().error("Skipped deleting link " + pair + " - The ending node in this pair had no ancillaries to search from.");
                return;
            }
            for(AncillaryNode<?> an : aAnc) {
                Griddable<?> aSource = GridTracking.getReferentOrThrow(an);
                GridUUID<?> aID = GridTracking.getAddress(aSource, an);
                for(AncillaryNode<?> bn : bAnc) {
                    AncillaryPair removed = domain.getHostGrid().lookup().pop(domain.getHostGrid(), an, bn);
                    if(removed == null) continue;
                    removed.MNADeallocate(domain);
                    removed.getStartAncillary().setDomainIndex(-2);
                    removed.getEndAncillary().setDomainIndex(-2);
                    Griddable<?> bSource = GridTracking.getReferentOrThrow(bn);
                    GridUUID<?> bID = GridTracking.getAddress(bSource, bn);
                    runner.targeting(aSource, bSource)
                        .withArguments(aID, bID)
                        .executeOnClients();
                    Disposable.disposeOf(removed);
                }
            }
        }

        private void deleteNode(GridDomain domain, Node node) {
            if(node instanceof AncillaryNode anc) 
                domain.getHostGrid().removeLinkDeferred(anc);
            node.forEachTerminal(terminal -> {
                terminal.MNADeallocate(domain);
                Disposable.disposeOf(terminal);
            });
            node.MNADeallocate(domain);
            domain.indexer().remove(node);
            Disposable.disposeOf(node);
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

