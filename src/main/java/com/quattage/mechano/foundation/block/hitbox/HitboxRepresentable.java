
package com.quattage.mechano.foundation.block.hitbox;

import java.util.Arrays;

import net.minecraft.world.level.ClipContext.ShapeGetter;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface HitboxRepresentable extends ShapeGetter {
    public VoxelShape get(Object... tokens);

    public VoxelShape get();

    default Object[] minusFirst(Object[] tokens) {
        return Arrays.copyOfRange(tokens, 1, tokens.length);
    }
}
