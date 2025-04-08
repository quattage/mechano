package com.quattage.mechano.content.block.power.alternator.rotor;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import com.quattage.mechano.MechanoBlockEntities;
import com.quattage.mechano.MechanoBlocks;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.placement.PoleHelper;

import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class SmallRotorBlock extends AbstractRotorBlock implements IBE<SmallRotorBlockEntity>{

    public static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

    public SmallRotorBlock(Properties properties) {
        super(properties);
    }

	@Override
	public BlockPos getParentPos(Level world, BlockPos pos, BlockState state) {
		return pos;
	}

    @Override
    public Class<SmallRotorBlockEntity> getBlockEntityClass() {
        return SmallRotorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SmallRotorBlockEntity> getBlockEntityType() {
        return MechanoBlockEntities.SMALL_ROTOR.get();
    }

    @Override
    boolean isRotor(Block block) {
        return block == MechanoBlocks.SMALL_ROTOR.get();
    }

    @Override
    protected int getPlacementHelperId() {
        return placementHelperId;
    }

    @MethodsReturnNonnullByDefault
	protected static class PlacementHelper extends PoleHelper<Direction.Axis> {

		protected PlacementHelper() {
			super(state -> state.getBlock() instanceof SmallRotorBlock, state -> state.getValue(AXIS), AXIS);
		}

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return i -> i.getItem() instanceof BlockItem
				&& ((BlockItem) i.getItem()).getBlock() instanceof SmallRotorBlock;
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return Predicates.or(MechanoBlocks.SMALL_ROTOR::has);
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