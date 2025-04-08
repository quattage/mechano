package com.quattage.mechano.content.block.power.alternator.stator;

import javax.annotation.Nullable;

import com.quattage.mechano.content.block.power.alternator.rotor.AbstractRotorBlock;
import com.quattage.mechano.content.block.power.alternator.rotor.AbstractRotorBlockEntity;
import com.quattage.mechano.content.block.power.alternator.rotor.BlockRotorable;
import com.quattage.mechano.foundation.block.BlockChangeListenable;
import com.quattage.mechano.foundation.block.SimpleOrientedBlock;
import com.quattage.mechano.foundation.block.hitbox.HitboxNameable;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.block.orientation.SimpleOrientation;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public abstract class AbstractStatorBlock<T extends Enum<T> & StringRepresentable & HitboxNameable & StatorTypeTransformable<T>> extends SimpleOrientedBlock implements BlockChangeListenable {

    public AbstractStatorBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.defaultBlockState()
            .setValue(getTypeProperty(), getDefaultModelType()));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return getInitialState(context.getLevel(), context.getClickedPos(), state, state.getValue(ORIENTATION).getOrient(), true);
    }

    public BlockState getInitialState(Level world, BlockPos pos, @Nullable BlockState state, Axis rotorAxis, boolean shouldUpdate) {
        
        if(state == null) state = this.defaultBlockState();

        BlockPos[] planePositions = DirectionTransformer.getAllAdjacent(pos, rotorAxis);

        for(BlockPos visitedPos : planePositions) {
            BlockState visitedState = world.getBlockState(visitedPos);
            if(isRotor(visitedState.getBlock())) {

                Direction delta = Direction.fromDelta(
                    pos.getX() - visitedPos.getX(), 
                    pos.getY() - visitedPos.getY(), 
                    pos.getZ() - visitedPos.getZ()
                );

                if(delta == null) continue;

                state = state.setValue(ORIENTATION, 
                    SimpleOrientation.combine(
                        delta.getOpposite(), rotorAxis
                    ));
                break;
            }
        }

        return getAlignedModelState(world, rotorAxis, pos, state, state, planePositions);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {

        super.onPlace(state, world, pos, oldState, isMoving);
        if(world.isClientSide) return;

        Axis rotorAxis = state.getValue(ORIENTATION).getOrient();
        if(getAlignmentType().needsForcedUpdates()) {
            BlockPos[] cornerPositions = getAlignmentType().toPositions(pos, rotorAxis);
            for(BlockPos visitedPos : cornerPositions) {
                BlockState visitedState = world.getBlockState(visitedPos);
                if(visitedState.getBlock() instanceof AbstractStatorBlock asb)
                    asb.forcedNeighborChange(world, rotorAxis, visitedPos, state, visitedState, cornerPositions);
            }
        }
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        // TODO wrench logic
        return InteractionResult.FAIL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean pIsMoving) {

        SimpleOrientation thisOrient = state.getValue(ORIENTATION);
        Direction orientation = DirectionTransformer.toDirection(thisOrient.getOrient());

        boolean hasRear = false;
        BlockState rear = world.getBlockState(pos.relative(orientation));
        if(this.isStator(rear.getBlock())) {
            hasRear = areBlocksContinuous(thisOrient, state.getValue(getTypeProperty()), 
                rear.getValue(ORIENTATION), rear.getValue(getTypeProperty()));
        }

        boolean hasFront = false;
        BlockState front = world.getBlockState(pos.relative(orientation.getOpposite()));
        if(this.isStator(front.getBlock())) {
            hasFront = areBlocksContinuous(thisOrient, state.getValue(getTypeProperty()), 
                front.getValue(ORIENTATION), front.getValue(getTypeProperty()));
        }

        T type = state.getValue(getTypeProperty());
        T typeNew = type.copy();

        if(hasFront && hasRear) typeNew = typeNew.toMiddle();
        else if(hasFront) typeNew = typeNew.toEndA();
        else if(hasRear) typeNew =  typeNew.toEndB();
        else typeNew = typeNew.toSingle();

        if(typeNew != type) setModel(world, pos, state, typeNew, true);

        super.neighborChanged(state, world, pos, block, fromPos, pIsMoving);
    }

    public void forcedNeighborChange(Level world, Axis fromAxis, BlockPos centerPos, BlockState fromState, BlockState thisState, BlockPos[] plane) {

        Axis thisAxis = thisState.getValue(ORIENTATION).getOrient();
        if(fromAxis != thisAxis) return;

        BlockState newState = getAlignedModelState(world, fromAxis, centerPos, fromState, thisState, plane);
        if(thisState != newState)
            setModel(world, centerPos, newState);
    }

    
    private void setModel(Level world, BlockPos pos, BlockState state, T bType, boolean update) {
        world.setBlock(pos, state.setValue(getTypeProperty(), bType), update ? 3 : 16);
    }

    private void setModel(Level world, BlockPos pos, BlockState state) {
        world.setBlock(pos, state, 3);
    }

    @Override
    public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 1f;
    }

    protected abstract int getPlacementHelperId();
    protected abstract BlockEntry<? extends AbstractStatorBlock<T>> getStatorEntry();
    protected abstract BlockEntry<? extends BlockRotorable> getRotorEntry();
    protected abstract EnumProperty<T> getTypeProperty();
    protected abstract T getDefaultModelType();
    protected abstract UpdateAlignment getAlignmentType();
    protected abstract BlockState getAlignedModelState(Level world, Axis fromAxis, BlockPos centerPos, BlockState fromState, BlockState thisState, BlockPos[] plane);
    protected abstract boolean areBlocksContinuous(SimpleOrientation thisOrient, T thisType, SimpleOrientation thatOrient, T thatType);
    public abstract BlockPos getAttachedRotorPos(Level world, BlockPos pos, BlockState state);

    public boolean hasRotor(Level world, BlockPos pos, BlockState state) {

        BlockPos rotorPos = getAttachedRotorPos(world, pos, state);
        if(rotorPos == null) return false;
        BlockState rotorState = world.getBlockState(rotorPos);
        if(!(rotorState.getBlock() instanceof AbstractRotorBlock)) return false;



        return rotorState.getValue(RotatedPillarKineticBlock.AXIS) == state.getValue(SimpleOrientedBlock.ORIENTATION).getOrient();
    }

    protected boolean isRotor(BlockState state) {
        return isRotor(state.getBlock());
    }

    private boolean isRotor(Block block) {
        return block == getRotorEntry().get();
    }

    protected boolean isStator(BlockState state) {
        return isStator(state.getBlock());
    }

    private boolean isStator(Block block) {
        return block == getStatorEntry().get();
    }
    
    public abstract int getRadius();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(getTypeProperty());
        super.createBlockStateDefinition(builder);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult ray) {
        IPlacementHelper helper = PlacementHelpers.get(getPlacementHelperId());

        ItemStack heldItem = player.getItemInHand(hand);
		if(helper.matchesItem(heldItem))
			return helper.getOffset(player, world, state, pos, ray)
				.placeInWorld(world, (BlockItem)heldItem.getItem(), player, hand, ray);

		return InteractionResult.PASS;
	}

    @Override
    public void onBeforeBlockBroken(Level world, BlockPos pos, BlockState currentState, BlockState destinedState) {
        
    }

    @Override
    public void onAfterBlockBroken(Level world, BlockPos pos, BlockState pastState, BlockState currentState) {
        updateAttachedRotor(world, pos, pastState, false);
    }

    @Override
    public void onBeforeBlockPlaced(Level world, BlockPos pos, BlockState currentState, BlockState destinedState) {
        
    }

    @Override
    public void onAfterBlockPlaced(Level world, BlockPos pos, BlockState pastState, BlockState currentState) {
        updateAttachedRotor(world, pos, currentState, true);
    }

    @Nullable 
    private BlockPos updateAttachedRotor(Level world, BlockPos pos, BlockState state, boolean inc) {
        
        AbstractRotorBlockEntity arbe = AbstractRotorBlockEntity.get(world, getAttachedRotorPos(world, pos, state));

        if(arbe != null) {
            boolean attached = state.getValue(SimpleOrientedBlock.ORIENTATION).getOrient()
                    .equals(arbe.getBlockState().getValue(RotatedPillarKineticBlock.AXIS));

            if(attached) {
                if(inc) arbe.incStatorCount(); 
                else arbe.decStatorCount();
                return arbe.getBlockPos();
            }
        }

        return null;
    }
    
    protected enum UpdateAlignment {
        ADJACENT(false),
        CORNERS(true);

        private final boolean needsForcedUpdates;

        private UpdateAlignment(boolean needsForcedUpdates) {
            this.needsForcedUpdates = needsForcedUpdates;
        }

        protected boolean needsForcedUpdates() {
            return this.needsForcedUpdates;
        }

        protected BlockPos[] toPositions(BlockPos center, Axis axis) {
            if(this == CORNERS)
                return DirectionTransformer.getAllCorners(center, axis);
            return DirectionTransformer.getAllAdjacent(center, axis);
        }
    }
}