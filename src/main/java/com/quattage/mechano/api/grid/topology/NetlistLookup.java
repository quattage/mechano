package com.quattage.mechano.api.grid.topology;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.grid.GridTracking;
import com.quattage.mechano.api.grid.GridUUID;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.topology.landmark.AncillaryNode;
import com.quattage.mechano.api.grid.topology.landmark.Node;
import com.quattage.mechano.api.grid.topology.landmark.link.AncillaryPair;
import com.quattage.mechano.api.switchboard.action.GridAction;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.world.level.ChunkPos;

/**
 * Grants API users the ability to look up {@link AncillaryPair links}
 */
public abstract class NetlistLookup<T> {

    protected Object2ObjectOpenHashMap<T, List<AncillaryPair>> links;

    public NetlistLookup() {}

    public abstract GridAction add(Grid grid, AncillaryPair link);
    public abstract GridAction remove(Grid grid, AncillaryPair link);
    public abstract Stream<AncillaryPair> byChunk(ChunkPos pos);
    public abstract void reset();

    public GridAction removeAsymmetric(@Nullable Grid grid, T start, T end) {
        List<AncillaryPair> linksAt = getLinksBelongingTo(start);
        if(linksAt == null) return GridAction.RESPONSE_FAIL_START_MISSING;
        int toRemove = -1;
        for(int x = 0; x < linksAt.size(); x++) {
            AncillaryPair link = linksAt.get(x);
            if(link != null && link.endsWith(end)) {
                toRemove = x;
                break;
            }
        }
        if(toRemove < 0) return GridAction.RESPONSE_FAIL_END_MISSING;
        AncillaryPair removed = linksAt.remove(toRemove);
        if(removed != null) {
            if(linksAt.isEmpty()) links.remove(start);
            if(grid != null) removed.onRemovedFromGrid(grid);
            return GridAction.RESPONSE_SUCCESS;
        }
        return GridAction.RESPONSE_FAIL_MISSING;
    }

    public @Nullable AncillaryPair popAsymmetric(@Nullable Grid grid, T start, T end) {
        List<AncillaryPair> linksAt = getLinksBelongingTo(start);
        if(linksAt == null) return null;
        int toRemove = -1;
        for(int x = 0; x < linksAt.size(); x++) {
            AncillaryPair link = linksAt.get(x);
            if(link != null && link.endsWith(end)) {
                toRemove = x;
                break;
            }
        }
        if(toRemove < 0) return null;
        AncillaryPair removed = linksAt.remove(toRemove);
        if(removed != null) {
            if(linksAt.isEmpty()) links.remove(start);
            if(grid != null) removed.onRemovedFromGrid(grid);
            return removed;
        }
        return null;
    }

    protected GridAction addAsymmetric(@Nullable Grid grid, T hash, AncillaryPair link, boolean limit) {
        List<AncillaryPair> linksAt = getLinksBelongingTo(hash);
        if(linksAt == null) {
            linksAt = new ArrayList<AncillaryPair>();
            linksAt.add(link);
            if(grid != null) link.onAddedToGrid(grid);
            links.put(hash, linksAt);
            return GridAction.RESPONSE_SUCCESS;
        }
        if(linksAt.contains(link)) return GridAction.RESPONSE_FAIL_DUPLICATE_ELEMENT;
        if(limit && linksAt.size() >= AncillaryNode.MAX_SHARED_OCCUPANCY)
            return GridAction.RESPONSE_FAIL_ELEMENT_FULL;
        if(grid != null) linksAt.add(link);
        link.onAddedToGrid(grid);
        return GridAction.RESPONSE_SUCCESS;
    }

    protected @Nullable AncillaryPair getFrom(List<AncillaryPair> linksAt, T lookup) {
        if(linksAt == null || linksAt.isEmpty()) return null;
        for(int x = 0; x < linksAt.size(); x++) {
            AncillaryPair link = linksAt.get(x);
            if(link.endsWith(lookup)) return link;
        }
        return null;
    }

    public @Nullable List<AncillaryPair> getLinksBelongingTo(T lookup) {
        return links.get(lookup);
    }

    public @Nullable AncillaryPair getLink(T start, T end) {
        return getFrom(getLinksBelongingTo(start), end);
    }

    @Override
    public String toString() {
        if(links.isEmpty()) return "\n\tEmpty";
        String out = "\n";
        for(Map.Entry<T, List<AncillaryPair>> entry : links.entrySet()) {
            out += "\t- " + entry.getKey() + ":\n";
            for(AncillaryPair link : entry.getValue())
                out += "\t\t* " + link + "\n";
            out = out.substring(0, out.length() - 1);
            out += "\n";
        }
        return out;
    }

    public void forEachLink(Consumer<AncillaryPair> cons) {
        if(links.isEmpty()) return;
        for(Map.Entry<T, List<AncillaryPair>> entry : links.entrySet()) {
            List<AncillaryPair> links = entry.getValue();
            if(links == null || links.isEmpty())
                continue;
            for(AncillaryPair link : links)
                if(link != null) cons.accept(link);
        }
    }

    public int size() {
        return links == null ? 0 : links.size();
    }

    public boolean isEmpty() {
        return size() <= 0;
    }



