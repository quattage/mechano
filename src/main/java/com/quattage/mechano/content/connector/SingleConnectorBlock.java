package com.quattage.mechano.content.connector;

import com.quattage.mechano.MechanoBlockEntities;
import com.quattage.mechano.foundation.SimpleBlockEntity.BERefreshable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class SingleConnectorBlock extends ConnectorBlock implements BERefreshable<SingleConnectorBlockEntity>{

    public SingleConnectorBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void onBlockStateChange(LevelReader level, BlockPos pos, BlockState oldState, BlockState newState) {
        refreshBE(oldState, level, pos, newState);
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
