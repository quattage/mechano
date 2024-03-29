package com.quattage.mechano.content.block.power.transfer.connector.transmission.stacked;

import com.quattage.mechano.core.block.orientation.relative.Relative;
import com.quattage.mechano.core.electricity.blockEntity.ElectricBlockEntity;
import com.quattage.mechano.core.electricity.node.NodeBankBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ConnectorStackedTier0BlockEntity extends ElectricBlockEntity {

    public ConnectorStackedTier0BlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void prepare(NodeBankBuilder nodeBank) {
        nodeBank
            .capacity(5000)
                .maxIO(2500)
                .interfaceSide(Relative.BOTTOM)
            .newNode()
                .id("conn1")
                .at(8, 16, 8) 
                .mode("B")
                .connections(2)
                .build()
        ;
    }
}
