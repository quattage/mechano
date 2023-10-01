package com.quattage.mechano.foundation.behavior;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import static com.quattage.mechano.foundation.electricity.system.GlobalTransferNetwork.NETWORK;

import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.electricity.system.SystemVertex;
import com.quattage.mechano.foundation.electricity.system.TransferSystem;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.utility.Color;

public class TransferNetworkDebugBehavior extends ClientBehavior {

    public TransferNetworkDebugBehavior(String name) {
        super(name);
    }

    @Override
    public boolean shouldTick(ClientLevel world, Player player, ItemStack mainHand, ItemStack offHand,
            Vec3 lookingPosition, BlockPos lookingBlockPos) {
        return instance.options.renderDebug;
    }

    @Override
    public void tickSafe(ClientLevel world, Player player, ItemStack mainHand, ItemStack offHand, Vec3 lookingPosition,
            BlockPos lookingBlockPos, double pTicks) {
        
        for(TransferSystem sys : NETWORK.all()) {
            SystemVertex last = null;
            Color col = sys.getDebugColor();
            for(SystemVertex node : sys.all()) {
                if(node != null && last != null) {
                    CreateClient.OUTLINER
                        .showLine(node.getPos(), node.getPos().getCenter(), last.getPos().getCenter())
                        .lineWidth(1/16f)
                        .disableCull()
                        .disableLineNormals()
                        .colored(col);
                }
                last = node;
            }
        }
    }

    public void drawNetworkStatistics(ClientLevel world, SystemVertex node) {   
        BlockEntity targetBE = world.getBlockEntity(node.getPos());
        BlockPos drawLocation = node.getPos().relative(DirectionTransformer.getUp(targetBE.getBlockState()), 2);
    }
    
    @Override
    public double setTickRate() {
        return 200;
    }

    @Override
    public double setTickIncrement() {
        return 1;
    }
}   
