package com.quattage.mechano.content.item.spool;

import static com.quattage.mechano.foundation.electricity.system.GlobalTransferNetwork.NETWORK;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EmptySpool extends Item {
    public EmptySpool(Properties properties) {
        super(properties);
    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        
        ItemStack handStack = player.getItemInHand(hand);

        if(!world.isClientSide()) {
            if(world instanceof ServerLevel sWorld)
            NETWORK.wipeNetwork(sWorld);
        }

        return InteractionResultHolder.pass(handStack);
    }
}
