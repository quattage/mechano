package com.quattage.mechano.api.anchor;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.griddable.Griddable;
import com.quattage.mechano.api.identifier.GridUUID;
import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface AnchorCollection extends Iterable<AnchorPoint> {

    public static AnchorArray asEmpty() {
        return new AnchorArray(new AnchorPoint[0]);
    }

    public static AnchorArray asSingle(AnchorPoint single) {
        return new AnchorArray(new AnchorPoint[] { single });
    }

    public static DynamicAnchorArray asDynamic() {
        return new DynamicAnchorArray(GridUUID.MAX_SHARED_OCCUPANCY);
    }
    
    abstract @NotNull AnchorPoint get(int index);
    abstract @Nullable AnchorPoint get(GridUUID addr);
    abstract int size();
    abstract void add(AnchorPoint anchor);
    abstract AnchorCollection bindTo(Griddable<?> host);

    default boolean checkFullOrWarn() {
        if(size() >= GridUUID.MAX_SHARED_OCCUPANCY) {
            Mechano.LOGGER.warn("Skipped adding AnchorPoint at index " + GridUUID.MAX_SHARED_OCCUPANCY  + " to dynamic array - This collection is already full!");
            return true;
        }
        return false;
    }

    default boolean isEmpty() {
        return size() <= 0;
    }

    default void updateOrientations(BlockState state) {
        CombinedOrientation dir = DirectionTransformer.extract(state);
        forEach(ap -> { ap.updateOrientation(dir); });
    }
    
    static class BreakoutException extends RuntimeException {}
    default boolean hasAnyConnections() {
        // this sucks lol
        try {
            forEach(ap -> {
                if(ap.getCurrentConnections() > 0);
                    throw new BreakoutException();
            });
        } catch(BreakoutException e) { return true; }
        return false;
    }


    default boolean contains(GridUUID addr) {
        if(addr == null) return false;
        if(addr.getIndex() < 0 || addr.getIndex() > GridUUID.MAX_SHARED_OCCUPANCY) return false;
        AnchorPoint anchor = get(addr.getIndex());
        if(anchor == null) return false;
        return addr.equals(anchor.getAddress());
    }

    @OnlyIn(Dist.CLIENT)
    public static class AnchorArray implements AnchorCollection {

        private AnchorPoint[] anchors;

        public AnchorArray(AnchorPoint[] anchors) {
            this.anchors = anchors;
        }

        @Override
        public Iterator<AnchorPoint> iterator() {
            return Arrays.stream(anchors).iterator();
        }

        @Override
        public @NotNull AnchorPoint get(int index) {
            if(index < 0 || index >= anchors.length)
                throw new ArrayIndexOutOfBoundsException("Index " + index + " is out of bounds for anchor array of length " + anchors.length);
            return anchors[index];
        }

        @Override
        public @Nullable AnchorPoint get(GridUUID addr) {
            Objects.requireNonNull(addr);
            return get(addr.getIndex());
        }

        @Override
        public int size() {
            return anchors.length;
        }

        @Override
        public void add(AnchorPoint anchor) {
            if(checkFullOrWarn()) return;
            AnchorPoint[] copy = new AnchorPoint[anchors.length + 1];
            System.arraycopy(anchors, 0, copy, 0, anchors.length);
            copy[anchors.length] = anchor;
            this.anchors = copy;
        }

        @Override
        public String toString() {
            if(anchors.length == 0) return "AnchorPoints[\n\tEMPTY\n]";
            String output = "AnchorPoints[\n";
            for(int x = 0; x < anchors.length; x++)
                output += anchors[x] == null ? "\tnull,\n" : ("\t" + anchors[x].toString() + ", \n");
            return output + "]";
        }

        @Override
        public AnchorCollection bindTo(Griddable<?> host) {
            GridUUID newAddr = host.getSurrogate().getOrCreateAddress();
            for(int x = 0; x < anchors.length; x++) {
                AnchorPoint ap = anchors[x];
                ap.replaceAddress(newAddr.indexedCopy(x));
            }
            return this;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class DynamicAnchorArray implements AnchorCollection {

        private final ObjectArrayList<AnchorPoint> contents;

        public DynamicAnchorArray(int size) {
            this.contents = new ObjectArrayList<>(size);
        }

        @Override
        public Iterator<AnchorPoint> iterator() {
            return contents.iterator();
        }

        @Override
        public @NotNull AnchorPoint get(int index) {
            return contents.get(index);
        }

        @Override
        public @Nullable AnchorPoint get(GridUUID addr) {
            Objects.requireNonNull(addr);
            return get(addr.getIndex());
        }

        @Override
        public int size() {
            return contents.size();
        }

        @Override
        public void add(AnchorPoint anchor) {
            if(checkFullOrWarn()) return;
            contents.add(anchor);
        }

        public AnchorPoint.Builder<DynamicAnchorArray> newAnchor() {
            return new AnchorPoint.Builder<DynamicAnchorArray>(this);
        }

        @Override
        public AnchorCollection bindTo(Griddable<?> host) {
            GridUUID newAddr = host.getSurrogate().getOrCreateAddress();
            for(int x = 0; x < contents.size(); x++) {
                AnchorPoint ap = contents.get(x);
                ap.replaceAddress(newAddr.indexedCopy(x));
            }
            return this;
        }

        public AnchorArray toArray() {
            return new AnchorArray(contents.toArray(new AnchorPoint[contents.size()]));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class AliasedAnchorMap implements AnchorCollection {

        private final Object2ObjectOpenHashMap<GridUUID, AnchorArray> aliases;

        public AliasedAnchorMap(int preload) {
            this.aliases = new Object2ObjectOpenHashMap<>(preload);
        }

        @Override
        public @NotNull AnchorPoint get(int index) {
            Optional<AnchorArray> first = aliases.values().stream().findFirst();
            Mechano.LOGGER.warn("Tried to query an aliased anchor map by direct index - this is inadvisible and returned the fallback value!");
            return first.isPresent() ? first.get().get(0) : null;
        }

        @Override
        public @Nullable AnchorPoint get(GridUUID addr) {
            Objects.requireNonNull(addr);
            AnchorArray contents = aliases.get(addr);
            return contents == null ? null : contents.get(addr);
        }

        public void combineWith(GridUUID alias, AnchorArray contents) {
            Objects.requireNonNull(contents);
            Objects.requireNonNull(alias);
            if(contents.isEmpty()) {
                Mechano.LOGGER.warn("Skipped adding an empty AnchorArray to an aliased map");
                return;
            }
            AnchorArray previousMapped = this.aliases.put(alias, contents);
            if(previousMapped != null)
                Mechano.LOGGER.error("AliasedAnchorMap add operation overwrote a previous mapping! This indicates a potential Griddable instance leak.");
        }

        @Override
        public Iterator<AnchorPoint> iterator() {
            return null;
        }

        @Override
        public int size() {
            int out = 0;
            for(AnchorArray contents : aliases.values())
                out += contents.size();
            return out;
        }

        @Override
        public void add(AnchorPoint anchor) {
            throw new UnsupportedOperationException("Aliased anchor maps cannot have raw AnchorPoints added directly to them! Use combineWith() instead!");
        }

        @Override
        public AnchorCollection bindTo(Griddable<?> host) {
            throw new UnsupportedOperationException("Aliased anchor maps cannot be rebound!");
        }

        @Override
        public void forEach(Consumer<? super AnchorPoint> action) {
            for(AnchorArray contents : aliases.values()) {
                for(int x = 0; x < contents.size(); x++) {
                    AnchorPoint ap = contents.get(x);
                    action.accept(ap);
                }
            }
        }
    }
}
