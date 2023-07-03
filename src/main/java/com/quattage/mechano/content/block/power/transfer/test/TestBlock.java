package com.quattage.mechano.content.block.power.transfer.test;

import com.quattage.mechano.core.block.CombinedOrientedBlock;
import com.quattage.mechano.core.electricity.base.StrictElectricalBlock;
import com.quattage.mechano.registry.MechanoBlockEntities;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TestBlock extends StrictElectricalBlock implements IBE<TestBlockEntity> {

    public TestBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public Class<TestBlockEntity> getBlockEntityClass() {
        return TestBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TestBlockEntity> getBlockEntityType() {
        return MechanoBlockEntities.TEST.get();
    }
}
