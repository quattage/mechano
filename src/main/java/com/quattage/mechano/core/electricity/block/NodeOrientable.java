package com.quattage.mechano.core.electricity.block;

import com.quattage.mechano.core.block.orientation.CombinedOrientation;
import com.quattage.mechano.core.electricity.blockEntity.ElectricBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/***
 * This used to be a solution for rotating nodes on placement, but it has caused some syncing issues
 * in the past. It remains here as a testament to what once was.
 */
public interface NodeOrientable {
    default void orientNodes(Level world, BlockPos pos, BlockState state, CombinedOrientation dir) {
        if(!state.hasBlockEntity()) return;
        BlockEntity be = world.getBlockEntity(pos);
        if(be instanceof ElectricBlockEntity ebe) {
            ebe.nodeBank.rotate(dir);
        }
    }
}
