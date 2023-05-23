package com.quattage.mechano.core.util;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class Stacky {
    /***
     * Returns a new ItemStack from an input BlockState
     * @param bs
     * @return new ItemStack
     */
    public static ItemStack newStack(BlockState bs) {
        return newStack(bs.getBlock());
    }

    /***
     * Returns a new ItemStack fron an imput BlockEntity
     * @param be
     * @return
     */
    public static ItemStack newStack(BlockEntity be) {
        return newStack(be.getBlockState().getBlock());
    }

    /***
     * Returns a new ItemStack fron an input Block
     * @param block 
     * @return new ItemStack
     */
    public static ItemStack newStack(Block block) {
        Item out =   block.defaultBlockState().getBlock().asItem();
        return new ItemStack(out);
    }
}
