package com.quattage.mechano.content.creative;

import com.quattage.mechano.MechanoBlockEntities;
import com.quattage.mechano.content.connector.BlockWithConnections;
import com.quattage.mechano.foundation.block.hitbox.MechanoHitboxes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CreativeSinkBlock extends BlockWithConnections<CreativeSinkBlockEntity> {

    public CreativeSinkBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public boolean canFloat() {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return MechanoHitboxes.CREATIVE_SINK.get(state);
    }

    @Override
    public Class<CreativeSinkBlockEntity> getBlockEntityClass() {
        return CreativeSinkBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CreativeSinkBlockEntity> getBlockEntityType() {
        return MechanoBlockEntities.CREATIVE_SINK.get();
    }

    
}
