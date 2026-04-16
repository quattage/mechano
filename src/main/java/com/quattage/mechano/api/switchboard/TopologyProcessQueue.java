package com.quattage.mechano.api.switchboard;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.GridTracking;
import com.quattage.mechano.api.grid.GridUUID;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.topology.GridDomain;
import com.quattage.mechano.api.grid.topology.landmark.AncillaryNode;
import com.quattage.mechano.api.grid.topology.landmark.Node;
import com.quattage.mechano.api.grid.topology.landmark.link.AncillaryPair;
import com.quattage.mechano.api.grid.topology.landmark.link.NodePair;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.api.switchboard.action.GridAction.ActionRunner;
import com.quattage.mechano.foundation.Disposable;
import com.quattage.mechano.foundation.numeric.EsoMath;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.util.RandomSource;

public class TopologyProcessQueue {

    private @Nullable PriorityQueue<TaskWrapper> queue;
    private boolean hasUnsavedChanges = false;

    public void applyTo(ServerGrid grid) {
        if(!containsChanges()) return;
        final RemovalCache removals = new RemovalCache();
        boolean brokeEarly = false;
        TaskWrapper wrapper = null;
        while(!queue.isEmpty()) {
            wrapper = queue.poll();
            if(wrapper.creates()) {
                brokeEarly = true;
                break;
            }
            wrapper.run(grid, removals);
        }
        removals.apply(grid);
        if(brokeEarly) wrapper.run(grid, removals);
        while(!queue.isEmpty()) {
            wrapper = queue.poll();
            wrapper.run(grid, removals);
        }
        clear();
    }

    public void add(@Nullable Grid grid, GridAction task, Object... args) {
        for(Object obj : args) {
            if(Disposable.hasBeenDisposed(obj))
                if(grid != null) {
                    grid.warn("Skipped scheduling of topology restructuring task '" + task.getSerializedName() 
                        + "' - An argument (" + obj.getClass().getSimpleName() + ") was disposed already!");
                }
        }
        if(this.queue == null) this.queue = new PriorityQueue<>(16);
        TaskWrapper toAdd = instantiateTask(grid, task, args);
        if(toAdd == null) return;
        this.queue.add(toAdd);
        this.hasUnsavedChanges = true;
    }

    public @Nullable TaskWrapper instantiateTask(@Nullable Grid grid, GridAction task, Object... args) {
        Objects.requireNonNull(grid);
        Objects.requireNonNull(task);
        if(!task.isTask()) {
            if(grid != null) grid.error("Attempted to queue action '" + task + "' but this action is not a task type.");
            return null;
        }
        if(!task.isWrappable()) {
            if(grid != null) grid.error("Attempted to queue action '" + task + "' but this task cannot be wrapped into the queue.");
            return null;
        }
        return new TaskWrapper(task, queue.size(), args);
    }

    /**
     * Used for testing
     */
    public void addRandom(ServerGrid grid, RandomSource random) {
        Objects.requireNonNull(grid);
        Objects.requireNonNull(random);
        int idx = EsoMath.randomInt(random, 0, 3);
        GridAction task = GridAction.values()[idx];
        add(grid, task, new Object[0]);
    }

