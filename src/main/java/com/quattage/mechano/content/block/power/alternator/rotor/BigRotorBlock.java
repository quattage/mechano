package com.quattage.mechano.content.block.power.alternator.rotor;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import com.quattage.mechano.MechanoBlockEntities;
import com.quattage.mechano.MechanoBlocks;
import com.quattage.mechano.content.block.power.alternator.rotor.dummy.BigRotorDummyBlock;
import com.quattage.mechano.content.block.power.alternator.rotor.dummy.BigRotorDummyBlockEntity;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.placement.PoleHelper;

import net.createmod.catnip.data.Pair;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class BigRotorBlock extends AbstractRotorBlock implements IBE<BigRotorBlockEntity> {

    public static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

    public BigRotorBlock(Properties properties) {
        super(properties);
    }

    @Override
	public BlockPos getParentPos(Level world, BlockPos pos, BlockState state) {
		return pos;
	}

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        return canPlaceDummies(world, pos, state);
    }

    protected boolean canPlaceDummies(LevelReader world, BlockPos pos, BlockState state) {
        Pair<BlockPos, BlockPos> corners = DirectionTransformer.getPositiveCorners(pos, state.getValue(AXIS));
        for(BlockPos vPos : BlockPos.betweenClosed(corners.getFirst(), corners.getSecond())) {
            if(!world.getBlockState(vPos).canBeReplaced())
                return false;
        }
        return true;
    }

    @Override
    public boolean isRotor(Block block) {
        return block instanceof BigRotorBlock;
    }

    @Override
    protected int getPlacementHelperId() {
        return placementHelperId;
    }

    @Override
    public void onBeforeBlockPlaced(Level world, BlockPos pos, BlockState currentState, BlockState futureState) {
        if(currentState.getBlock() != futureState.getBlock())
            placeDummies(world, pos, futureState);
    }


    protected void placeDummies(Level world, BlockPos pos, BlockState state) {

        Pair<BlockPos, BlockPos> corners = DirectionTransformer.getPositiveCorners(pos, state.getValue(AXIS));

        for(BlockPos vPos : BlockPos.betweenClosed(corners.getFirst(), corners.getSecond())) {
            if(!(world.getBlockState(vPos).getBlock() instanceof BigRotorBlock)) {
                BlockState dummy = MechanoBlocks.BIG_ROTOR_DUMMY.get().defaultBlockState();
                dummy = dummy.setValue(AXIS, state.getValue(AXIS));
                world.setBlock(vPos, dummy, 2);
                ((BigRotorDummyBlockEntity)world.getBlockEntity(vPos)).setParentPos(pos);
            }
        }
    }

    @Override
    public void onAfterBlockBroken(Level world, BlockPos pos, BlockState pastState, BlockState currentState) {
        if(pastState.getBlock() != currentState.getBlock())
            removeDummies(world, pos, pastState);
        super.onAfterBlockBroken(world, pos, pastState, currentState);
    }

    public void removeDummies(Level world, BlockPos pos, BlockState state) {
        Pair<BlockPos, BlockPos> corners = DirectionTransformer.getPositiveCorners(pos, state.getValue(AXIS));
        BlockPos.betweenClosed(corners.getFirst(), corners.getSecond()).forEach(vPos -> {
            if(world.getBlockState(vPos).getBlock() instanceof BigRotorDummyBlock)
                world.setBlock(vPos, Blocks.AIR.defaultBlockState(), 3);
        });
    }

    @Override
    public Class<BigRotorBlockEntity> getBlockEntityClass() {
        return BigRotorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BigRotorBlockEntity> getBlockEntityType() {
        return MechanoBlockEntities.BIG_ROTOR.get();
    }

    @MethodsReturnNonnullByDefault
	protected static class PlacementHelper extends PoleHelper<Direction.Axis> {

		protected PlacementHelper() {
			super(state -> state.getBlock() instanceof BigRotorBlock, state -> state.getValue(AXIS), AXIS);
		}

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return i -> i.getItem() instanceof BlockItem
				&& ((BlockItem) i.getItem()).getBlock() instanceof BigRotorBlock;
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return Predicates.or(MechanoBlocks.BIG_ROTOR::has);
		}

		@Override
		public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos,
                                         BlockHitResult ray) {
			PlacementOffset offset = super.getOffset(player, world, state, pos, ray);
			if(offset.isSuccessful()) {
				offset.withTransform(offset.getTransform()
					.andThen(newState -> ShaftBlock.pickCorrectShaftType(newState, world, offset.getBlockPos())));
            }
			return offset;
		}
	}
}