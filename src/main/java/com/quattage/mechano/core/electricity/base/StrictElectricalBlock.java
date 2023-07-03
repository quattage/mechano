package com.quattage.mechano.core.electricity.base;

import com.quattage.mechano.core.block.CombinedOrientedBlock;
import com.quattage.mechano.core.block.orientation.CombinedOrientation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class StrictElectricalBlock extends CombinedOrientedBlock {

    public StrictElectricalBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, world, pos, oldState, isMoving);
        orientNodes(world, pos, state, state.getValue(CombinedOrientedBlock.ORIENTATION));
    }
    
    public static void orientNodes(Level world, BlockPos pos, BlockState state, CombinedOrientation dir) {
        if(!state.hasBlockEntity()) return;
        BlockEntity be = world.getBlockEntity(pos);
        if(be instanceof ElectricBlockEntity ebe) {
            ebe.setOrient(dir);
        }
    }
}
