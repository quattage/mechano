package com.quattage.mechano.content.block.power.transfer.connector.transmission.stacked;

import com.quattage.mechano.core.block.orientation.relative.Relative;
import com.quattage.mechano.core.electricity.blockEntity.ElectricBlockEntity;
import com.quattage.mechano.core.electricity.node.NodeBankBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ConnectorStackedTier3BlockEntity extends ElectricBlockEntity {

    public ConnectorStackedTier3BlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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
                .at(20, 21, 8)
                .mode("B")
                .connections(3)
                .build()
            .newNode()
                .id("conn2")
                .at(-4, 21, 8) 
                .mode("B")
                .connections(3)
                .build()
        ;
    }
    
    
}
