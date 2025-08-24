package com.quattage.mechano.foundation.api.anchor;

import java.util.function.Consumer;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.Griddable;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Wraps an immutable array of {@link AnchorPoint} objects
 * and provides helpers for managing and accessing them.
 */
@OnlyIn(Dist.CLIENT)
public class AnchorArray {

    public static final AnchorArray EMPTY = new AnchorArray(new AnchorPoint[0]);
    protected AnchorPoint[] anchors;

    public static AnchorArray ofSingle(AnchorPoint anchor) {
        return new AnchorArray(new AnchorPoint[] {anchor});
    }

    public static AnchorArray.Builder construct(Griddable<?> points) {
        return new Builder(points);
    }

    private AnchorArray(AnchorPoint[] anchors) {
        this.anchors = anchors;
    }

    public void forEach(Consumer<AnchorPoint> action) {
        for(int x = 0; x < anchors.length; x++) {
            action.accept(anchors[x]);
        }
    }

    /**
     * @return The size of this AnchorArray
     */
    public int size() {
        return anchors.length;
    }

    /**
     * @param index
     * @return The AnchorPoint at the given index
     * @throws ArrayIndexOutOfBoundsException
     */
    public AnchorPoint getByIndex(int index) {
        if(index < 0 || index >= anchors.length)
            throw new ArrayIndexOutOfBoundsException("Can't retrieve AnchorPoint at index " + index + " from array of size " + anchors.length);
        return anchors[index];
    }

    /**
     * Updates the location and hitbox of all {@link AnchorPoint} objects
     * in this AnchorArray to reflect the data contained within the given 
     * BlockState
     * @param state state to extract orientation data from
     */
    public void updateOrientation(BlockState state) {
        CombinedOrientation dir = DirectionTransformer.extract(state);
        forEach(anchor -> {
            anchor.updateOrientation(dir);
        });
    }

    @Override
    public String toString() {
        if(anchors.length == 0) return "AnchorPoints[\n\tEMPTY\n]";
        String output = "AnchorPoints[\n";
        for(int x = 0; x < anchors.length; x++)
            output += anchors[x] == null ? "\tnull,\n" : ("\t" + anchors[x].toString() + ", \n");
        return output + "]";
    }

    public boolean contains(GridUUID addr) {
        if(addr == null) return false;
        if(addr.getIndex() < 0 || addr.getIndex() > GridUUID.MAX_SHARED_OCCUPANCY) return false;
        AnchorPoint anchor = anchors[addr.getIndex()];
        if(anchor == null) return false;
        return addr.equals(anchor.getAddress());
    }



    public static class DynamicAnchorArray extends AnchorArray {
        
        public DynamicAnchorArray() {
            super(new AnchorPoint[0]);
        }

        public void add(AnchorPoint anchor) {
            AnchorPoint[] copy = new AnchorPoint[anchors.length + 1];
            System.arraycopy(anchors, 0, copy, 0, anchors.length);
            copy[anchors.length] = anchor;
            this.anchors = copy;
        }
    }



    public static class Builder {
        
        private ObjectArrayList<AnchorPoint.Builder> anchors = new ObjectArrayList<>(GridUUID.MAX_SHARED_OCCUPANCY);
        private Griddable<?> points;

        public Builder(Griddable<?> points) {
            this.points = points;
        }

        protected void add(AnchorPoint.Builder newBuilder) {
            anchors.add(newBuilder);
        }
        
        public AnchorPoint.Builder add() {
            return new AnchorPoint.Builder(this);
        }

        public AnchorArray confirm(BlockPos pos) {
            anchors.trim();
            if(anchors.isEmpty()) {
                Mechano.LOGGER.warn("AnchorPoint array for " + points + " - was built with no members!");
                return AnchorArray.EMPTY;
            }
            AnchorPoint[] builtAnchors = new AnchorPoint[anchors.size()];
            for(int x = 0; x < builtAnchors.length; x++) {
                if(x >= GridUUID.MAX_SHARED_OCCUPANCY) {
                    Mechano.LOGGER.warn("Skipped adding AnchorPoint to " + points + " - Max anchor occupancy (" + GridUUID.MAX_SHARED_OCCUPANCY + ") has been reached!");
                    break;
                } builtAnchors[x] = anchors.get(x).make(pos, x);
            } return new AnchorArray(builtAnchors);
        }

        public AnchorArray confirm(BlockPos pos, GridUUID override) {
            anchors.trim();
            if(anchors.isEmpty()) {
                Mechano.LOGGER.warn("AnchorPoint array for " + points + " - was built with no members!");
                return AnchorArray.EMPTY;
            }
            AnchorPoint[] builtAnchors = new AnchorPoint[anchors.size()];
            for(int x = 0; x < builtAnchors.length; x++) {
                if(x >= GridUUID.MAX_SHARED_OCCUPANCY) {
                    Mechano.LOGGER.warn("Skipped adding AnchorPoint to " + points + " - Max anchor occupancy (" + GridUUID.MAX_SHARED_OCCUPANCY + ") has been reached!");
                    break;
                } 
                builtAnchors[x] = anchors.get(x).make(override.indexedCopy(x));
            } return new AnchorArray(builtAnchors);
        }
    }


    
}
