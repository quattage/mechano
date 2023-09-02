package com.quattage.mechano.content.block.power.transfer.adapter;

import java.util.Locale;

import com.quattage.mechano.MechanoBlockEntities;
import com.quattage.mechano.MechanoBlocks;
import com.quattage.mechano.content.block.power.alternator.collector.CollectorBlock;
import com.quattage.mechano.foundation.block.CombinedOrientedBlock;
import com.quattage.mechano.foundation.block.SimpleOrientedBlock;
import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.helper.ShapeBuilder;
import com.quattage.mechano.foundation.helper.BlockMath;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CouplingNodeBlock extends CombinedOrientedBlock implements IBE<CouplingNodeBlockEntity> {

    public static final VoxelShaper NODE_SHAPE_CANTED = ShapeBuilder.newShape(1, 2, 1, 15, 16, 15).defaultUp();
    public static final VoxelShaper NODE_SHAPE_BASE = ShapeBuilder.newShape(3, 0, 3, 13, 4, 13).add(1, 4, 1, 15, 16, 15).defaultUp();
    public static final EnumProperty<CouplingNodeModelType> MODEL_TYPE = EnumProperty.create("model", CouplingNodeModelType.class);

    public CouplingNodeBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(defaultBlockState().setValue(MODEL_TYPE, CouplingNodeModelType.BASE));
    }

    public enum CouplingNodeModelType implements StringRepresentable {
        BASE, GROUNDED, GIRDERED, ROTORED, ROTORED_CANTED;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    
        @Override
        public String toString() {
            return getSerializedName();
        }
    
        public static CouplingNodeModelType cycleRotor(CouplingNodeModelType type) {
            int pos = type.ordinal();
            if(pos == 4) {
                pos -= 1;
                return CouplingNodeModelType.values()[pos];
            }
            pos += 1;
            return CouplingNodeModelType.values()[pos];
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if(state.getValue(CouplingNodeBlock.MODEL_TYPE) == CouplingNodeModelType.ROTORED_CANTED)
            return NODE_SHAPE_CANTED.get(state.getValue(ORIENTATION).getLocalForward());
        return NODE_SHAPE_BASE.get(state.getValue(ORIENTATION).getLocalUp());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MODEL_TYPE);
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction localUp = context.getClickedFace();
        Direction localForward = context.getHorizontalDirection().getOpposite();

        BlockState behindState = context.getLevel().getBlockState(context.getClickedPos().relative(localUp.getOpposite()));
        if(behindState.getBlock() != MechanoBlocks.COLLECTOR.get()) {
            if(localUp.getAxis() == localForward.getAxis())
                localForward = BlockMath.getClickedQuadrant(context, localUp, false);

            if(context.getPlayer().isCrouching()) localForward = localForward.getOpposite();
            return this.defaultBlockState().setValue(ORIENTATION, CombinedOrientation.combine(localUp, localForward.getOpposite()));
        }

        localForward = behindState.getValue(CollectorBlock.FACING);
        if(localForward.getAxis() == localUp.getAxis())
            localForward = BlockMath.getClickedQuadrant(context, localUp, false);
        return this.defaultBlockState().setValue(ORIENTATION, CombinedOrientation.combine(localUp, localForward.getOpposite()));
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean pIsMoving) {
        super.onPlace(state, world, pos, oldState, pIsMoving);
        doBlockCheck(world, pos, state);
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        super.neighborChanged(state, world, pos, pBlock, pFromPos, pIsMoving);
        doBlockCheck(world, pos, state);
    }

    private void doBlockCheck(Level world, BlockPos pos, BlockState state) {
        Direction ax = state.getValue(ORIENTATION).getLocalUp().getOpposite();
        if(world.getBlockState(pos.relative(ax)).getBlock() == AllBlocks.METAL_GIRDER.get()) {
            world.setBlock(pos, state.setValue(MODEL_TYPE, CouplingNodeModelType.GIRDERED), Block.UPDATE_ALL);
            return;
        }
        Block facingBlock = world.getBlockState(pos.relative(ax)).getBlock();
        BlockState behindState = world.getBlockState(pos.relative(ax));
        if(behindState.getBlock() == MechanoBlocks.COLLECTOR.get()) {
            if(state.getValue(ORIENTATION).getLocalForward() == behindState.getValue(CollectorBlock.FACING).getOpposite()) {
                CouplingNodeModelType modelType = CouplingNodeModelType.cycleRotor(state.getValue(MODEL_TYPE));
                if(modelType == CouplingNodeModelType.ROTORED || modelType == CouplingNodeModelType.ROTORED_CANTED) return;
                world.setBlock(pos, state.setValue(MODEL_TYPE, CouplingNodeModelType.ROTORED), Block.UPDATE_ALL);
                return;
            }        
        }

        boolean faceFull = false;
        try {
            faceFull = Block.isFaceFull(facingBlock.getShape(state, (BlockGetter)world, pos, null), ax.getOpposite());
        } catch(Exception e) {} // sorry

        if(faceFull) {
            world.setBlock(pos, state.setValue(MODEL_TYPE, CouplingNodeModelType.GROUNDED), Block.UPDATE_ALL);
            return;
        }

        world.setBlock(pos, state.setValue(MODEL_TYPE, CouplingNodeModelType.BASE), Block.UPDATE_ALL);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level world = context.getLevel();
        CombinedOrientation strictCD = CombinedOrientation.cycleLocalForward(state.getValue(ORIENTATION));
        CouplingNodeModelType modelType = CouplingNodeModelType.cycleRotor(state.getValue(MODEL_TYPE));
        if(state.getValue(MODEL_TYPE) == CouplingNodeModelType.ROTORED || state.getValue(MODEL_TYPE) == CouplingNodeModelType.ROTORED_CANTED) {
            BlockState rotated = state.setValue(MODEL_TYPE, modelType);
            if (!rotated.canSurvive(world, context.getClickedPos()))
                return InteractionResult.PASS;

            KineticBlockEntity.switchToBlockState(world, context.getClickedPos(), updateAfterWrenched(rotated, context));
        } else {
            BlockState rotated = state.setValue(ORIENTATION, strictCD);
            if (!rotated.canSurvive(world, context.getClickedPos()))
                return InteractionResult.PASS;

            KineticBlockEntity.switchToBlockState(world, context.getClickedPos(), updateAfterWrenched(rotated, context));
        }
        return wrenchConfirm(world, context, state);
    }

    private InteractionResult wrenchConfirm(Level world, UseOnContext context, BlockState state) {
        if (world.getBlockState(context.getClickedPos()) != state)
			playRotateSound(world, context.getClickedPos());

        return InteractionResult.SUCCESS;
    }

    @Override
    public Class<CouplingNodeBlockEntity> getBlockEntityClass() {
        return CouplingNodeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CouplingNodeBlockEntity> getBlockEntityType() {
        return MechanoBlockEntities.COUPLING_NODE.get();
    }
}
