package com.quattage.mechano.content.item.wire;

import java.util.Optional;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.item.spool.WireSpool;
import com.quattage.mechano.core.block.orientation.relative.Relative;
import com.quattage.mechano.core.block.orientation.relative.RelativeDirection;
import com.quattage.mechano.core.electricity.blockEntity.ElectricBlockEntity;
import com.quattage.mechano.core.electricity.node.base.ElectricNode;
import com.quattage.mechano.core.events.ClientBehavior;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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

        return mainHand.getItem() instanceof WireSpool
            && !instance.options.renderDebug;
    }

    @Override
    public void tickSafe(ClientLevel world, Player player, ItemStack mainHand, ItemStack offHand, Vec3 lookingPosition,
            BlockPos lookingBlockPos, double pTicks) {

        boolean isOccupied = false;

        if(mainHand.hasTag()) {
            isOccupied = mainHand.getTag().contains("at") || mainHand.getTag().contains("from");
        }
    

        drawCapDirs(world, player, mainHand, offHand, lookingPosition, lookingBlockPos, pTicks, isOccupied);
        drawNodes(world, player, mainHand, offHand, lookingPosition, lookingBlockPos, pTicks, isOccupied);
    }

    /***
     * Renders the on-screen information relevent to the player when they're holding a WireSpool.
     * This renders the boxes that represent ElectricNodes.
     */
    private void drawNodes(ClientLevel world, Player player, ItemStack mainHand, ItemStack offHand, Vec3 lookingPosition,
        BlockPos lookingBlockPos, double pTicks, boolean isOccupied) {
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

                nodeBox = nodeBox.inflate(Mth.lerp(pTicks, oldGrow, newGrow));
                CreateClient.OUTLINER.showAABB(node.getId() + name, nodeBox)
                    .disableLineNormals()
                    .withFaceTexture(AllSpecialTextures.CUTOUT_CHECKERED)
                    .lineWidth((float)Mth.clamp(newGrow, 0.006, 0.8))
                    .colored(node.getColor((float)growProgress));
                oldGrow = newGrow;
            }
        } else {
            oldGrow = 0; newGrow = 0; growProgress = 0.0;
        }
    }

    private void drawCapDirs(ClientLevel world, Player player, ItemStack mainHand, ItemStack offHand, Vec3 lookingPosition,
        BlockPos lookingBlockPos, double pTicks, boolean isOccupied) {
        if(world.getBlockEntity(lookingBlockPos) instanceof ElectricBlockEntity ebe) {

            Optional<RelativeDirection[]> dirsOptional = ebe.nodes.getRelDirs();
            if(!dirsOptional.isPresent()) return;
            RelativeDirection[] dirs = dirsOptional.get();
            if(dirs.length == 0) dirs = RelativeDirection.populateAll();
            
            for(int x = 0; x < dirs.length; x++) {
                BlockPos pos = lookingBlockPos.relative(dirs[x].get());

                AABB cm = boxFromPos(pos, 0.2f);
                CreateClient.OUTLINER.showAABB(pos.hashCode() + "_cm_" + x + "(" + dirs[x] + ")", cm)
                    .disableLineNormals()
                    .withFaceTexture(AllSpecialTextures.CUTOUT_CHECKERED)
                    .lineWidth(0.01f)
                    .colored(dirs[x].getColor());
            }
        }
    }

    private AABB boxFromPos(Vec3 pos, float s) {
        Vec3 size = new Vec3(s, s, s);
        return new AABB(pos.subtract(size), pos.add(size));
    }

    private AABB boxFromPos(BlockPos pos, float s) {
        return boxFromPos(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) , s);
    }

    private boolean isBound(BlockEntity be, ItemStack wireStack) {
        if(wireStack.getTag() == null) return false;
        CompoundTag nbt = wireStack.getTag().getCompound("At");
        BlockPos targetPos = new BlockPos(
            nbt.getInt("x"),
            nbt.getInt("y"),
            nbt.getInt("z")
        );

        return be.getBlockPos() != targetPos;
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
