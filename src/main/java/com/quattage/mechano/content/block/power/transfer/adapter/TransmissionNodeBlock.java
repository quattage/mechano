package com.quattage.mechano.content.block.power.transfer.adapter;

import static com.quattage.mechano.content.block.power.transfer.adapter.NodeModelType.NODE_MODEL_TYPE;

import com.mrh0.createaddition.shapes.CAShapes;
import com.quattage.mechano.content.block.power.transfer.connector.HeapConnectorStackedBlock;
import com.quattage.mechano.core.block.ComplexDirectionalBlock;
import com.quattage.mechano.core.placement.ComplexDirection;
import com.quattage.mechano.registry.MechanoBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TransmissionNodeBlock extends ComplexDirectionalBlock {

    public static final VoxelShaper BASE_SHAPE = CAShapes.shape(3, 0, 3, 13, 4, 13).add(1, 4, 1, 15, 16, 15).forDirectional();

    public TransmissionNodeBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(defaultBlockState().setValue(NODE_MODEL_TYPE, NodeModelType.BASE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(NODE_MODEL_TYPE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return BASE_SHAPE.get(state.getValue(ORIENTATION).getCardinal());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction localUp = context.getClickedFace();
        Direction localForward = context.getHorizontalDirection().getOpposite();

        if(localUp.getAxis() == localForward.getAxis())
            localForward = ComplexDirectionalBlock.getTriQuadrant(context, localUp, false);

        return this.defaultBlockState().setValue(ORIENTATION, ComplexDirection.combine(localUp, localForward.getAxis()));
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean pIsMoving) {
        super.onPlace(state, world, pos, oldState, pIsMoving);
        doBlockCheck(world, pos, state);
    }


    @Override
    public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        super.playerWillDestroy(world, pos, state, player);
        BlockPos adjacent = pos.relative(state.getValue(ORIENTATION).getCardinal());
        BlockState connectorState = world.getBlockState(adjacent);
        if(connectorState.getBlock() == MechanoBlocks.HEAP_CONNECTOR_STACKED.get()) {
            // HeapConnectorStackedBlock connector = (HeapConnectorStackedBlock) connectorState.getBlock();
            world.destroyBlock(adjacent, true);
        }
    }
    
    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        super.neighborChanged(state, world, pos, pBlock, pFromPos, pIsMoving);
        doBlockCheck(world, pos, state);
    }

    private void doBlockCheck(Level world, BlockPos pos, BlockState state) {
        Direction ax = state.getValue(ORIENTATION).getCardinal().getOpposite();
        if(world.getBlockState(pos.relative(ax)).getBlock() == AllBlocks.METAL_GIRDER.get()) {
            world.setBlock(pos, state.setValue(NODE_MODEL_TYPE, NodeModelType.GIRDERED), Block.UPDATE_ALL);
            return;
        }
        Block facingBlock = world.getBlockState(pos.relative(ax)).getBlock();
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
        ComplexDirection cd = ComplexDirection.cycleOrient(state.getValue(ORIENTATION));
        BlockState rotated = state.setValue(ORIENTATION, cd);

        if (!rotated.canSurvive(world, context.getClickedPos()))
			return InteractionResult.PASS;
            
        KineticBlockEntity.switchToBlockState(world, context.getClickedPos(), updateAfterWrenched(rotated, context));

        if (world.getBlockState(context.getClickedPos()) != state)
			playRotateSound(world, context.getClickedPos());

        return InteractionResult.SUCCESS;
    }

}
