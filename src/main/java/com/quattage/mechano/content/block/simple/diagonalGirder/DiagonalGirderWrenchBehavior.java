package com.quattage.mechano.content.block.simple.diagonalGirder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.content.block.simple.diagonalGirder.DiagonalGirderBlock.GirderPartial;
import com.quattage.mechano.core.events.ClientBehavior;
import com.quattage.mechano.registry.MechanoBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3; 

public class DiagonalGirderWrenchBehavior extends ClientBehavior {

    public DiagonalGirderWrenchBehavior(String name) {
        super(name);
    }

    @Override
    public boolean shouldTick(ClientLevel world, Player player, ItemStack mainHand, ItemStack offHand,
            Vec3 lookingPosition, BlockPos lookingBlockPos) {    
        return AllItems.WRENCH.isIn(mainHand) && 
            MechanoBlocks.DIAGONAL_GIRDER.has(world.getBlockState(lookingBlockPos)) &&
            !isShifting();
    }

    @Override
    public void tickSafe(ClientLevel world, Player player, ItemStack mainHand, ItemStack offHand, Vec3 lookingPosition,
            BlockPos lookingBlockPos) {
        BlockState girderState = world.getBlockState(lookingBlockPos);
        DiagonalGirderBlock girderBlock = ((DiagonalGirderBlock)girderState.getBlock());
        List<Pair<AABB, GirderPartial>> possibleShapes = girderBlock.getRelevantPartials(girderState);    
        Pair<AABB, GirderPartial> shapeCheck = getClosest(lookingBlockPos, lookingPosition, possibleShapes);

        if(shapeCheck != null) {
            CreateClient.OUTLINER.showAABB("diagonalGirderWrench", shapeCheck.getFirst().move(lookingBlockPos))
                .lineWidth(1 / 32f)
                .colored(new Color(127, 127, 127));
        }
    }

    @Nullable
    private Pair<AABB, GirderPartial> getClosest(BlockPos pos, Vec3 hit, List<Pair<AABB, GirderPartial>> hitboxes) {
        if(hitboxes.isEmpty()) return null;

        Pair<AABB, GirderPartial> out = null;
        double lastDist = 10000;
		for(Pair<AABB, GirderPartial> partial : hitboxes) {
            AABB box = partial.getFirst();
            box = box.move(pos);
            double dist = Math.abs(hit.distanceTo(box.getCenter()));

            if(dist > 0.6) continue;
            if(dist < 0.0001) {
                return partial;
            }

            if(dist < lastDist) {
                out = partial;
            }
            
            lastDist = dist;
        }
        return out;
	}

    public boolean handleClick(Level world, BlockPos pos, BlockState state, BlockHitResult ray, BlockEntity entity) {
        BlockState girderState = world.getBlockState(pos);
        DiagonalGirderBlock girderBlock = ((DiagonalGirderBlock)girderState.getBlock());
        List<Pair<AABB, GirderPartial>> possibleShapes = girderBlock.getRelevantPartials(girderState);    
        Pair<AABB, GirderPartial> shapeCheck = getClosest(pos, ray.getLocation(), possibleShapes);

        if(shapeCheck == null) return false;
        if(entity == null) return false;
        if (!MechanoBlocks.DIAGONAL_GIRDER.has(world.getBlockState(pos))) 
            return false;
        if(!(entity instanceof DiagonalGirderBlockEntity)) return false;
        
        switch(shapeCheck.getSecond()) {
            case LONG_DOWN_FLAT:
            case SHORT_DOWN_FLAT:
                ((DiagonalGirderBlockEntity)entity).showUpVert = 
                    !((DiagonalGirderBlockEntity)entity).showUpVert;
                entity.setChanged();
                return true;
            case LONG_DOWN_VERT:
            case SHORT_DOWN_VERT:
                ((DiagonalGirderBlockEntity)entity).showUpFlat = 
                    !((DiagonalGirderBlockEntity)entity).showUpFlat;
                entity.setChanged();
                return true;
            case LONG_UP_FLAT:
            case SHORT_UP_FLAT:
                ((DiagonalGirderBlockEntity)entity).showDownVert = 
                    !((DiagonalGirderBlockEntity)entity).showDownVert;
                entity.setChanged();
                return true;
            case LONG_UP_VERT:
            case SHORT_UP_VERT:
                ((DiagonalGirderBlockEntity)entity).showDownFlat = 
                    !((DiagonalGirderBlockEntity)entity).showDownFlat;
                entity.setChanged();
                return true;
            default:
                return false;
        }
    }
}
