package com.quattage.mechano.content.block.Alternator.Stator;

import java.util.Locale;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.mrh0.createaddition.shapes.CAShapes;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.core.placement.StatorDirectionalHelper;
import com.quattage.mechano.core.placement.StrictOrientation;
import com.quattage.mechano.registry.MechanoBlockEntities;
import com.quattage.mechano.registry.MechanoBlocks;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementOffset;
import com.simibubi.create.foundation.placement.PoleHelper;
import com.simibubi.create.foundation.utility.VoxelShaper;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.Plane;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.accesstransformer.generated.AtParser.Return_valueContext;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class StatorBlock extends Block implements IBE<StatorBlockEntity>{

    public static final EnumProperty<StatorBlockModelType> MODEL_TYPE = EnumProperty.create("model", StatorBlockModelType.class);  // BASE or CORNER
    public static final EnumProperty<StrictOrientation> ORIENTATION = EnumProperty.create("orientation", StrictOrientation.class); //accomodates for up and down PER CARDINAL, ex. UP_NORTH, or DOWN_EAST
    public static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());
    public static final VoxelShaper BASE_SHAPE = CAShapes.shape(0, 0, 0, 16, 11, 16).forDirectional();
    public static final VoxelShaper CORNER_SHAPE = CAShapes.shape(0, 0, 0, 16, 11, 7).add(0, 9, 5, 16, 16, 16).forDirectional();

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
        }
        return CORNER_SHAPE.get(cardinal);
    }

    ////////
    public StatorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(MODEL_TYPE, StatorBlockModelType.BASE)
            .setValue(ORIENTATION, StrictOrientation.UP_X)
        );
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
        builder.add(ORIENTATION, MODEL_TYPE);
    }

	public boolean hasSegmentTowards(BlockPos pos, Direction dir, Level world) {
        return world.getBlockState(pos.relative(dir)).getBlock() == MechanoBlocks.ROTOR.get();
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
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        checkForCorner(world, pos, state);
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos,
            boolean isMoving) {
        checkForCorner(world, pos, state);
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
    }

    private void checkForCorner(Level world, BlockPos pos, BlockState state) {
        if(state.getValue(ORIENTATION).getOrient() == Axis.Y) return;
        // unimplemented for now
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction orientation = context.getClickedFace();
        Direction followingDir = getTriQuadrant(context, orientation, true);
    
        BlockState clickedBlock = context.getLevel().getBlockState(context.getClickedPos());

        if(orientation == followingDir) followingDir = context.getHorizontalDirection();
        if(orientation.getAxis() == followingDir.getAxis()) followingDir = followingDir.getClockWise();
        if(context.getPlayer().isCrouching()) orientation = orientation.getOpposite();

        Mechano.log("should be facing: " + orientation + " following " + followingDir.getAxis() + ", -> " + StrictOrientation.combine(orientation, followingDir.getAxis())); 
        return this.defaultBlockState().setValue(ORIENTATION, StrictOrientation.combine(orientation, followingDir.getAxis()));
    }

    private Direction getHalf(double x, boolean negateCenter) {
        if(negateCenter) {
            double cen = 0.3;
            if(x > cen && x < (1 - cen))
                return Direction.UP;
            if( x < cen) return Direction.WEST;
            return Direction.EAST;
        } 
        if(x > 0.5) return Direction.EAST;
        return Direction.WEST;
    }

    private Direction getTriQuadrant(BlockPlaceContext context, Direction orient, boolean negateCenter) {
        double x = 0;
        double y = 0;

        if(orient.getAxis() == Axis.Y) {
            y = context.getClickLocation().z - (double)context.getClickedPos().getZ();
            x = context.getClickLocation().x - (double)context.getClickedPos().getX();
        } else if(orient.getAxis() == Axis.Z) {
            y = context.getClickLocation().y - (double)context.getClickedPos().getY();
            x = context.getClickLocation().x - (double)context.getClickedPos().getX();
        } else if(orient.getAxis() == Axis.X) {
            y = context.getClickLocation().y - (double)context.getClickedPos().getY();
            x = context.getClickLocation().z - (double)context.getClickedPos().getZ();
        }

        double lineA = 1 * x + 0;
        double lineB = -1 * x + 1;

        if(negateCenter) {
            double cen = 0.3;
            if(x > cen && x < (1 - cen) && y > cen && y < (1 - cen))
                return orient;
        }

        // sorry
        if(orient == Direction.UP) {
            if(y <= lineA && y <= lineB) return Direction.NORTH;    // down
            if(y <= lineA && y >= lineB) return Direction.EAST;     // right
            if(y >= lineA && y >= lineB) return Direction.SOUTH;    // up
            if(y >= lineA && y <= lineB) return Direction.WEST;     // left
        } else if(orient == Direction.DOWN) {
            if(y <= lineA && y <= lineB) return Direction.NORTH;   
            if(y <= lineA && y >= lineB) return Direction.EAST; 
            if(y >= lineA && y >= lineB) return Direction.SOUTH;    
            if(y >= lineA && y <= lineB) return Direction.WEST;  
        } else if(orient == Direction.NORTH) {
            if(y <= lineA && y <= lineB) return Direction.DOWN;
            if(y <= lineA && y >= lineB) return Direction.EAST;
            if(y >= lineA && y >= lineB) return Direction.UP;
            if(y >= lineA && y <= lineB) return Direction.WEST;
        } else if(orient == Direction.SOUTH) {
            if(y <= lineA && y <= lineB) return Direction.DOWN;
            if(y <= lineA && y >= lineB) return Direction.WEST;
            if(y >= lineA && y >= lineB) return Direction.UP;
            if(y >= lineA && y <= lineB) return Direction.EAST;
        } else if(orient == Direction.EAST) {
            if(y <= lineA && y <= lineB) return Direction.DOWN;
            if(y <= lineA && y >= lineB) return Direction.NORTH;
            if(y >= lineA && y >= lineB) return Direction.UP;
            if(y >= lineA && y <= lineB) return Direction.SOUTH;
        } else if(orient == Direction.WEST) {
            if(y <= lineA && y <= lineB) return Direction.DOWN;
            if(y <= lineA && y >= lineB) return Direction.SOUTH;
            if(y >= lineA && y >= lineB) return Direction.UP;
            if(y >= lineA && y <= lineB) return Direction.NORTH;
        }
        return Direction.UP;
    }

    public static BlockState getStatorOrientation(BlockState originalState, BlockState stateForPlacement, Level level, BlockPos pos) {
		return stateForPlacement.setValue(ORIENTATION, originalState.getValue(ORIENTATION));
	}

    @MethodsReturnNonnullByDefault
	private static class PlacementHelper extends StatorDirectionalHelper<StrictOrientation> {
		// co-opted from Create's shaft placement helper, but this uses StrictOrientation instead of simple Axis

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
