package com.quattage.mechano.foundation.electricity.core;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.electricity.ElectricBlockEntity;
import com.quattage.mechano.foundation.electricity.WireNodeBlockEntity;
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

        Mechano.log("Wrench Hit");

        if(be instanceof WireNodeBlockEntity wbe) {
            wbe.reOrient();
            playRotateSound(context.getLevel(), context.getClickedPos());
            return;
        }

        if(be instanceof ElectricBlockEntity ebe) {
            ebe.reOrient();
            playRotateSound(context.getLevel(), context.getClickedPos());
            return;
        }
    }
}
