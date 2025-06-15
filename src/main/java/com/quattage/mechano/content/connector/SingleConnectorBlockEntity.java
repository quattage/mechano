package com.quattage.mechano.content.connector;

import com.quattage.mechano.foundation.api.anchor.AnchorArray.Builder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class SingleConnectorBlockEntity extends ConnectorBlockEntity {

    public SingleConnectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void constructAnchors(Builder anchors) {
        anchors
            .add()
                .at(8, 16, 8) 
                .connections(3)
                .radius(1.7f)
                .make();
    }
}