    /**
     * Look up links based on their {@link ChunkPos} and attached {@link Node}
     */
    public static class ServerNetlistLookup extends NetlistLookup<AncillaryNode<?>> {

        private Object2ObjectOpenHashMap<ChunkPos, Set<AncillaryNode<?>>> chunkOccupation; 

        public ServerNetlistLookup() {
            links = new Object2ObjectOpenHashMap<>();
            chunkOccupation = new Object2ObjectOpenHashMap<>();
        }

        @Override
        public GridAction add(Grid grid, AncillaryPair link) {
            GridAction output = super.addAsymmetric(grid, link.getStartAncillary(), link, true);
            if(output.getActionType().indicatesSuccess())
                super.addAsymmetric(grid, link.getEndAncillary(), link.flippedCopy(), true);
            markChunkly(grid, link.getStartAncillary());
            markChunkly(grid, link.getEndAncillary());
            return output;
        }

        @Override
        public GridAction remove(Grid grid, AncillaryPair link) {
            GridAction output = super.removeAsymmetric(grid, link.getStartAncillary(), link.getEndAncillary());
            super.removeAsymmetric(grid, link.getEndAncillary(), link.getStartAncillary());
            tryUnmark(link.getStartAncillary());
            tryUnmark(link.getEndAncillary());
            return output;
        }

        public AncillaryPair pop(Grid grid, AncillaryNode<?> start, AncillaryNode<?> end) {
            AncillaryPair output = super.popAsymmetric(grid, start, end);
            super.removeAsymmetric(grid, end, start);
            tryUnmark(start);
            tryUnmark(end);
            return output;
        }

        public GridAction remove(Grid grid, AncillaryNode<?> start, AncillaryNode<?> end) {
            GridAction output = super.removeAsymmetric(grid, start, end);
            super.removeAsymmetric(grid, end, start);
            tryUnmark(start);
            tryUnmark(end);
            return output;
        }

        private void tryUnmark(AncillaryNode<?> node) {
            List<AncillaryPair> links = getLinksBelongingTo(node);
            if(links == null || links.isEmpty())
                unmarkChunkly(node);
        }

        public @Nullable Set<AncillaryNode<?>> getNodesByChunk(ChunkPos pos) {
            Objects.requireNonNull(pos);
            if(chunkOccupation.isEmpty()) return null;
            return chunkOccupation.get(pos);
        }

        private void markChunkly(@Nullable Grid grid, AncillaryNode<?> node) {
            Griddable<?> source = GridTracking.getReferentOrThrow(node);
            if(source == null && grid != null) {
                grid.warn("Skipped marking " + node + " because its source couldn't be found.");
                return;
            }
            if(source == null || source.canMoveDynamically()) return;
            ChunkPos cp = source.getChunkPos();
            if(cp == null) return;
            Set<AncillaryNode<?>> preexisting = chunkOccupation.get(cp);
            if(preexisting == null) {
                preexisting = new HashSet<>();
                chunkOccupation.put(cp, preexisting);
            }
            preexisting.add(node);
        }

        private void unmarkChunkly(Node node) {
            Griddable<?> source = GridTracking.getReferentOrThrow(node);
            if(source == null || source.canMoveDynamically()) return;
            ChunkPos cp = source.getChunkPos();
            if(cp == null) return;
            Set<AncillaryNode<?>> preexisting = chunkOccupation.get(cp);
            if(preexisting == null) return;
            preexisting.remove(node);
            if(preexisting.isEmpty()) chunkOccupation.remove(cp);
        }

        @Override
        public Stream<AncillaryPair> byChunk(ChunkPos pos) {
            return Optional.ofNullable(chunkOccupation.get(pos))
                .stream()
                .flatMap(Set::stream)
                .flatMap(node -> 
                    Optional.ofNullable(links.get(node))
                        .stream().flatMap(List::stream)
                );
        }

        @Override
        public void reset() {
            links = new Object2ObjectOpenHashMap<>();
            chunkOccupation = new Object2ObjectOpenHashMap<>();
        }

        public ObjectSet<Entry<AncillaryNode<?>, List<AncillaryPair>>> all() {
            return links.entrySet();
        }
    }

    /**
     * Look up links based on their {@link GridUUID uuid}
     */
    public static class ClientNetlistLookup extends NetlistLookup<GridUUID<?>> {

        public ClientNetlistLookup() {
            links = new Object2ObjectOpenHashMap<>();
        }

        @Override
        public GridAction add(Grid grid, AncillaryPair link) {
            GridAction output = super.addAsymmetric(grid, link.getStartID().copyAndClearBindings(), link, false);
            if(output.getActionType().indicatesSuccess())
                super.addAsymmetric(grid, link.getEndID().copyAndClearBindings(), link.flippedCopy(), false);
            return output;
        }

        @Override
        public GridAction remove(Grid grid, AncillaryPair link) {
            super.removeAsymmetric(grid, link.getStartID().copyAndClearBindings(), link.getEndID());
            return super.removeAsymmetric(grid, link.getEndID().copyAndClearBindings(), link.getStartID());
        }

        @Override
        public Stream<AncillaryPair> byChunk(ChunkPos pos) {
            return Stream.empty();
        }
        
        @Override
        public void reset() {
            links = new Object2ObjectOpenHashMap<>();
        }
    }
}
