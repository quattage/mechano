package com.quattage.mechano.content.block.power.transfer.adapter;

import static com.quattage.mechano.content.block.power.transfer.adapter.NodeModelType.NODE_MODEL_TYPE;

import com.mrh0.createaddition.shapes.CAShapes;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.block.power.alternator.collector.CollectorBlock;
import com.quattage.mechano.core.block.SimpleOrientedBlock;
import com.quattage.mechano.core.block.CombinedOrientedBlock;
import com.quattage.mechano.core.block.orientation.CombinedOrientation;
import com.quattage.mechano.registry.MechanoBlockEntities;
import com.quattage.mechano.registry.MechanoBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CouplingNodeBlock extends CombinedOrientedBlock implements IBE<CouplingNodeBlockEntity> {

    public static final VoxelShaper NODE_SHAPE_CANTED = CAShapes.shape(1, 2, 1, 15, 16, 15).forDirectional();
    public static final VoxelShaper NODE_SHAPE_BASE = CAShapes.shape(3, 0, 3, 13, 4, 13).add(1, 4, 1, 15, 16, 15).forDirectional();

    public CouplingNodeBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(defaultBlockState().setValue(NODE_MODEL_TYPE, NodeModelType.BASE));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if(state.getValue(NodeModelType.NODE_MODEL_TYPE) == NodeModelType.ROTOR_CANTED)
            return NODE_SHAPE_CANTED.get(state.getValue(ORIENTATION).getLocalForward());
        return NODE_SHAPE_BASE.get(state.getValue(ORIENTATION).getLocalUp());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(NODE_MODEL_TYPE);
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction localUp = context.getClickedFace();
        Direction localForward = context.getHorizontalDirection().getOpposite();

        BlockState behindState = context.getLevel().getBlockState(context.getClickedPos().relative(localUp.getOpposite()));
        if(behindState.getBlock() != MechanoBlocks.COLLECTOR.get()) {
            if(localUp.getAxis() == localForward.getAxis())
                localForward = SimpleOrientedBlock.getTriQuadrant(context, localUp, false);

            if(context.getPlayer().isCrouching()) localForward = localForward.getOpposite();
            return this.defaultBlockState().setValue(ORIENTATION, CombinedOrientation.combine(localUp, localForward));
        }

        localForward = behindState.getValue(CollectorBlock.FACING);
        if(localForward.getAxis() == localUp.getAxis())
            localForward = SimpleOrientedBlock.getTriQuadrant(context, localUp, false);
        return this.defaultBlockState().setValue(ORIENTATION, CombinedOrientation.combine(localUp, localForward));
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
            world.setBlock(pos, state.setValue(NODE_MODEL_TYPE, NodeModelType.GIRDERED), Block.UPDATE_ALL);
            return;
        }
        Block facingBlock = world.getBlockState(pos.relative(ax)).getBlock();
        BlockState behindState = world.getBlockState(pos.relative(ax));
        if(behindState.getBlock() == MechanoBlocks.COLLECTOR.get()) {
            if(state.getValue(ORIENTATION).getLocalForward() == behindState.getValue(CollectorBlock.FACING)) {
                NodeModelType modelType = NodeModelType.cycleRotor(state.getValue(NODE_MODEL_TYPE));
                if(modelType == NodeModelType.ROTORED || modelType == NodeModelType.ROTOR_CANTED) return;
                world.setBlock(pos, state.setValue(NODE_MODEL_TYPE, NodeModelType.ROTORED), Block.UPDATE_ALL);
                return;
            }        
        }

        boolean faceFull = false;
        try {
            faceFull = Block.isFaceFull(facingBlock.getShape(state, (BlockGetter)world, pos, null), ax.getOpposite());
        } catch(Exception e) {} // sorry

        if(faceFull) {
            world.setBlock(pos, state.setValue(NODE_MODEL_TYPE, NodeModelType.GROUNDED), Block.UPDATE_ALL);
            return;
        }

        world.setBlock(pos, state.setValue(NODE_MODEL_TYPE, NodeModelType.BASE), Block.UPDATE_ALL);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level world = context.getLevel();
        CombinedOrientation strictCD = CombinedOrientation.cycleLocalForward(state.getValue(ORIENTATION));
        NodeModelType modelType = NodeModelType.cycleRotor(state.getValue(NODE_MODEL_TYPE));
        if(state.getValue(NODE_MODEL_TYPE) == NodeModelType.ROTORED || state.getValue(NODE_MODEL_TYPE) == NodeModelType.ROTOR_CANTED) {
            BlockState rotated = state.setValue(NODE_MODEL_TYPE, modelType);
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
