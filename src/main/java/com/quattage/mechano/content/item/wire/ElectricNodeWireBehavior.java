package com.quattage.mechano.content.item.wire;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.item.spool.WireSpool;
import com.quattage.mechano.core.blockEntity.ElectricBlockEntity;
import com.quattage.mechano.core.blockEntity.SyncableBlockEntity;
import com.quattage.mechano.core.blockEntity.observe.IObservable;
import com.quattage.mechano.core.electricity.node.base.ElectricNode;
import com.quattage.mechano.core.events.ClientBehavior;
import com.quattage.mechano.registry.MechanoBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.utility.Color;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public class ElectricNodeWireBehavior extends ClientBehavior {

    public ElectricNodeWireBehavior(String name) {
        super(name);
    }

    @Override
    public boolean shouldTick(ClientLevel world, Player player, ItemStack mainHand, ItemStack offHand,
            Vec3 lookingPosition, BlockPos lookingBlockPos) {

        return mainHand.getItem() instanceof WireSpool 
            && world.getBlockEntity(lookingBlockPos) instanceof ElectricBlockEntity;
    }

    @Override
    public void tickSafe(ClientLevel world, Player player, ItemStack mainHand, ItemStack offHand, Vec3 lookingPosition,
            BlockPos lookingBlockPos) {
        if(world.getBlockEntity(lookingBlockPos) instanceof ElectricBlockEntity blockEntity) {
            Mechano.logSlow("" + blockEntity.nodes);
            for(ElectricNode node : blockEntity.nodes.values()) {
                Color c = new Color(255, 255, 255);
                if(node.getId().equals("INPUT")) c = new Color(0, 255, 0);
                else if(node.getId().equals("OUTPUT")) c = new Color(255, 0, 0);
                CreateClient.OUTLINER.showAABB(node.getId() + "Highlight", node.getHitbox())
                    .lineWidth(1 / 32f)
                    .colored(c);
            }
        }
    }
}
