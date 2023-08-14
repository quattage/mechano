package com.quattage.mechano.content.block.power.transfer.test;

import com.quattage.mechano.MechanoBlockEntities;
import com.quattage.mechano.core.CreativeTabExcludable;
import com.quattage.mechano.core.electricity.block.ElectricBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.world.level.block.entity.BlockEntityType;

public class TestBlock extends ElectricBlock implements IBE<TestBlockEntity>, CreativeTabExcludable {

    public TestBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public Class<TestBlockEntity> getBlockEntityClass() {
        return TestBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TestBlockEntity> getBlockEntityType() {
        return MechanoBlockEntities.TEST_BLOCK.get();
    }
}
