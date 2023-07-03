package com.quattage.mechano.content.block.power.transfer.test;

import java.util.ArrayList;
import java.util.List;

import com.quattage.mechano.core.electricity.base.ElectricBlockEntity;
import com.quattage.mechano.core.electricity.base.SyncableBlockEntity;
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
    protected void setData() {
        if(!getLevel().isClientSide()) test = 1;
        data.put("test", test);
    }

    @Override
    protected void getData(TagManager data) {
        test = data.getInt("test");
    }

    @Override
    public void addConnections(NodeBankBuilder builder) {
        builder
            .add(8, 8, 0, "OUTPUT", 1)
            .add(8, 8, 16, "INPUT", 1);
    }
}
