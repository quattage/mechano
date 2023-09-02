package com.quattage.mechano.content.block.power.transfer.test;

import com.quattage.mechano.MechanoBlockEntities;
import com.quattage.mechano.foundation.block.CombinedOrientedBlock;
import com.quattage.mechano.foundation.helper.CreativeTabExcludable;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class TestBlock extends CombinedOrientedBlock implements IBE<TestBlockEntity>, CreativeTabExcludable {

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
