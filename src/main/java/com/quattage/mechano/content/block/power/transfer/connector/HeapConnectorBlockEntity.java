package com.quattage.mechano.content.block.power.transfer.connector;

import com.quattage.mechano.core.electricity.blockEntity.ElectricBlockEntity;
import com.quattage.mechano.core.electricity.node.NodeBankBuilder;
import com.quattage.mechano.core.electricity.observe.NodeDataPacket;
import com.quattage.mechano.core.electricity.observe.NodeObservable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class HeapConnectorBlockEntity extends ElectricBlockEntity {


    public HeapConnectorBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }


    @Override
    public void addConnections(NodeBankBuilder builder) {
        builder
            .newNode()
                .id("in1")
                .at(8, 0, 8) 
                .mode("I")
                .connections(2)
                .build()
        ;
    }
}
