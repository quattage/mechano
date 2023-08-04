package com.quattage.mechano.content.block.integrated.toolStation;

import java.util.Locale;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoBlockEntities;
import com.quattage.mechano.MechanoBlocks;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.item.ItemHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;


public class ToolStationBlock extends HorizontalDirectionalBlock implements IBE<ToolStationBlockEntity> {
    public static final EnumProperty<WideBlockModelType> MODEL_TYPE = EnumProperty.create("model", WideBlockModelType.class);
    protected static final VoxelShape BLOCK_NORTH = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape BLOCK_SOUTH = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape BLOCK_EAST = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape BLOCK_WEST = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    
    public ToolStationBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(MODEL_TYPE, WideBlockModelType.BASE));
    }

    public enum WideBlockModelType implements StringRepresentable {
        BASE, FORGED, HEATED, MAXIMIZED, DUMMY;

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
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            if (state.getValue(MODEL_TYPE) != WideBlockModelType.DUMMY) {
                BlockPos otherpos = pos.relative(state.getValue(FACING).getClockWise());
                BlockState otherstate = level.getBlockState(otherpos);
                if (otherstate.getBlock() == this) {
                    level.setBlock(otherpos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    if (!state.hasBlockEntity())
                        return;
                    withBlockEntityDo(level, pos, te -> te.spawnOpposingBreakParticles(otherpos));
                }
            }
            else {
                BlockPos otherpos = pos.relative(state.getValue(FACING).getCounterClockWise());
                BlockState otherstate = level.getBlockState(otherpos);

                Mechano.log("POSITION: " + otherpos + " STATE: " + otherstate);
                if (otherstate.getBlock() == this) {
                    level.setBlock(otherpos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    if (!state.hasBlockEntity())
                        return;
                    withBlockEntityDo(level, pos, te -> te.spawnOpposingBreakParticles(otherpos));
                }
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity player, ItemStack stack) {
        super.setPlacedBy(world, pos, state, player, stack);
        if(!world.isClientSide) {
            BlockPos possy = pos.relative(state.getValue(FACING).getClockWise());
            world.setBlock(possy, state.setValue(MODEL_TYPE, WideBlockModelType.DUMMY), Block.UPDATE_ALL);
            world.updateNeighborsAt(pos, Blocks.AIR);
        }
    }

    
    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborChanged(state, world, pos, sourceBlock, sourcePos, notify);
        if(!world.isClientSide) {
            if(state.getValue(MODEL_TYPE) == WideBlockModelType.DUMMY) {
                referToMainBlockForUpdate(state, world, pos, sourceBlock, sourcePos, notify);
            } else {
                mainBlockUpdate(state, world, pos, sourceBlock, sourcePos, notify);
            }
        }
    }

    private void referToMainBlockForUpdate(BlockState state, Level world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        BlockPos mainBlockPos = pos.relative(state.getValue(FACING).getCounterClockWise());
        BlockState mainBlockState = world.getBlockState(mainBlockPos);
        this.mainBlockUpdate(mainBlockState, world, mainBlockPos, sourceBlock, sourcePos, notify);
    }

    private void mainBlockUpdate(BlockState state, Level world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if(state.getBlock() != this)
            return;
        
        BlockPos left = pos.relative(state.getValue(FACING).getCounterClockWise());
        BlockPos right = pos.relative(state.getValue(FACING).getClockWise(), 2);
        if(sourcePos.equals(left) || sourcePos.equals(right)) {
            BlockState leftBlockState = world.getBlockState(left);
            BlockState rightBlockState = world.getBlockState(right);
                                                                                        // TODO -> rightBlockState.getBlock().equals(MechanoBlocks.INDUCTOR.get()
            if(leftBlockState.getBlock().equals(MechanoBlocks.FORGE_UPGRADE.get()) && rightBlockState.getBlock().equals(Blocks.DIRT)) {
                setLevel(world, pos, state, WideBlockModelType.MAXIMIZED);
            } else if(leftBlockState.getBlock().equals(MechanoBlocks.FORGE_UPGRADE.get())) {
                setLevel(world, pos, state, WideBlockModelType.FORGED);
                // TODO -> rightBlockState.getBlock().equals(MechanoBlocks.INDUCTOR.get()
            } else if(rightBlockState.getBlock().equals(Blocks.DIRT)) {
                setLevel(world, pos, state, WideBlockModelType.HEATED);
            } else {
                setLevel(world, pos, state, WideBlockModelType.BASE);
            }
        }
    }

    private void setLevel(Level world, BlockPos pos, BlockState state, WideBlockModelType bType) {
        if(state.getValue(MODEL_TYPE) != bType) {
            if (!state.hasBlockEntity())
                return;
            withBlockEntityDo(world, pos, te -> te.doUpgradeEffects(state, bType));
            world.setBlock(pos, state.setValue(MODEL_TYPE, bType), Block.UPDATE_ALL);
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, @NotNull BlockGetter view, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        switch (state.getValue(FACING)) {
            case NORTH:
                return BLOCK_NORTH;
            case SOUTH:
                return BLOCK_SOUTH;
            case WEST:
                return BLOCK_WEST;
            default:
                return BLOCK_EAST;
        }
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter view, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MODEL_TYPE);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader view, BlockPos pos) {
        BlockPos otherpos = pos.relative(state.getValue(FACING).getClockWise());
        return view.getBlockState(otherpos).canBeReplaced();
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter view, BlockPos pos) {
        return 1;
    }
    
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.hasBlockEntity() || state.getBlock() == newState.getBlock())
			return;

		withBlockEntityDo(level, pos, te -> ItemHelper.dropContents(level, pos, te.INVENTORY));
		level.removeBlockEntity(pos);
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            withBlockEntityDo(level, pos, entity -> NetworkHooks.openScreen((ServerPlayer) player, entity, entity::sendToMenu));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
	public BlockEntityType<? extends ToolStationBlockEntity> getBlockEntityType() {
		return MechanoBlockEntities.TOOL_STATION.get();
	}

    @Override
    public Class<ToolStationBlockEntity> getBlockEntityClass() {
        return ToolStationBlockEntity.class;
    }
}