package com.quattage.mechano.api.griddable;

import java.util.Locale;

import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.OrientationUpdatable;
import com.quattage.mechano.foundation.block.orientation.Relative;
import com.quattage.mechano.foundation.block.orientation.RelativeDirection;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

/**
 * Describes a single interaction to allow adjacent grid-compatible blocks
 * to form local matrices where possible and to allow FE compatibility in 
 * the future.
 */
public class VoxelPowerJunction implements OrientationUpdatable {
    
    private final RelativeDirection side;
    private FlowDirection direction;

    private VoxelPowerJunction(RelativeDirection side) {
        this.side = side;
    }
    
    public FlowDirection getFlowDirection(){ 
        return direction;
    }

    /**
     * @return The block face described by this junction
     * @see {@link #getSide}
     */
    public Relative getFace() {
        return side.getRaw();
    }

    /**
     * VoxelPowerJunctions store their direction relative to the block's center and
     * orientation. The direction returned by this method will always point outwards
     * from the block's center and should always be rotated to match the orientaiton
     * of the block itself. Calling {@link #updateOrientation} after block update
     * events is reccomended.
     * @return the direction this power junction is currently pointing.
     */
    public Direction getSide() {
        return side.get();
    }

    /**
     * Updates the orientation of this VoxelPowerJunction to reflect the current
     * orientation of its parent block. Calling this method will make calls to 
     * {@link #getSide} return a direction cooresponding to the correct block face
     * regardless of the block's orientation.
     * @param dir
     */
    @Override
    public void updateOrientation(CombinedOrientation dir) {
        side.updateOrientation(dir);
    }

    public boolean interacts() {
        if(direction == null || side == null) return false;
        return direction.interacts();
    }

    public static enum FlowDirection implements StringRepresentable {
        INWARD_ONLY(true, false),
        OUTWARD_ONLY(false, true),
        BIDIRECTIONAL(true, true),
        BLOCKING(false, false);

        private final boolean pullsIn, pushesOut;
        private FlowDirection(boolean pullsIn, boolean pushesOut) {
            this.pullsIn = pullsIn; this.pushesOut = pushesOut;
        }

        public static FlowDirection cycle(FlowDirection in) {
            return in == null ? FlowDirection.values()[0] : in.next();
        }

        public FlowDirection next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public boolean canPullIn() {
            return pullsIn;
        }

        public boolean canPushOut() {
            return pushesOut;
        }

        public boolean interacts() {
            return canPullIn() || canPushOut();
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        @Override
        public String toString() {
            return getSerializedName();
        }
    }
}
