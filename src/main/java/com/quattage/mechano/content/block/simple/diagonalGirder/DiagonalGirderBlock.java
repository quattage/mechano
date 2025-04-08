package com.quattage.mechano.content.block.simple.diagonalGirder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.common.base.Predicate;
import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoClientEvents;
import com.quattage.mechano.foundation.block.hitbox.RotatableHitboxShape;
import com.quattage.mechano.foundation.helper.VoxelShapeBuilder;
import com.quattage.mechano.foundation.block.hitbox.HitboxNameable;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.block.IBE;

import net.createmod.catnip.data.Pair;
import net.createmod.catnip.math.VoxelShaper;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DiagonalGirderBlock extends DirectionalBlock implements IBE<DiagonalGirderBlockEntity> {
    public static final EnumProperty<DiagonalGirderModelType> MODEL_TYPE = EnumProperty.create("model", DiagonalGirderModelType.class);
    public static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());
    private static RotatableHitboxShape<Direction> hitbox;
    
    public static final VoxelShaper BOX_LONG_DOWN_FLAT =
        VoxelShaper.forDirectional(VoxelShapeBuilder.newBox(3, -4.5, -5.75, 13.1, 0, 5.25), Direction.UP);
    public static final VoxelShaper BOX_LONG_DOWN_VERT = 
        VoxelShaper.forDirectional(VoxelShapeBuilder.newBox(3.1, -5.5, -4.85, 13.2, 5.5, -0.35), Direction.UP);
    public static final VoxelShaper BOX_LONG_UP_FLAT = 
        VoxelShaper.forDirectional(VoxelShapeBuilder.newBox(3, 16, 10.75, 13.1, 20.5, 21.75), Direction.UP);
    public static final VoxelShaper BOX_LONG_UP_VERT = 
        VoxelShaper.forDirectional(VoxelShapeBuilder.newBox(3.1, 10.5, 16.35, 13.2, 21.5, 20.85), Direction.UP);

    public static final VoxelShaper BOX_SHORT_DOWN_FLAT = 
        VoxelShaper.forDirectional(VoxelShapeBuilder.newBox(3, 0, -1, 13.1, 4.5, 10), Direction.UP);
    public static final VoxelShaper BOX_SHORT_DOWN_VERT = 
        VoxelShaper.forDirectional(VoxelShapeBuilder.newBox(3, -1, 0, 13.1, 10, 4.5), Direction.UP);
    public static final VoxelShaper BOX_SHORT_UP_FLAT = 
        VoxelShaper.forDirectional(VoxelShapeBuilder.newBox(3, 11.5, 6, 13.1, 16, 17), Direction.UP);
    public static final VoxelShaper BOX_SHORT_UP_VERT = 
        VoxelShaper.forDirectional(VoxelShapeBuilder.newBox(3, 6, 11.5, 13.1, 17, 16), Direction.UP);


    // public static final VoxelShaper DOUBLE_DOWN = ShapeBuilder
    //     .shape(2.9, 7.15, 0, 13, 18.15, 4)
    //     .add()
    // .forDirectional();

    public enum DiagonalGirderModelType implements StringRepresentable, HitboxNameable {
        LONG_DOUBLE("long"),         // there are girders, walls, fences, etc. (non-full blocks) on both ends of the diagonal
        LONG_END_UP("long"),            // there's a girder, wall, fence, etc. on one end of the diagonal
        LONG_END_DOWN("long"),            // there's a girder, wall, fence, etc. on one end of the diagonal
        MIDDLE("middle"),              // there are other diagonal girders on both ends of the diagonal 
        SHORT_END_UP("short"),           // there's a full block on one end of the diagonal
        SHORT_END_DOWN("short"),           // there's a full block on one end of the diagonal
        SHORT_DOUBLE("short");        // there's a full block on both ends of the diagonal
        // CROSS; // Optional manually assigned X shape

        private final String hitboxName;

        private DiagonalGirderModelType(String hitboxName) {
            this.hitboxName = hitboxName;
        }

        @Override
        public String getHitboxName() {
            return hitboxName;
        }
    
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
        hitbox = Mechano.HITBOXES.get(FACING, state.getValue(MODEL_TYPE), this);
        return hitbox.getRotated(state.getValue(FACING));
    }

    public DiagonalGirderBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(defaultBlockState().setValue(MODEL_TYPE, DiagonalGirderModelType.SHORT_DOUBLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MODEL_TYPE).add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if(facing.getAxis() == Direction.Axis.Y)
            facing = context.getHorizontalDirection();

        if(context.getPlayer().isCrouching()) facing = facing.getOpposite();

        BlockState destination = getDestinationState(world, pos, 
            this.defaultBlockState().setValue(FACING, facing));

        return destination;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        forceUpdate(world, pos, state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        forceUpdate(world, pos, state);
        super.onPlace(state, world, pos, oldState, isMoving);
    }

    @Override
    public void destroy(LevelAccessor world, BlockPos pos, BlockState state) {
        if(world instanceof Level)
            forceUpdate((Level)world, pos, state);
        super.destroy(world, pos, state);
    }

    // forces an update on facing diagonals
    protected void forceUpdate(Level world, BlockPos pos, BlockState state) {
        if(world.isClientSide) return;
        BlockState over = getOver(world, pos, state);
        BlockState under = getUnder(world, pos, state);
        Direction facing = state.getValue(FACING);

        if(over.getBlock() instanceof DiagonalGirderBlock) {
            BlockPos overPos = pos.relative(facing).relative(Direction.UP);
            ((DiagonalGirderBlock) over.getBlock()).doShapeUpdate(world, overPos, over);
        }

        if(under.getBlock() instanceof DiagonalGirderBlock) {
            BlockPos underPos = pos.relative(facing.getOpposite()).relative(Direction.DOWN);
            ((DiagonalGirderBlock) under.getBlock()).doShapeUpdate(world, underPos, under);
        }
    }

    protected void doShapeUpdate(Level world, BlockPos pos, BlockState state) {
        BlockState destination = getDestinationState(world, pos, state);
        world.setBlock(pos, destination, Block.UPDATE_ALL);
    }

    public BlockState getDestinationState(Level world, BlockPos pos, BlockState state) {
        BlockState under = getUnder(world, pos, state);
        BlockState over = getOver(world, pos, state);

        if(isDiagonalGirder(under) && isDiagonalGirder(over))
            return state.setValue(MODEL_TYPE, DiagonalGirderModelType.MIDDLE);

        if(isCreateGirder(world, pos, under, state) && isCreateGirder(world, pos, over, state))
            return state.setValue(MODEL_TYPE, DiagonalGirderModelType.LONG_DOUBLE);

        if(isDiagonalGirder(under) && isCreateGirder(world, pos, over, state))
            return state.setValue(MODEL_TYPE, DiagonalGirderModelType.LONG_END_DOWN);

        if(isCreateGirder(world, pos, under, state) && isDiagonalGirder(over))
            return state.setValue(MODEL_TYPE, DiagonalGirderModelType.LONG_END_UP);

        if(isDiagonalGirder(under) && !isCreateGirder(world, pos, over, state))
            return state.setValue(MODEL_TYPE, DiagonalGirderModelType.SHORT_END_DOWN);

        if(!isCreateGirder(world, pos, under, state) && isDiagonalGirder(over))
            return state.setValue(MODEL_TYPE, DiagonalGirderModelType.SHORT_END_UP);

        if(!isCreateGirder(world, pos, under, state) && isCreateGirder(world, pos, over, state))
            return state.setValue(MODEL_TYPE, DiagonalGirderModelType.LONG_END_DOWN);

        if(isCreateGirder(world, pos, under, state) && !isCreateGirder(world, pos, over, state))
            return state.setValue(MODEL_TYPE, DiagonalGirderModelType.LONG_END_UP);

        if(!isCreateGirder(world, pos, under, state) && !isCreateGirder(world, pos, over, state))
            return state.setValue(MODEL_TYPE, DiagonalGirderModelType.SHORT_DOUBLE);

        return state;
    }

    private boolean isDiagonalGirder(BlockState state) {
        return false;
        // return state.getBlock() == MechanoBlocks.DIAGONAL_GIRDER.get();
    }

    private boolean isCreateGirder(Level world, BlockPos pos, BlockState targetState, BlockState girderState) {
        if(targetState.getBlock() == AllBlocks.METAL_GIRDER.get()) return true;
        return false;
        // Direction dir = girderState.getValue(FACING);
		// return targetState.isFaceSturdy(world, pos, dir.getOpposite(), SupportType.CENTER);
    }

    @Override
    public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 1f;
    }

    private BlockState getUnder(Level world, BlockPos mainPos, BlockState state) {
        Direction facing = state.getValue(FACING);
        BlockPos offset = mainPos.relative(facing.getOpposite());
        return world.getBlockState(offset.relative(Direction.DOWN));
    }

    private BlockState getOver(Level world, BlockPos mainPos, BlockState state) {
        Direction facing = state.getValue(FACING);
        BlockPos offset = mainPos.relative(facing);
        return world.getBlockState(offset.relative(Direction.UP));
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult ray) {
        IPlacementHelper helper = PlacementHelpers.get(placementHelperId);
        ItemStack heldItem = player.getItemInHand(hand);
		if (helper.matchesItem(heldItem))
			return helper.getOffset(player, world, state, pos, ray)
				.placeInWorld(world, (BlockItem) heldItem.getItem(), player, hand, ray);

        if (AllItems.WRENCH.isIn(heldItem) && !player.isSteppingCarefully()) {
            if (MechanoClientEvents.GIRDER_BEHAVIOR.handleClick(world, pos, state, ray, getBlockEntity(world, pos))) {
                world.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.5f, 0.9f);
                world.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.2f, 1);   
                
                return InteractionResult.sidedSuccess(world.isClientSide);
            }
            doShapeUpdate(world, pos, state);
            return InteractionResult.FAIL;
        }
		return InteractionResult.PASS;
	}

    public List<Pair<AABB, GirderPartial>> getRelevantPartials(BlockState state) {
        Direction facing = state.getValue(FACING).getOpposite();
        List<Pair<AABB, GirderPartial>> out = new ArrayList<Pair<AABB, GirderPartial>>();
        switch(state.getValue(MODEL_TYPE)) {
            case LONG_DOUBLE:
                out.add(Pair.of(BOX_LONG_DOWN_FLAT.get(facing).bounds(), GirderPartial.LONG_DOWN_FLAT));
                out.add(Pair.of(BOX_LONG_DOWN_VERT.get(facing).bounds(), GirderPartial.LONG_DOWN_VERT));
                out.add(Pair.of(BOX_LONG_UP_FLAT.get(facing).bounds(), GirderPartial.LONG_UP_FLAT));
                out.add(Pair.of(BOX_LONG_UP_VERT.get(facing).bounds(), GirderPartial.LONG_UP_VERT));
                break;
            case LONG_END_DOWN:
                out.add(Pair.of(BOX_LONG_DOWN_FLAT.get(facing).bounds(), GirderPartial.LONG_DOWN_FLAT));
                out.add(Pair.of(BOX_LONG_DOWN_VERT.get(facing).bounds(), GirderPartial.LONG_DOWN_VERT));
                out.add(Pair.of(BOX_SHORT_UP_FLAT.get(facing).bounds(), GirderPartial.SHORT_UP_FLAT));
                out.add(Pair.of(BOX_SHORT_UP_VERT.get(facing).bounds(), GirderPartial.SHORT_UP_VERT));
                break;
            case LONG_END_UP:
                out.add(Pair.of(BOX_LONG_UP_FLAT.get(facing).bounds(), GirderPartial.LONG_UP_FLAT));
                out.add(Pair.of(BOX_LONG_UP_VERT.get(facing).bounds(), GirderPartial.LONG_UP_VERT));
                out.add(Pair.of(BOX_SHORT_DOWN_FLAT.get(facing).bounds(), GirderPartial.SHORT_DOWN_FLAT));
                out.add(Pair.of(BOX_SHORT_DOWN_VERT.get(facing).bounds(), GirderPartial.SHORT_DOWN_VERT));
                break;
            case MIDDLE:
                break;
            case SHORT_DOUBLE:
                out.add(Pair.of(BOX_SHORT_DOWN_FLAT.get(facing).bounds(), GirderPartial.SHORT_DOWN_FLAT));
                out.add(Pair.of(BOX_SHORT_DOWN_VERT.get(facing).bounds(), GirderPartial.SHORT_DOWN_VERT));
                out.add(Pair.of(BOX_SHORT_UP_FLAT.get(facing).bounds(), GirderPartial.SHORT_UP_FLAT));
                out.add(Pair.of(BOX_SHORT_UP_VERT.get(facing).bounds(), GirderPartial.SHORT_UP_VERT));
                break;
            case SHORT_END_DOWN:
                out.add(Pair.of(BOX_SHORT_DOWN_FLAT.get(facing).bounds(), GirderPartial.SHORT_DOWN_FLAT));
                out.add(Pair.of(BOX_SHORT_DOWN_VERT.get(facing).bounds(), GirderPartial.SHORT_DOWN_VERT));
                break;
            case SHORT_END_UP:
                out.add(Pair.of(BOX_SHORT_UP_FLAT.get(facing).bounds(), GirderPartial.SHORT_UP_FLAT));
                out.add(Pair.of(BOX_SHORT_UP_VERT.get(facing).bounds(), GirderPartial.SHORT_UP_VERT));
                break;
        }
        return out;
    }

    @MethodsReturnNonnullByDefault
	private static class PlacementHelper extends GirderDirectionalHelper<Direction> {
		private PlacementHelper() {
			super(state -> state.getBlock() instanceof DiagonalGirderBlock, state -> state.getValue(FACING), FACING);
		}

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return i -> i.getItem() instanceof BlockItem
				&& ((BlockItem) i.getItem()).getBlock() instanceof DiagonalGirderBlock;
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			// return Predicates.or(MechanoBlocks.DIAGONAL_GIRDER::has);
            return null;
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

    @Override
    public Class<DiagonalGirderBlockEntity> getBlockEntityClass() {
        return DiagonalGirderBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends DiagonalGirderBlockEntity> getBlockEntityType() {
        // return MechanoBlockEntities.DIAGONAL_GIRDER.get();
        return null;
    }

    public enum GirderPartial {
        LONG_DOWN_FLAT, 
        LONG_DOWN_VERT,
        LONG_UP_FLAT,  
        LONG_UP_VERT,
        SHORT_DOWN_FLAT, 
        SHORT_DOWN_VERT,
        SHORT_UP_FLAT,  
        SHORT_UP_VERT;
    }
}