package com.quattage.mechano.content.test;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoBlockEntities;
import com.quattage.mechano.foundation.SimpleBlockEntity.SBE;
import com.quattage.mechano.foundation.block.CombinedOrientedBlock;
import com.quattage.mechano.foundation.block.hitbox.RotatableHitboxShape;
import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TestAxisBlock extends CombinedOrientedBlock implements SBE<TestAxisBlockEntity> {

    protected static RotatableHitboxShape<CombinedOrientation> hitbox;

    public TestAxisBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        onBlockPlaced(oldState, level, pos, state);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        onBlockBroken(state, level, pos, newState);
    }

    @Override
    public void onBlockStateChange(LevelReader level, BlockPos pos, BlockState oldState, BlockState newState) {
        onBlockStateChanged(oldState, level, pos, newState);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if(hitbox == null) hitbox = Mechano.HITBOXES.get(this, null, ORIENTATION);
        return hitbox.getRotated(state.getValue(ORIENTATION));
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
