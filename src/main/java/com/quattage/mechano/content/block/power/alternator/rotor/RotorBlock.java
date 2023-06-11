package com.quattage.mechano.content.block.power.alternator.rotor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import com.google.common.base.Predicates;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.registry.MechanoBlockEntities;
import com.quattage.mechano.registry.MechanoBlocks;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PlacementOffset;
import com.simibubi.create.foundation.placement.PoleHelper;

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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class RotorBlock extends RotatedPillarKineticBlock implements IBE<RotorBlockEntity> {

    public static final EnumProperty<RotorModelType> MODEL_TYPE = EnumProperty.create("model", RotorModelType.class);
    public static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

    public RotorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(defaultBlockState().setValue(MODEL_TYPE, RotorModelType.SINGLE));
    }

    public enum RotorModelType implements StringRepresentable {
        SINGLE, MIDDLE, ENDA, ENDB;

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
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        ArrayList<BlockState> adjacentBlockStates = getAdjacentShaftBlocks(state.getValue(AXIS), pos, world); // front block = 0, rear block = 1
        String neighborStatus = getRotorStatus(adjacentBlockStates);
        if(neighborStatus == "BOTH")
            setModel(world, pos, state, RotorModelType.MIDDLE);
        else if(neighborStatus == "FRONT")
            setModel(world, pos, state, RotorModelType.ENDB);
        else if(neighborStatus == "REAR")
            setModel(world, pos, state, RotorModelType.ENDA);
        else if(neighborStatus == "NONE")
            setModel(world, pos, state, RotorModelType.SINGLE);
    }

    private void setModel(Level world, BlockPos pos, BlockState state, RotorModelType bType) {
        if(state.getValue(MODEL_TYPE) != bType) {
            world.setBlock(pos, state.setValue(MODEL_TYPE, bType), Block.UPDATE_ALL);
        }
    }

    //FRONT, REAR, BOTH, NONE;
    private String getRotorStatus(ArrayList<BlockState> adjacentBlockStates) {
        boolean hasFrontRotor = adjacentBlockStates.get(0).getBlock() == MechanoBlocks.ROTOR.get();
        boolean hasRearRotor = adjacentBlockStates.get(1).getBlock() == MechanoBlocks.ROTOR.get();
        
        if(hasFrontRotor && hasRearRotor)
            return "BOTH";
        if(hasFrontRotor && !hasRearRotor)
            return "FRONT";
        if(!hasFrontRotor && hasRearRotor)
            return "REAR";
        return "NONE";
    }

    private ArrayList<BlockState> getAdjacentShaftBlocks(Axis rotationAxis, BlockPos currentPos, Level world) {
        ArrayList<BlockState> out = new ArrayList<BlockState>(2);
        if(rotationAxis == Axis.Z) {
            out.add(world.getBlockState(currentPos.north()));
            out.add(world.getBlockState(currentPos.south()));
        } else if(rotationAxis == Axis.X) {
            out.add(world.getBlockState(currentPos.east()));
            out.add(world.getBlockState(currentPos.west()));
        } else {
            return null;
        }
        return out;
    }

    @Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult ray) {
        if (player.isShiftKeyDown() || !player.mayBuild()) return InteractionResult.PASS;

        IPlacementHelper helper = PlacementHelpers.get(placementHelperId);
        ItemStack heldItem = player.getItemInHand(hand);

        if (helper.matchesItem(heldItem))
            return helper.getOffset(player, world, state, pos, ray)
                .placeInWorld(world, (BlockItem) heldItem.getItem(), player, hand, ray);

        return InteractionResult.PASS;
    }

    @Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		return face.getAxis() == state.getValue(AXIS);
	}

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {    // This makes sure rotors can't be placed on the Y axis. Remove this for vertical alternators
        Axis ax = state.getValue(AXIS); 
        if(ax == Axis.Y)
            return false;
        return super.canSurvive(state, world, pos);
    }

    @Override
    public Class<RotorBlockEntity> getBlockEntityClass() {
        return RotorBlockEntity.class;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MODEL_TYPE);
    }

    @Override
    public BlockEntityType<? extends RotorBlockEntity> getBlockEntityType() {
        return MechanoBlockEntities.ROTOR.get();
    }    

    @MethodsReturnNonnullByDefault
	private static class PlacementHelper extends PoleHelper<Direction.Axis> {
		private PlacementHelper() {
			super(state -> state.getBlock() instanceof RotorBlock, state -> state.getValue(AXIS), AXIS);
		}

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return i -> i.getItem() instanceof BlockItem
				&& ((BlockItem) i.getItem()).getBlock() instanceof RotorBlock;
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return Predicates.or(MechanoBlocks.ROTOR::has);
		}

		@Override
		public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos,
			BlockHitResult ray) {
			PlacementOffset offset = super.getOffset(player, world, state, pos, ray);
			if (offset.isSuccessful()) {
				offset.withTransform(offset.getTransform()
					.andThen(newState -> ShaftBlock.pickCorrectShaftType(newState, world, offset.getBlockPos())));
            }
			return offset;
		}
	}
}