    public boolean containsChanges() {
        return !(queue == null || queue.isEmpty());
    }

    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }

    public long size() {
        return !containsChanges() ? 0 : queue.size();
    }

    public void clear() {
        queue = null;
    }

    @Override
    public String toString() {
        String out =  "TopologyProcessQueue:\n";
        out += "  Unsaved changes: " + (hasUnsavedChanges ? "yes" : "no") + "\n";
        out += "  Topology:\n";
        if(!containsChanges()) return out + "    [Empty]";
        PriorityQueue<TaskWrapper> copy = new PriorityQueue<>(queue.size());
        while(!queue.isEmpty()) {
            TaskWrapper head = queue.poll();
            out += "    " + head + "\n";
            copy.add(head);
        }
        this.queue = copy;
        return out;
    }

    private static class TaskWrapper implements Comparable<TaskWrapper> {

        private final GridAction action;
        private final Object[] args;
        private final int index;

        private TaskWrapper(GridAction task, int index, Object[] args) {
            Objects.requireNonNull(task);
            this.action = task;
            this.index = index;
            if(args == null) args = new Object[0];
            this.args = args;
        }

        private GridAction run(ServerGrid grid, RemovalCache removals) {
            if(GridAction.VERBOSE_LOGS) grid.debug("Initiating wrapped task (" + this.action.getTask().getClass().getSimpleName() + ")");
            GridAction output = this.action.getTask().executeTopological(grid, removals, args);
            return output == null ? GridAction.NONE : output;
        }

        public boolean creates() {
            return action == GridAction.TASK_LINK_CREATE;
        }

        private int getPriority() {
            return action.ordinal();
        }

        @Override
        public int compareTo(TaskWrapper that) {
            int priorityCompare = Integer.compare(this.getPriority(), that.getPriority());
            if(priorityCompare != 0) return priorityCompare;
            return Integer.compare(this.index, that.index);
        }

        @Override
        public String toString() {
            return action.getTask().getClass().getSimpleName() + " (index " + index + "), " + action.getTask().collectArgsAsString(args);
        }
    }

    public static class RemovalCache implements Disposable {

        private Int2ObjectOpenHashMap<Removal> removals;

        public RemovalCache() {
            removals = new Int2ObjectOpenHashMap<>();
        }

        public void mark(Node node) {
            Objects.requireNonNull(node);
            if(hasBeenDisposed()) {
                Mechano.LOGGER.warn("Skipped adding " + node + " to removal holder that has already been disposed.");
                return;
            }
            int idx = node.getDomainIndex();
            if(idx < 0) return;
            Removal at = removals.get(idx);
            if(at == null) {
                at = new Removal();
                removals.put(idx, at);
            }
            at.mark(node);
            return;
        }

        public void mark(NodePair link) {
            assertNotDisposed();
            Objects.requireNonNull(link);
            int idx = link.getNodeA().getDomainIndex();
            if(idx < 0) return;
            Removal at = removals.get(idx);
            if(at == null) {
                at = new Removal();
                removals.put(idx, at);
            }
            at.mark(link);
            return;
        }

        public void apply(ServerGrid grid) {
            assertNotDisposed();
            final Int2ObjectOpenHashMap<GridDomain> toReduce = new Int2ObjectOpenHashMap<>(removals.size());
            for(Map.Entry<Integer, Removal> entry : removals.int2ObjectEntrySet()) {
                GridDomain domain = grid.getDomain(entry.getKey());
                Removal removal = entry.getValue();
                if(removal.isEmpty()) continue;
                boolean wasReduced = removeBatch(grid, domain, removal.nodes, removal.links);
                if(!wasReduced) continue;
                domain.markDirty();
                toReduce.put((int)entry.getKey(), domain);
            }
            for(Map.Entry<Integer, GridDomain> entry : toReduce.int2ObjectEntrySet()) 
                reduceSingleDomain(grid, entry.getValue(), entry.getKey());
            dispose();
        }

        private boolean removeBatch(ServerGrid grid, GridDomain domain, @Nullable Set<Node> removedNodes, @Nullable Set<NodePair> removedLinks) {
            int preSize = domain.netlist().size();
            domain.netlist().massRemove(grid, removedNodes, removedLinks);
            ActionRunner runner = grid.initiateTask(GridAction.TASK_LINK_DESTROY);
            for(NodePair pair : removedLinks)
                deleteLink(grid, domain, runner, pair);
            for(Node node : removedNodes)
                deleteNode(grid, domain, node);
            if(preSize > domain.netlist().size()) throw new IllegalStateException("netlist grew in size after removal (" + preSize + " -> " + domain.netlist().size() + ")");
            return preSize < domain.netlist().size();
        }

        private void deleteLink(ServerGrid grid, GridDomain domain, ActionRunner runner, NodePair pair) {
            // if the pair is a link it is deleted straight away
            if(pair instanceof AncillaryPair link) {
                grid.lookup().remove(grid, link);
                link.MNADeallocate(grid, domain);
                AncillaryNode<?> start = link.getStartAncillary(), end = link.getEndAncillary();
                Griddable<?> ss = GridTracking.getReferentOrThrow(start), es = GridTracking.getReferentOrThrow(end);
                runner.targeting(ss, es)
                    .withArguments(
                        GridTracking.getAddress(ss, start), 
                        GridTracking.getAddress(es, end)
                    ).executeOnClients();
                if(link instanceof Disposable dp)
                    dp.dispose();
                return;
            }
            // if the pair isnt a link the closest match is searched for
            List<AncillaryNode<?>> aAnc = pair.getNodeA().getAncillaries();
            List<AncillaryNode<?>> bAnc = pair.getNodeB().getAncillaries();
            if(aAnc == null || aAnc.isEmpty()) return;
            if(bAnc == null || bAnc.isEmpty()) return;
            for(AncillaryNode<?> an : aAnc) {
                Griddable<?> aSource = GridTracking.getReferentOrThrow(an);
                GridUUID<?> aID = GridTracking.getAddress(aSource, an);
                for(AncillaryNode<?> bn : bAnc) {
                    AncillaryPair removed = grid.lookup().pop(grid, an, bn);
                    if(removed == null) continue;
                    removed.MNADeallocate(grid, domain);
                    Griddable<?> bSource = GridTracking.getReferentOrThrow(bn);
                    GridUUID<?> bID = GridTracking.getAddress(bSource, bn);
                    runner.targeting(aSource, bSource)
                        .withArguments(aID, bID)
                        .executeOnClients();
                    if(removed instanceof Disposable dp)
                        dp.dispose();
                }
            }
        }

        private void deleteNode(ServerGrid grid, GridDomain domain, Node node) {
            node.forEachTerminal(terminal -> {
                terminal.MNADeallocate(grid, domain);
                terminal.dispose();
            });
            node.MNADeallocate(grid, domain);
            node.dispose();
        }

        private void reduceSingleDomain(ServerGrid grid, GridDomain domain, int idx) {
            List<GridDomain> reduceResult = domain.deriveFromSplits(grid.getWorld());
            domain.clear();
            if(reduceResult.size() == 1) {
                grid.domains().set(idx, reduceResult.get(1));
                return;
            }
            if(reduceResult.size() > 1) {
                grid.domains().set(idx, reduceResult.get(1));
                for(int x = 1; x < reduceResult.size(); x++) {
                    GridDomain reducedDomain = reduceResult.get(x);
                    grid.domains().add(reducedDomain);
                }
            }
            grid.domains().remove(idx);
            // if the domain was removed, all subsequent domains have their nodal indices updated
            for(int x = idx; idx < grid.domains().size(); x++)
                grid.domains().get(x).markDirty();
        }


        @Override
        public void dispose() {
            removals = null;
        }

        @Override
        public boolean hasBeenDisposed() {
            return removals == null;
        }

    }

    private static class Removal {

        private Set<Node> nodes = null;
        private Set<NodePair> links = null;

        private void mark(Node node) {
            if(nodes == null) nodes = new HashSet<>();
            nodes.add(node);
        }

        private void mark(NodePair link) {
            if(links == null) links = new HashSet<>();
            links.add(link);
        }

        private boolean isEmpty() {
            return (nodes == null || nodes.isEmpty()) && (links == null || links.isEmpty());
        }
    }
}