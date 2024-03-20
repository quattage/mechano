package com.quattage.mechano.content.block.power.alternator.stator;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.quattage.mechano.MechanoBlockEntities;
import com.quattage.mechano.MechanoBlocks;
import com.quattage.mechano.content.block.power.alternator.rotor.RotorBlockEntity;
import com.quattage.mechano.foundation.block.SimpleOrientedBlock;
import com.quattage.mechano.foundation.block.orientation.SimpleOrientation;
import com.quattage.mechano.foundation.helper.ShapeBuilder;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PlacementOffset;
import com.simibubi.create.foundation.utility.VoxelShaper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Locale;

public class StatorBlock extends SimpleOrientedBlock implements IBE<StatorBlockEntity> {

    public static final EnumProperty<StatorBlockModelType> MODEL_TYPE = EnumProperty.create("model", StatorBlockModelType.class);  // BASE or CORNER
    public static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());
    public static final VoxelShaper BASE_SHAPE = ShapeBuilder.newShape(0, 0, 0, 16, 11, 16).defaultUp();
    public static final VoxelShaper CORNER_SHAPE = ShapeBuilder.newShape(0, 0, 0, 16, 11, 7).add(0, 9, 5, 16, 16, 16).defaultUp();

    public enum StatorBlockModelType implements StringRepresentable {
        BASE, CORNER;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        @Override
        public String toString() {
            return getSerializedName();
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Axis orient = state.getValue(ORIENTATION).getOrient();
        Direction cardinal = state.getValue(ORIENTATION).getCardinal();
        if(state.getValue(MODEL_TYPE) == StatorBlockModelType.BASE) {
            return BASE_SHAPE.get(cardinal);
        } // TODO ADDRESS WEIRDNESS
        return CORNER_SHAPE.get(cardinal);
    }

    ////////
    public StatorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
            .setValue(MODEL_TYPE, StatorBlockModelType.BASE)
        );
    }

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        updateRotorsAround(pLevel, pPos, pState);
        super.onPlace(pState, pLevel, pPos, pOldState, pIsMoving);
    }



    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
        updateRotorsAround(pLevel, pPos,pState);
    }

    private void updateRotorsAround(Level pLevel, BlockPos pPos, BlockState pState) {
        if(pLevel == null) return;
        Axis axis = pState.getValue(ORIENTATION).getOrient();
        BlockPos corner1 = pPos.offset(axis == Axis.Z ? 1 : 0,1, axis == Axis.X ? 1 : 0);
        BlockPos corner2 =  pPos.offset(axis == Axis.Z ? -1 : 0,-1, axis == Axis.X ? -1 : 0);
        BlockPos.betweenClosed(corner1, corner2).forEach(pos -> {
            if (pLevel.getBlockEntity(pos) instanceof RotorBlockEntity rotor)
                rotor.updateStatorCount();
        });
    }


    @Override
    public Class<StatorBlockEntity> getBlockEntityClass() {
        return StatorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends StatorBlockEntity> getBlockEntityType() {
        return MechanoBlockEntities.STATOR.get();
    }

    @Override
    public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 1f;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MODEL_TYPE);
    }
    
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult ray) {
        IPlacementHelper helper = PlacementHelpers.get(placementHelperId);

        ItemStack heldItem = player.getItemInHand(hand);
		if (helper.matchesItem(heldItem))
			return helper.getOffset(player, world, state, pos, ray)
				.placeInWorld(world, (BlockItem) heldItem.getItem(), player, hand, ray);

		return InteractionResult.PASS;
	}

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if(state.getValue(MODEL_TYPE) == StatorBlockModelType.CORNER)
            return InteractionResult.PASS;
        return super.onWrenched(state, context);
    }

    public boolean hasSegmentTowards(BlockPos pos, Direction dir, Level world) {
        return world.getBlockState(pos.relative(dir)).getBlock() == MechanoBlocks.ROTOR.get();
	}

    public static BlockState getStatorOrientation(BlockState originalState, BlockState stateForPlacement, Level level, BlockPos pos) {
		return stateForPlacement.setValue(ORIENTATION, originalState.getValue(ORIENTATION));
	}

    @MethodsReturnNonnullByDefault
	private static class PlacementHelper extends StatorDirectionalHelper<SimpleOrientation> {
		// co-opted from Create's shaft placement helper, but this uses SimpelOrientation instead of DirectionalAxis

		private PlacementHelper() {
			super(state -> state.getBlock() instanceof StatorBlock, state -> state.getValue(ORIENTATION), ORIENTATION);
		}

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return i -> i.getItem() instanceof BlockItem
				&& ((BlockItem) i.getItem()).getBlock() instanceof StatorBlock;
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return Predicates.or(MechanoBlocks.STATOR::has);
		}

		@Override
		public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos,
			BlockHitResult ray) {
			PlacementOffset offset = super.getOffset(player, world, state, pos, ray);
			if (offset.isSuccessful()) {
				offset.withTransform(offset.getTransform());
            }
			return offset;
		}
	}
}
