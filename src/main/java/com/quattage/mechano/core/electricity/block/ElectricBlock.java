package com.quattage.mechano.core.electricity.block;

import com.quattage.mechano.core.block.CombinedOrientedBlock;
import com.quattage.mechano.core.electricity.observe.NodeObservable;
import com.quattage.mechano.core.electricity.observe.NodeDataPacket;

import net.minecraft.core.BlockPos; 
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ElectricBlock extends CombinedOrientedBlock implements NodeObservable {

    public ElectricBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, world, pos, oldState, isMoving);
    }
    

    @Override
    public void playerWillDestroy(Level pLevel, BlockPos pPos, BlockState pState, Player pPlayer) {
        super.playerWillDestroy(pLevel, pPos, pState, pPlayer);
    }

    @Override
    public void onLookedAt(ServerPlayer player, NodeDataPacket packet) {
        
    }
}
