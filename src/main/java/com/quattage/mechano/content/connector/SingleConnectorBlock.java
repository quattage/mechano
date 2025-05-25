package com.quattage.mechano.content.connector;

import com.quattage.mechano.MechanoBlockEntities;

import net.minecraft.world.level.block.entity.BlockEntityType;

public class SingleConnectorBlock extends ConnectorBlock<SingleConnectorBlockEntity> {

    public SingleConnectorBlock(Properties pProperties) {
        super(pProperties);
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
