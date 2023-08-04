package com.quattage.mechano.core.block.placementHelper;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import com.quattage.mechano.content.block.simple.diagonalGirder.DiagonalGirderBlock;
import com.simibubi.create.content.equipment.extendoGrip.ExtendoGripItem;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementOffset;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ForgeMod;

public class GirderDirectionalHelper<T extends Comparable<T>> implements IPlacementHelper {

    protected final Predicate<BlockState> statePredicate;
    protected final Property<T> property;
    protected final Function<BlockState, Direction> dirFunc;

    public GirderDirectionalHelper(Predicate<BlockState> statePredicate, Function<BlockState, Direction> dirFunc, Property<T> property) {
        this.statePredicate = statePredicate;
        this.dirFunc = dirFunc;
        this.property = property;
    }

    @Override
    public Predicate<ItemStack> getItemPredicate() {
        return i -> i.getItem() instanceof BlockItem
            && ((BlockItem) i.getItem()).getBlock() instanceof Block;
    }

    @Override
    public Predicate<BlockState> getStatePredicate() {
        return this.statePredicate;
    }

    public boolean isMatchingOrient(BlockState state, Direction dir) {
		if (!statePredicate.test(state)) return false;
		return dirFunc.apply(state) == dir;
	}

    public int getAttachedPoles(Level world, BlockPos pos, Direction dirToLook, BlockState originState) { // up or down
		BlockPos checkPos = stepForward(pos, originState, dirToLook);
		BlockState destState = world.getBlockState(checkPos);
		int count = 0;
		while(getStateOrFalse(destState)) {
            count++;
			checkPos = stepForward(checkPos, originState, dirToLook);
			destState = world.getBlockState(checkPos);
            if(count > 50) break; // TODO remove
		}
		return count;
	}

    private BlockPos stepForward(BlockPos pos, BlockState state, Direction dirToLook) {
        Direction relativeDirection = state.getValue(DirectionalBlock.FACING);
        if(relativeDirection == dirToLook) 
            return getOver(pos, state);
        return getUnder(pos, state);
    }


    private BlockPos stepForward(BlockPos pos, BlockState state, Direction dirToLook, int distance) {
        Direction relativeDirection = state.getValue(DirectionalBlock.FACING);
        if(relativeDirection == dirToLook) 
            return getOver(pos, state, distance);
        return getUnder(pos, state, distance);
    }


    private boolean getStateOrFalse(BlockState state) {
        boolean out = false;
        try {out = isMatchingOrient(state, state.getValue(DirectionalBlock.FACING));}
        catch(Exception e) {}
        return out;
    }

    @Override
    public PlacementOffset getOffset(Player player, Level world, BlockState poleState, BlockPos polePos, BlockHitResult ray) {
        List<Direction> directions = getDirectionsForPlacement(poleState, polePos, ray);
        for (Direction dir : directions) {
            int range = AllConfigs.server().equipment.placementAssistRange.get();
            if (player != null) {
                AttributeInstance reach = player.getAttribute(ForgeMod.BLOCK_REACH.get());
                if (reach != null && reach.hasModifier(ExtendoGripItem.singleRangeAttributeModifier))
                    range += 4;
                }
            int attachedPoles = getAttachedPoles(world, polePos, dir, poleState);
            if (attachedPoles >= range)
                return PlacementOffset.fail();

            BlockPos newPos = stepForward(polePos, poleState, dir, attachedPoles + 1);
            BlockState newState = world.getBlockState(newPos);

            if (newState.canBeReplaced()) {
                Block poleBlock = poleState.getBlock();
                BlockState model;
                if(poleBlock instanceof DiagonalGirderBlock)
                    model = ((DiagonalGirderBlock)poleBlock).getDestinationState(world, newPos, poleState);
                else continue;
                return PlacementOffset.success(newPos, bState -> bState
                    .setValue(property, poleState.getValue(property))
                    .setValue(DiagonalGirderBlock.MODEL_TYPE, model.getValue(DiagonalGirderBlock.MODEL_TYPE)));
            }
        }
        return PlacementOffset.fail();
    }

    private List<Direction> getDirectionsForPlacement(BlockState state, BlockPos pos, BlockHitResult ray) {
		return IPlacementHelper.orderedByDistance(pos, ray.getLocation(), state.getValue(DirectionalBlock.FACING).getAxis());
	}


    private BlockPos getUnder(BlockPos mainPos, BlockState state) {
        Direction facing = state.getValue(DirectionalBlock.FACING);
        BlockPos offset = mainPos.relative(facing.getOpposite());
        return offset.relative(Direction.DOWN);
    }

    private BlockPos getUnder(BlockPos mainPos, BlockState state, int rep) {
        Direction facing = state.getValue(DirectionalBlock.FACING);
        BlockPos offset = mainPos.relative(facing.getOpposite(), rep);
        return offset.relative(Direction.DOWN, rep);
    }

    private BlockPos getOver(BlockPos mainPos, BlockState state) {
        Direction facing = state.getValue(DirectionalBlock.FACING);
        BlockPos offset = mainPos.relative(facing);
        return offset.relative(Direction.UP);
    }

    private BlockPos getOver(BlockPos mainPos, BlockState state, int rep) {
        Direction facing = state.getValue(DirectionalBlock.FACING);
        BlockPos offset = mainPos.relative(facing, rep);
        return offset.relative(Direction.UP, rep);
    }
}
