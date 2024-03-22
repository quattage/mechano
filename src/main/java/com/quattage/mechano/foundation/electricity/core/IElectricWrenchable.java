package com.quattage.mechano.foundation.electricity.core;

import com.quattage.mechano.foundation.electricity.ElectricBlockEntity;
import com.quattage.mechano.foundation.electricity.IBatteryBank;
import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface IElectricWrenchable extends IWrenchable {

    /***
     * Fixes the data in ElectricBlockEntities and WireNodeBlockEntities after
     * their blocks have been rotated by wrenches.
     * @param oldState BlockState before the block was wrenched.
     * @param context
     */
    default void confirmWrench(BlockState oldState, UseOnContext context) {

        BlockEntity be = context.getLevel().getBlockEntity(context.getClickedPos());

        if(be == null) return;
        if(be.getBlockState() == oldState) return;

        if(be instanceof IBatteryBank bb) {
            bb.reOrient();
            playRotateSound(context.getLevel(), context.getClickedPos());
        }
    }
}
