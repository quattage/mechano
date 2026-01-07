package com.quattage.mechano.foundation.block.orientation;

import net.minecraft.core.Direction;

public class RelativeDirection implements OrientationUpdatable {

    private final Relative rel;
    private Direction facingDir;

    public static RelativeDirection[] all() {
        Relative[] allRels = Relative.values();
        RelativeDirection[] out = new RelativeDirection[allRels.length];
        for(int x = 0; x < out.length; x++) 
            out[x] = new RelativeDirection(allRels[x]);
        return out;
    }

    public RelativeDirection(Relative rel) {
        this.rel = rel;
        this.facingDir = rel.getDefaultDir();
    }

    @Override
    public void updateOrientation(CombinedOrientation dir) {
        this.facingDir = rel.apply(dir);
    }

    /**
     * Gets this RelativeDirection's actual, globally-oriented {@link Direction} in its current 
     * state. You'll need to ensure that {@link #updateOrientation} is called at least once
     * before this method is called after blockstate changes so that the direction returned
     * from this method is up-to-date.
     * @param dir (Optional) a {@link CombinedOrientation} to update this RelativeDirection with
     * @return The current global facing {@link Direction} that this RelativeDirection represents
     * @see #updateOrientation
     */
    public Direction get() {
        return facingDir;
    }

    /**
     * Gets this RelativeDirection's actual, globally-oriented 
     * {@link Direction} in its current state.
     * @param dir (Optional) a {@link CombinedOrientation} to update this RelativeDirection with
     * @return The current global facing {@link Direction} that this RelativeDirection represents
     * @see #updateOrientation
     */
    public Direction get(CombinedOrientation dir) {
        updateOrientation(dir);
        return facingDir;
    }

    public Relative getRaw() {
        return rel;
    }

    @Override
    public String toString() {
        return facingDir + " [relative to local " + rel + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(!(obj instanceof RelativeDirection that)) return false;
        return this.rel == that.rel;
    }

    @Override
    public int hashCode() {
        return this.rel.hashCode();
    }
}
