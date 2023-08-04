package com.quattage.mechano.content.block.power.transfer.test;

import com.quattage.mechano.core.block.orientation.relative.Relative;
import com.quattage.mechano.core.electricity.blockEntity.ElectricBlockEntity;
import com.quattage.mechano.core.electricity.node.NodeBankBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TestBlockEntity extends ElectricBlockEntity { 

    public int test;

    public TestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void prepare(NodeBankBuilder nodeBank) {
        nodeBank
            .capacity(5000)
            .maxIO(2500)
            .interfaceSide(Relative.LEFT)
            .newNode()
                .id("in1")
                .at(16, 10, 6) 
                .mode("I")
                .connections(2)
                .build()
            .newNode()
                .id("out1")
                .at(0, 6, 11)
                .mode("O")
                .connections(2)
                .build()
            .newNode()
                .id("test")
                .at(8, 16, 8)
                .mode("B")
                .connections(2)
                .build()
        ;
    }
}
