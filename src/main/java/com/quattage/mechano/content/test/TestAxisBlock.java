package com.quattage.mechano.content.test;

import com.quattage.mechano.MechanoBlockEntities;
import com.quattage.mechano.foundation.api.blockEntity.SimpleBlockEntity.BERefreshable;
import com.quattage.mechano.foundation.block.CombinedOrientedBlock;
import com.quattage.mechano.foundation.block.hitbox.MechanoHitboxes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TestAxisBlock extends CombinedOrientedBlock implements BERefreshable<TestAxisBlockEntity> {

    public TestAxisBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void onBlockStateChange(LevelReader level, BlockPos pos, BlockState oldState, BlockState newState) {
        refreshBE(oldState, level, pos, newState);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return MechanoHitboxes.TEST_AXIS.get(state);
    }

    @Override
    public Class<TestAxisBlockEntity> getBlockEntityClass() {
        return TestAxisBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TestAxisBlockEntity> getBlockEntityType() {
        return MechanoBlockEntities.TEST_AXIS.get();
    }
}
