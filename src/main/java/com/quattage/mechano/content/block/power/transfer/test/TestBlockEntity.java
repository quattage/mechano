package com.quattage.mechano.content.block.power.transfer.test;

import java.util.ArrayList;
import java.util.List;

import com.quattage.mechano.core.blockEntity.ElectricBlockEntity;
import com.quattage.mechano.core.blockEntity.SyncableBlockEntity;
import com.quattage.mechano.core.electricity.node.NodeBankBuilder;
import com.quattage.mechano.core.electricity.node.base.ElectricNode;
import com.quattage.mechano.core.util.nbt.TagManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TestBlockEntity extends ElectricBlockEntity { 

    public int test;

    public TestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addConnections(NodeBankBuilder builder) {
        builder
            .newNode()
                .id("in1")
                .at(16, 10, 6) 
                .mode("I")
                .build()
            .newNode()
                .id("out1")
                .at(0, 6, 11)
                .mode("O")
                .build();
    }
}
