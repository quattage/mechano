package com.quattage.mechano.foundation.behavior;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import javax.annotation.Nullable;

import org.joml.Vector3f;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.block.orientation.relative.RelativeDirection;
import com.quattage.mechano.foundation.effect.ParticleBuilder;
import com.quattage.mechano.foundation.effect.ParticleSpawner;
import com.quattage.mechano.foundation.electricity.NodeBank;
import com.quattage.mechano.foundation.electricity.WireNodeBlockEntity;
import com.quattage.mechano.foundation.electricity.WireSpool;
import com.quattage.mechano.foundation.electricity.core.InteractionPolicy;
import com.quattage.mechano.foundation.electricity.core.node.ElectricNode;
import com.quattage.mechano.foundation.helper.VectorHelper;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
     * Searches in a cone where the player is facing for relevent ElectricBlockEntities places small
     * indicators in the world to tell the player where ElectricNodes are.
     */
    private void drawNodes(ClientLevel world, Player player, ItemStack mainHand, ItemStack offHand, 
        Vec3 lookingPosition, BlockPos lookingBlockPos, double pTicks, boolean isOccupied) {

        Triplet<ArrayList<ElectricNode>, Integer, NodeBank<?>> releventNodes = null;

        // if we're looking directly at an ElectricBlockEntity we don't have to find one
        if(world.getBlockEntity(lookingBlockPos) instanceof WireNodeBlockEntity ebe) {
            Pair<ElectricNode[], Integer> direct = ebe.nodeBank.getAllNodes(lookingPosition);
            releventNodes = new Triplet<ArrayList<ElectricNode>, Integer, NodeBank<?>> (
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

        ElectricNode indicated = getIndicatedNode(world, mainHand);
        if(indicated != null) drawFizzles(world, indicated);

        if(releventNodes.getB() == -1) {
            decrementGrow();
        }

        NodeBank<?> indicatedBank = releventNodes.getC();
        int index = 0;
        for(ElectricNode node : releventNodes.getA()) {

            // i have no idea how the bank is ever null but this check prevents a crash
            if(indicatedBank == null) continue;
            if(node.equals(indicated)) continue;

            AABB nodeBox = node.getHitbox();
            if(releventNodes.getB() == index) {

                newGrow = incrementGrow() * 0.03;
                nodeBox = nodeBox.inflate(Mth.lerp(pTicks, oldGrow, newGrow));

                CreateClient.OUTLINER.showAABB(indicatedBank.indexOf(node) + name, nodeBox)
                    .disableLineNormals()
                    .withFaceTexture(AllSpecialTextures.CUTOUT_CHECKERED)
                    .lineWidth(0.03f)
                    .colored(node.getColor((float)growProgress));

                oldGrow = newGrow;
            } else {
                CreateClient.OUTLINER.showAABB(indicatedBank.indexOf(node) + name, nodeBox)
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
    private void drawCapabilityMarkers(ClientLevel world, Player player, ItemStack mainHand, ItemStack offHand, 
        Vec3 lookingPosition, BlockPos lookingBlockPos, double pTicks, boolean isOccupied) {
        if(world.getBlockEntity(lookingBlockPos) instanceof WireNodeBlockEntity ebe) {

            InteractionPolicy[] policies = ebe.batteryBank.getRawInteractions();
            if(policies == null) return;
            for(InteractionPolicy dir : policies) {

                BlockPos pos = lookingBlockPos.relative(dir.getDirection());
                AABB cm = VectorHelper.toAABB(pos, 0.2f);
                CreateClient.OUTLINER.showAABB(pos.hashCode() + "_cm_" + dir, cm)
                    .disableLineNormals()
                    .withFaceTexture(AllSpecialTextures.CUTOUT_CHECKERED)
                    .lineWidth(0.01f)
                    .colored(dir.getColor());
            }
        }
    }

    private void drawFizzles(ClientLevel world, ElectricNode indicated) {

        ParticleSpawner poofParticle = 
        ParticleBuilder.ofType(new DustParticleOptions(indicated.getColor().asVectorF(), 1f))
            .density(1)
            .size(1)
            .randomness(0.08f)
            .cooldown(20)
            .build();

        poofParticle.setPos(indicated.getPosition());
        poofParticle.spawnAsClient(world);
    }

    private ElectricNode getIndicatedNode(ClientLevel world, ItemStack stack) {
        WireNodeBlockEntity target = getBoundTarget(world, stack);
        if(target == null) return null;
        int id = stack.getTag().getInt("from");
        if(id == -1) return null;
        return target.nodeBank.get(id);
    }

    @Nullable
    private WireNodeBlockEntity getBoundTarget(ClientLevel world, ItemStack wireStack) {
        if(!(wireStack.getItem() instanceof WireSpool) || wireStack.getTag() == null) return null;
        CompoundTag nbt = wireStack.getTag().getCompound("at");
        BlockPos targetPos = new BlockPos(
            nbt.getInt("x"),
            nbt.getInt("y"),
            nbt.getInt("z")
        );

        if(world.getBlockEntity(targetPos) instanceof WireNodeBlockEntity ebe)
            return ebe;
        return null;
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
