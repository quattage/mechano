package com.quattage.mechano.content.block.power.alternator.stator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import com.quattage.mechano.MechanoBlocks;
import com.quattage.mechano.content.block.power.alternator.rotor.RotorBlock;
import com.quattage.mechano.content.block.power.alternator.stator.StatorBlock.StatorBlockModelType;
import com.quattage.mechano.foundation.block.SimpleOrientedBlock;
import com.quattage.mechano.foundation.block.orientation.SimpleOrientation;
import com.quattage.mechano.foundation.helper.shape.CircleGetter;
import com.quattage.mechano.foundation.helper.shape.ShapeGetter;
import com.simibubi.create.content.equipment.extendoGrip.ExtendoGripItem;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementOffset;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;

@MethodsReturnNonnullByDefault
public class StatorDirectionalHelper<T extends Comparable<T>> implements IPlacementHelper {

    protected final Predicate<BlockState> statePredicate;
	protected final Property<T> property;
	protected final Function<BlockState, SimpleOrientation> strictDirFunc;
	protected BlockPos straightPos = null;

    public StatorDirectionalHelper(Predicate<BlockState> statePredicate, Function<BlockState, SimpleOrientation> strictDirFunc, Property<T> property) {
		this.statePredicate = statePredicate;
		this.strictDirFunc = strictDirFunc;
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

    public boolean isMatchingOrient(BlockState state, SimpleOrientation dir) {
		if (!statePredicate.test(state))
			return false;

		return strictDirFunc.apply(state) == dir;
	}

    public int getAttachedStrictBlocks(Level world, BlockPos pos, Direction dirToLook) {
		BlockPos checkPos = pos.relative(dirToLook);
		BlockState destState = world.getBlockState(checkPos);
		int count = 0;
		while(getStateOrFalse(destState)) {
			count++;
			checkPos = checkPos.relative(dirToLook);
			destState = world.getBlockState(checkPos);
		}
		return count;
	}

    private boolean getStateOrFalse(BlockState state) {
        boolean out = false;
        try {
            out = isMatchingOrient(state, state.getValue(SimpleOrientedBlock.ORIENTATION));
        }
        catch(Exception e) {}
        return out;
    }

    @Override
    public PlacementOffset getOffset(Player player, Level world, BlockState statorState, BlockPos statorPos, BlockHitResult ray) {
		BlockPos frontPos = statorPos.relative(statorState.getValue(SimpleOrientedBlock.ORIENTATION).getCardinal());
		BlockState frontState = world.getBlockState(frontPos);
		if(frontState.getBlock() == MechanoBlocks.ROTOR.get() && statorState.getValue(SimpleOrientedBlock.ORIENTATION).getOrient() == frontState.getValue(RotorBlock.AXIS)) {
			PlacementOffset checkRotor = getRotoredOffset(world, frontPos, frontState, statorPos, statorState);	
			if(checkRotor.isSuccessful()) return checkRotor;
		} 
		return getFreehandOffset(player, world, statorState, statorPos, ray);
	}

	private PlacementOffset getRotoredOffset(Level world, BlockPos rotorPos, BlockState rotorState, BlockPos statorPos, BlockState statorState) {
		Axis revolvingAxis = rotorState.getValue(RotorBlock.AXIS);
		Direction initialDir = statorState.getValue(SimpleOrientedBlock.ORIENTATION).getCardinal();
		Axis initialAxis = statorState.getValue(SimpleOrientedBlock.ORIENTATION).getOrient();


		ShapeGetter circle = 
			ShapeGetter.ofShape(CircleGetter.class)
				.at(world, rotorPos)
				.withRadius(1)
				.onAxis(revolvingAxis).build();
		
		circle.compute();
		
		for(Map.Entry<BlockPos, BlockState> entry : circle.getBlocks()) {
			
		}

		return PlacementOffset.fail();
	}

    public Direction getDirectionTo(Level world, BlockPos fromPos, BlockPos centerPos, Axis rotationAxis) {
        Direction[] directions = getAxisDirections(rotationAxis);
		for (Direction dir : directions) {
            BlockPos checkPos = fromPos.relative(dir);
            if(checkPos.equals(centerPos)) return dir;
        }
        return null;
    }

    public static Direction[] getAxisDirections(Axis axis) {
        Direction[] out = new Direction[4];
        if(axis == Axis.Z) {
            out[0] = Direction.DOWN;
            out[1] = Direction.EAST;
            out[2] = Direction.UP;
            out[3] = Direction.WEST;
        } else if(axis == Axis.Y) {
            out[0] = Direction.NORTH;
            out[1] = Direction.EAST;
            out[2] = Direction.SOUTH;
            out[3] = Direction.WEST;
        } else {
            out[0] = Direction.DOWN;
            out[1] = Direction.NORTH;
            out[2] = Direction.UP;
            out[3] = Direction.SOUTH;
        }
        return out;
    }



	private PlacementOffset getFreehandOffset(Player player, Level world, BlockState strictState, BlockPos pos, BlockHitResult ray) {
		List<Direction> directions = getDirectionsForPlacement(strictState, pos, ray);
		for (Direction dir : directions) {
			int range = AllConfigs.server().equipment.placementAssistRange.get();
			if (player != null) {
				AttributeInstance reach = player.getAttribute(ForgeMod.BLOCK_REACH.get());
				if (reach != null && reach.hasModifier(ExtendoGripItem.singleRangeAttributeModifier))
					range += 4;
			}
			int strictBlocks = getAttachedStrictBlocks(world, pos, dir);
			if (strictBlocks >= range)
				continue;

			BlockPos newPos = pos.relative(dir, strictBlocks + 1);
			BlockState newState = world.getBlockState(newPos);

			if (newState.canBeReplaced())
				return PlacementOffset.success(newPos, bState -> bState.setValue(property, strictState.getValue(property)).setValue(StatorBlock.MODEL_TYPE, strictState.getValue(StatorBlock.MODEL_TYPE)));
		}
		return PlacementOffset.fail();
	}

	private List<Direction> getDirectionsForPlacement(BlockState state, BlockPos pos, BlockHitResult ray) {
		return IPlacementHelper.orderedByDistance(pos, ray.getLocation(), state.getValue(SimpleOrientedBlock.ORIENTATION).getOrient());
	}

	private static <T> void reverseList(List<T> list) {
		if (list.size() <= 1 || list == null)
            return;
        T value = list.remove(0);
        reverseList(list);
        list.add(value);
	}
}
