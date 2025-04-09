package com.quattage.mechano.content.block.power.alternator.stator;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import com.quattage.mechano.content.block.power.alternator.rotor.AbstractRotorBlock;
import com.quattage.mechano.content.block.power.alternator.rotor.BlockRotorable;
import com.quattage.mechano.foundation.block.SimpleOrientedBlock;
import com.quattage.mechano.foundation.block.orientation.SimpleOrientation;
import com.quattage.mechano.foundation.helper.shape.CircleGetter;
import com.quattage.mechano.foundation.helper.shape.ShapeGetter;
import com.simibubi.create.content.equipment.extendoGrip.ExtendoGripItem;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ForgeMod;

@MethodsReturnNonnullByDefault
public class StatorDirectionalHelper<T extends Enum<T> & StringRepresentable & StatorTypeTransformable<?>> implements IPlacementHelper {

    protected final Predicate<BlockState> statePredicate;
	protected final Function<BlockState, SimpleOrientation> strictDirFunc;
	protected BlockPos straightPos = null;
	protected final ShapeGetter circleGetter;
	protected final EnumProperty<T> modelType;

    public StatorDirectionalHelper(int radius, Predicate<BlockState> statePredicate, Function<BlockState, SimpleOrientation> strictDirFunc, EnumProperty<T> modelType) {
		this.statePredicate = statePredicate;
		this.strictDirFunc = strictDirFunc;
		this.modelType = modelType;
		circleGetter = ShapeGetter.ofShape(CircleGetter.class).withRadius(radius).build();
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
		SimpleOrientation orient = statorState.getValue(SimpleOrientedBlock.ORIENTATION);
		BlockPos frontPos = statorPos.relative(orient.getCardinal());

		BlockState frontState = world.getBlockState(frontPos);
		if(frontState.getBlock() instanceof BlockRotorable br 
			&& orient.getOrient() == br.getRotorAxis(frontState)) {

			PlacementOffset checkRotor = getRotoredOffset(world, 
				br.getParentPos(world, frontPos, frontState), 	
				frontState, statorPos, statorState, player, ray);	

			if(checkRotor.isSuccessful()) return checkRotor;
		} 
		return getFreehandOffset(player, world, statorState, statorPos, ray);
	}

	private PlacementOffset getRotoredOffset(Level world, BlockPos rotorPos, BlockState rotorState, BlockPos statorPos, BlockState statorState, Player player, BlockHitResult ray) {
		Axis revolvingAxis = rotorState.getValue(AbstractRotorBlock.AXIS);

		return circleGetter.moveTo(rotorPos).setAxis(revolvingAxis).evaluatePlacement(perimeterPos -> {

			final BlockState targetState = world.getBlockState(perimeterPos);
			if(targetState.getBlock() instanceof AbstractStatorBlock) return null;
			if(!targetState.canBeReplaced()) return PlacementOffset.fail();

			return PlacementOffset.success(perimeterPos, state -> 
				((AbstractStatorBlock<?>)statorState.getBlock())
					.getInitialState(world, perimeterPos, statorState, revolvingAxis, false));
		});
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

			if(newState.canBeReplaced())
				return PlacementOffset.success(newPos, bState -> 
					bState.setValue(SimpleOrientedBlock.ORIENTATION, 
						strictState.getValue(SimpleOrientedBlock.ORIENTATION))
						.setValue(modelType, strictState
							.getValue(modelType)));
		}
		return PlacementOffset.fail();
	}

	private List<Direction> getDirectionsForPlacement(BlockState state, BlockPos pos, BlockHitResult ray) {
		return IPlacementHelper.orderedByDistance(pos, ray.getLocation(), state.getValue(SimpleOrientedBlock.ORIENTATION).getOrient());
	}
}