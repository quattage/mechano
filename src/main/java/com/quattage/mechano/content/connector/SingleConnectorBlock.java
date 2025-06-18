package com.quattage.mechano.content.connector;

import com.quattage.mechano.MechanoBlockEntities;
import com.quattage.mechano.foundation.block.hitbox.MechanoHitboxes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SingleConnectorBlock extends ConnectorBlock<SingleConnectorBlockEntity> {

    public SingleConnectorBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return MechanoHitboxes.CONNECTOR_SINGLE.get(state);
    }

    @Override
    public Class<SingleConnectorBlockEntity> getBlockEntityClass() {
        return SingleConnectorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SingleConnectorBlockEntity> getBlockEntityType() {
        return MechanoBlockEntities.CONNECTOR_SINGLE.get();
    }
}
