package com.quattage.mechano.content.block.power.transfer.test;

import com.quattage.mechano.core.electricity.base.SyncableBlockEntity;
import com.quattage.mechano.core.nbt.TagManager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ElectricBlockEntity extends SyncableBlockEntity{

    

    public ElectricBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void setData() {
        
    }

    @Override
    protected void getData(TagManager data) {
        
    }
    
}
