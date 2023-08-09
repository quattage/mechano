package com.quattage.mechano.content.block.power.transfer.connector.transmission;

import com.quattage.mechano.core.electricity.blockEntity.ElectricBlockEntity;
import com.quattage.mechano.core.electricity.node.NodeBankBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TransmissionConnectorBlockEntity extends ElectricBlockEntity {


    public TransmissionConnectorBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }


    @Override
    public void prepare(NodeBankBuilder nodeBank) {
        nodeBank
            .newNode()
                .id("c1")
                .at(8, 0, 8) 
                .mode("B")
                .connections(2)
                .build()
        ;
    }
}
