package com.quattage.mechano.content.block.power.transfer.connector.transmission.stacked;

import com.quattage.mechano.core.block.orientation.relative.Relative;
import com.quattage.mechano.core.electricity.blockEntity.ElectricBlockEntity;
import com.quattage.mechano.core.electricity.node.NodeBankBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ConnectorStackedTier2BlockEntity extends ElectricBlockEntity {

    public ConnectorStackedTier2BlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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
                .at(13, 20, 8) 
                .mode("B")
                .connections(2)
                .build()
            .newNode()
                .id("conn2")
                .at(3, 20, 8) 
                .mode("B")
                .connections(2)
                .build()
        ;
    }
    
    
}
