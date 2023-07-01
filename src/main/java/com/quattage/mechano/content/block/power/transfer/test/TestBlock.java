package com.quattage.mechano.content.block.power.transfer.test;

import com.quattage.mechano.core.block.StrictComplexDirectionalBlock;
import com.quattage.mechano.registry.MechanoBlockEntities;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.world.level.block.entity.BlockEntityType;

public class TestBlock extends StrictComplexDirectionalBlock implements IBE<ElectricBlockEntity> {

    public TestBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public Class<ElectricBlockEntity> getBlockEntityClass() {
        return ElectricBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ElectricBlockEntity> getBlockEntityType() {
        return MechanoBlockEntities.TEST.get();
    }
}
