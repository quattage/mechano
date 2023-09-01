package com.quattage.mechano.core.electricity.block;

import com.quattage.mechano.core.block.orientation.CombinedOrientation;
import com.quattage.mechano.core.electricity.blockEntity.WireNodeBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/***
 * This used to be a solution for rotating nodes on placement, but it has caused some syncing issues
 * in the past. It remains here as a testament to what once was.
 */
public interface NodeOrientable {
    default void orientNodes(Level world, BlockPos pos, BlockState state, CombinedOrientation dir) {
        if(state == null) return;
        if(!state.hasBlockEntity()) return;
        if(world.getBlockEntity(pos) instanceof WireNodeBlockEntity nbe)
        nbe.nodeBank.reflectStateChange(state);
    }
}
