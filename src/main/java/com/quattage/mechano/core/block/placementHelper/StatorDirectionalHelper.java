package com.quattage.mechano.core.block.placementHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.block.power.alternator.rotor.RotorBlock;
import com.quattage.mechano.content.block.power.alternator.stator.StatorBlock;
import com.quattage.mechano.content.block.power.alternator.stator.StatorBlock.StatorBlockModelType;
import com.quattage.mechano.core.block.SimpleOrientedBlock;
import com.quattage.mechano.core.block.orientation.SimpleOrientation;
import com.quattage.mechano.core.util.BlockMath;
import com.quattage.mechano.registry.MechanoBlocks;
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
import net.minecraft.world.level.block.Blocks;
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
		List<BlockPos> circleMembers = getCircle(statorPos, rotorPos, initialDir, revolvingAxis); 
		if(initialDir.getAxis() == Axis.X) reverseList(circleMembers);
		if(initialDir.getAxis() == Axis.Y && initialAxis == Axis.Z) reverseList(circleMembers);    // i have no idea why this works but it does
		int count = 0;
		for(BlockPos pos : circleMembers) {
			count++;
			BlockState targetState = world.getBlockState(pos);
			if(targetState.getBlock() == MechanoBlocks.STATOR.get()) continue;
			if(!targetState.getMaterial().isReplaceable()) return PlacementOffset.fail();
			
			if(count % 2 == 0) {  // every other placement is a corner
				straightPos = new BlockPos(pos.getX(), pos.getY(), pos.getZ());
				Direction dirToMiddle = BlockMath.getDirectionTo(world, pos, rotorPos, revolvingAxis);
				return PlacementOffset.success(pos, s -> s.setValue(StatorBlock.ORIENTATION, SimpleOrientation.combine(dirToMiddle, revolvingAxis)));
			}
			else {
				if(count == 1) {
					straightPos = new BlockPos(pos.getX(), pos.getY(), pos.getZ());
					return PlacementOffset.success(pos, s -> s.setValue(StatorBlock.ORIENTATION, SimpleOrientation.combine(initialDir, revolvingAxis)).setValue(StatorBlock.MODEL_TYPE, StatorBlockModelType.CORNER));
				}
				else {
					if(straightPos != null) {
						Direction dirToMiddle = BlockMath.getDirectionTo(world, straightPos, rotorPos, revolvingAxis);
						if(dirToMiddle != null) 
							return PlacementOffset.success(pos, s -> s.setValue(StatorBlock.ORIENTATION, SimpleOrientation.combine(dirToMiddle, revolvingAxis)).setValue(StatorBlock.MODEL_TYPE, StatorBlockModelType.CORNER));
					}
				}
			}  
		}
		return PlacementOffset.fail();
	}

	private PlacementOffset getFreehandOffset(Player player, Level world, BlockState strictState, BlockPos pos, BlockHitResult ray) {
		List<Direction> directions = getDirectionsForPlacement(strictState, pos, ray);
		for (Direction dir : directions) {
			int range = AllConfigs.server().equipment.placementAssistRange.get();
			if (player != null) {
				AttributeInstance reach = player.getAttribute(ForgeMod.REACH_DISTANCE.get());
				if (reach != null && reach.hasModifier(ExtendoGripItem.singleRangeAttributeModifier))
					range += 4;
			}
			int strictBlocks = getAttachedStrictBlocks(world, pos, dir);
			if (strictBlocks >= range)
				continue;

			BlockPos newPos = pos.relative(dir, strictBlocks + 1);
			BlockState newState = world.getBlockState(newPos);

			if (newState.getMaterial().isReplaceable())
				return PlacementOffset.success(newPos, bState -> bState.setValue(property, strictState.getValue(property)).setValue(StatorBlock.MODEL_TYPE, strictState.getValue(StatorBlock.MODEL_TYPE)));
		}
		return PlacementOffset.fail();
	}

	private List<Direction> getDirectionsForPlacement(BlockState state, BlockPos pos, BlockHitResult ray) {
		return IPlacementHelper.orderedByDistance(pos, ray.getLocation(), state.getValue(SimpleOrientedBlock.ORIENTATION).getOrient());
	}

	private List<BlockPos> getCircle(BlockPos start, BlockPos center, Direction initialDir, Axis revolvingAxis) {
		List<BlockPos> output = new ArrayList<BlockPos>();

		double angleInc = 2 * Math.PI / 8;
		double currentAngle = 0;

		if(initialDir == Direction.NORTH || initialDir == Direction.WEST) currentAngle = Math.toRadians(45);
		else if(initialDir == Direction.SOUTH || initialDir == Direction.EAST) currentAngle = Math.toRadians(225); 
		else if(initialDir == Direction.UP) currentAngle = Math.toRadians(-45);
		else if(initialDir == Direction.DOWN) currentAngle = Math.toRadians(135); 

		double radius = BlockMath.getSimpleDistance(start, center);

		int count = 0;
		while(count < 7) {
			Vec3 possy = new Vec3(0, 0, 0);
			if(revolvingAxis == Axis.X) { 
				double z = center.getZ() + radius * Math.cos(currentAngle);
				double y = center.getY() + radius * Math.sin(currentAngle);

				possy = new Vec3(start.getX(), y, z);
			}
			else if(revolvingAxis == Axis.Z) {
				double x = center.getX() + radius * Math.cos(currentAngle);
				double y = center.getY() + radius * Math.sin(currentAngle);
				possy = new Vec3(x, y, start.getZ());
			}
			currentAngle += angleInc;
			BlockPos out = new BlockPos(Math.round(possy.x), Math.round(possy.y), Math.round(possy.z));
			output.add(out);
			count++;
		}
		return output;
	}

	private static <T> void reverseList(List<T> list) {
		if (list.size() <= 1 || list == null)
            return;
        T value = list.remove(0);
        reverseList(list);
        list.add(value);
	}
}
