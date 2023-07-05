package com.quattage.mechano.content.item.wire;

import javax.lang.model.util.ElementScanner14;

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
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ElectricNodeWireBehavior extends ClientBehavior {

    private double growProgress = 0;
    private double newGrow = 0;
    private double oldGrow = 0;

    public ElectricNodeWireBehavior(String name) {
        super(name);
    }

    @Override
    public boolean shouldTick(ClientLevel world, Player player, ItemStack mainHand, ItemStack offHand,
            Vec3 lookingPosition, BlockPos lookingBlockPos) {

        return (mainHand.getItem() instanceof WireSpool 
            && world.getBlockEntity(lookingBlockPos) instanceof ElectricBlockEntity
            && !instance.options.renderDebug)
            || growProgress > 0;
            
    }

    @Override
    public void tickSafe(ClientLevel world, Player player, ItemStack mainHand, ItemStack offHand, Vec3 lookingPosition,
            BlockPos lookingBlockPos, double pTicks) {
        if(world.getBlockEntity(lookingBlockPos) instanceof ElectricBlockEntity blockEntity) {
            Pair<ElectricNode, Double> target = blockEntity.nodes.getClosest(lookingPosition);

            if(target != null) {
                ElectricNode node = target.getFirst();
                double distance = target.getSecond().doubleValue();

                AABB nodeBox = node.getHitbox();
                
                if(distance < (node.getHitSize() * 1.5) + (growProgress * 0.3))
                    newGrow = incrementGrow() * 0.03;
                else
                    newGrow = decrementGrow() * 0.03;
                Mechano.log("TICK: " + pTicks);
                nodeBox = nodeBox.inflate(Mth.lerp(pTicks, oldGrow, newGrow));
                CreateClient.OUTLINER.showAABB(node.getId() + name, nodeBox)
                    .lineWidth((float)Mth.clamp(newGrow, 0.01, 1))
                    .colored(node.getColor((float)growProgress));

                oldGrow = newGrow;
            }
        } else {
            oldGrow = 0; newGrow = 0; growProgress = 0.0;
        }
    }

    private double incrementGrow() {
        if(growProgress < 0.67) {
            growProgress += 0.1;
            return Math.sin(Math.pow((growProgress * 2), 2) * 1.05);
        }
        return 0.9509;
    }

    private double decrementGrow() {
        if(growProgress > 0) {
            growProgress -= 0.1;
            return Math.sin(Math.pow((growProgress * 2), 2) * 0.85);
        }
        return 0;
    }
}
