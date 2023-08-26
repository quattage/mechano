package com.quattage.mechano.content.item.spool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.core.block.orientation.relative.RelativeDirection;
import com.quattage.mechano.core.electricity.blockEntity.ElectricBlockEntity;
import com.quattage.mechano.core.electricity.node.NodeBank;
import com.quattage.mechano.core.electricity.node.base.ElectricNode;
import com.quattage.mechano.core.events.ClientBehavior;
import com.quattage.mechano.core.util.VectorHelper;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import oshi.util.tuples.Triplet;

public class ElectricNodeSpoolBehavior extends ClientBehavior {

    private double growProgress = 0;
    private double newGrow = 0;
    private double oldGrow = 0;

    public ElectricNodeSpoolBehavior(String name) {
        super(name);
    }

    @Override
    public boolean shouldTick(ClientLevel world, Player player, ItemStack mainHand, ItemStack offHand,
            Vec3 lookingPosition, BlockPos lookingBlockPos) {

        return (mainHand.getItem() instanceof WireSpool
            || mainHand.getItem() instanceof WrenchItem)
            && !instance.options.renderDebug;
    }

    @Override
    public void tickSafe(ClientLevel world, Player player, ItemStack mainHand, ItemStack offHand, 
        Vec3 lookingPosition, BlockPos lookingBlockPos, double pTicks) {

        boolean isOccupied = false;

        if(mainHand.hasTag()) {
            isOccupied = mainHand.getTag().contains("at") || mainHand.getTag().contains("from");
        }
    

        drawCapabilityMarkers(world, player, mainHand, offHand, lookingPosition, lookingBlockPos, pTicks, isOccupied);
        drawNodes(world, player, mainHand, offHand, lookingPosition, lookingBlockPos, pTicks, isOccupied);
    }

    /***
     * Renders boxes and particle effects to convey information regarding ElectricNodes to the player. <p>
     * Searches in a cone where the player is facing for relevent ElectricBlockEntities places small
     * indicators in the world to tell the player where ElectricNodes are.
     */
    private void drawNodes(ClientLevel world, Player player, ItemStack mainHand, ItemStack offHand, 
        Vec3 lookingPosition, BlockPos lookingBlockPos, double pTicks, boolean isOccupied) {

        Triplet<ArrayList<ElectricNode>, Integer, NodeBank> releventNodes = null;

        // if we're looking directly at an ElectricBlockEntity we don't have to find one
        if(world.getBlockEntity(lookingBlockPos) instanceof ElectricBlockEntity ebe) {
            Pair<ElectricNode[], Integer> direct = ebe.nodeBank.getAllNodes(lookingPosition);
            releventNodes = new Triplet<ArrayList<ElectricNode>, Integer, NodeBank> (
                    new ArrayList<ElectricNode>(Arrays.asList(direct.getFirst())), 
                    direct.getSecond(), ebe.nodeBank
                );
        }
        else {
            releventNodes = NodeBank.findClosestNodeAlongRay(
                world, 
                instance.cameraEntity.getEyePosition(), 
                lookingPosition, 0
            );
        }

        if(releventNodes.getB() == -1) {
            decrementGrow();
        }

        int index = 0;
        for(ElectricNode node : releventNodes.getA()) {

            AABB nodeBox = node.getHitbox();
            if(releventNodes.getB() == index) {

                newGrow = incrementGrow() * 0.03;
                nodeBox = nodeBox.inflate(Mth.lerp(pTicks, oldGrow, newGrow));
                

                CreateClient.OUTLINER.showAABB(node.getId() + name, nodeBox)
                    .disableLineNormals()
                    .withFaceTexture(AllSpecialTextures.CUTOUT_CHECKERED)
                    .lineWidth(0.03f)
                    .colored(node.getColor((float)growProgress));

                oldGrow = newGrow;
            } else {
                CreateClient.OUTLINER.showAABB(node.getId() + name, nodeBox)
                    .disableLineNormals()
                    .withFaceTexture(AllSpecialTextures.CUTOUT_CHECKERED)
                    .lineWidth(0.03f)
                    .colored(node.getColor());
            } 
            
            index++;
        }

    }

    /***
     * Only for debug and will not be used, either removed later or tied to a config option.
     * Represents ForgeEnergy capability directions as small boxes to indicate all valid
     * push/pull sides for the targeted ElectricBlockEntity
     */
    private void drawCapabilityMarkers(ClientLevel world, Player player, ItemStack mainHand, ItemStack offHand, Vec3 lookingPosition,
        BlockPos lookingBlockPos, double pTicks, boolean isOccupied) {
        if(world.getBlockEntity(lookingBlockPos) instanceof ElectricBlockEntity ebe) {

            Optional<RelativeDirection[]> dirsOptional = ebe.nodeBank.getRelativeDirs();
            if(!dirsOptional.isPresent()) return;
            RelativeDirection[] dirs = dirsOptional.get();
            if(dirs.length == 0) dirs = RelativeDirection.populateAll();
            
            for(int x = 0; x < dirs.length; x++) {
                BlockPos pos = lookingBlockPos.relative(dirs[x].get());

                AABB cm = VectorHelper.toAABB(pos, 0.2f);
                CreateClient.OUTLINER.showAABB(pos.hashCode() + "_cm_" + x + "(" + dirs[x] + ")", cm)
                    .disableLineNormals()
                    .withFaceTexture(AllSpecialTextures.CUTOUT_CHECKERED)
                    .lineWidth(0.01f)
                    .colored(dirs[x].getColor());
            }
        }
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
