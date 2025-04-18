package com.quattage.mechano.content.test;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.block.CombinedOrientedBlock;
import com.quattage.mechano.foundation.block.hitbox.RotatableHitboxShape;
import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TestAxisBlock extends CombinedOrientedBlock {

    protected static RotatableHitboxShape<CombinedOrientation> hitbox;

    public TestAxisBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if(hitbox == null) hitbox = Mechano.HITBOXES.get(this, null, ORIENTATION);
        return hitbox.getRotated(state.getValue(ORIENTATION));
    }
}
