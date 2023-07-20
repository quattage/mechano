package com.quattage.mechano.core.electricity;

import com.quattage.mechano.core.block.CombinedOrientedBlock;
import com.quattage.mechano.core.block.orientation.CombinedOrientation;
import com.quattage.mechano.core.electricity.observe.IObservable;
import com.quattage.mechano.core.electricity.observe.NodeDataPacket;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ElectricBlock extends CombinedOrientedBlock implements IObservable {

    public ElectricBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        orientNodes(world, pos, state, state.getValue(CombinedOrientedBlock.ORIENTATION));
        super.onPlace(state, world, pos, oldState, isMoving);
    }
    
    public static void orientNodes(Level world, BlockPos pos, BlockState state, CombinedOrientation dir) {
        if(!state.hasBlockEntity()) return;
        BlockEntity be = world.getBlockEntity(pos);
        if(be instanceof ElectricBlockEntity ebe) {
            ebe.setOrient(dir);
        }
    }

    @Override
    public void playerWillDestroy(Level pLevel, BlockPos pPos, BlockState pState, Player pPlayer) {
        
        super.playerWillDestroy(pLevel, pPos, pState, pPlayer);
    }

    @Override
    public void onLookedAt(ServerPlayer player, NodeDataPacket packet) {
        
    }
}
