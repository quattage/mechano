package com.quattage.mechano.content.connector;

import com.quattage.mechano.api.blockEntity.GriddableBlockEntity;
import com.quattage.mechano.api.grid.topology.CircuitComponent;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ConnectorBlockEntity extends GriddableBlockEntity {

    public ConnectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        CircuitComponent c = getCircuit();
    }
}
