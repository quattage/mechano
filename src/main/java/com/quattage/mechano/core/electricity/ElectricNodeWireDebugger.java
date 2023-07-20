package com.quattage.mechano.core.electricity;

import com.quattage.mechano.core.electricity.node.base.ElectricNode;
import com.quattage.mechano.core.events.ClientBehavior;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.utility.Color;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class ElectricNodeWireDebugger extends ClientBehavior {

    public ElectricNodeWireDebugger(String name) {
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
        
        if(world.getBlockEntity(lookingBlockPos) instanceof ElectricBlockEntity blockEntity) {
            for(ElectricNode node : blockEntity.nodes.values()) {
                CreateClient.OUTLINER.showAABB(node.getId() + "Highlight", node.getHitbox()
                    .inflate((Math.sin(((pTicks * 0.201) - 10.2) / 6.36) * 0.01)))
                    .disableCull()
                    .lineWidth(1 / 32f)
                    .colored(new Color(255, 255, 255)
                        .mixWith(node.getColor(), Mth.clamp((float)Math.sin((pTicks - 0.50) * 3.2) * 0.4f + 1, 0, 1)));
            }
        }
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
