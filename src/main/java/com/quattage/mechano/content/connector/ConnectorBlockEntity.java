package com.quattage.mechano.content.connector;

import com.quattage.mechano.foundation.blockEntity.GriddableBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ConnectorBlockEntity extends GriddableBlockEntity {

    public ConnectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
