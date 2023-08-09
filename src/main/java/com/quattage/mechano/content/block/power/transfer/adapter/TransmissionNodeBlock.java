package com.quattage.mechano.content.block.power.transfer.adapter;


import java.util.Locale;

import com.quattage.mechano.core.block.SimpleOrientedBlock;
import com.quattage.mechano.core.block.orientation.SimpleOrientation;
import com.quattage.mechano.core.util.ShapeBuilder;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TransmissionNodeBlock extends SimpleOrientedBlock {

    public static final VoxelShaper BASE_SHAPE = ShapeBuilder.newShape(3, 0, 3, 13, 4, 13).add(1, 4, 1, 15, 16, 15).defaultUp();
    public static final EnumProperty<TransmissionNodeModelType> MODEL_TYPE = EnumProperty.create("model", TransmissionNodeModelType.class);

    public TransmissionNodeBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(defaultBlockState().setValue(MODEL_TYPE, TransmissionNodeModelType.BASE));
    }

    public enum TransmissionNodeModelType implements StringRepresentable {
        BASE, GROUNDED, GIRDERED;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    
        @Override
        public String toString() {
            return getSerializedName();
        }
    
        public static TransmissionNodeModelType cycleRotor(TransmissionNodeModelType type) {
            int pos = type.ordinal();
            if(pos == 4) {
                pos -= 1;
                return TransmissionNodeModelType.values()[pos];
            }
            pos += 1;
            return TransmissionNodeModelType.values()[pos];
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MODEL_TYPE);
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
            localForward = SimpleOrientedBlock.getTriQuadrant(context, localUp, false);

        return this.defaultBlockState().setValue(ORIENTATION, SimpleOrientation.combine(localUp, localForward.getAxis()));
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
    }
    
    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        super.neighborChanged(state, world, pos, pBlock, pFromPos, pIsMoving);
        doBlockCheck(world, pos, state);
    }

    private void doBlockCheck(Level world, BlockPos pos, BlockState state) {
        Direction ax = state.getValue(ORIENTATION).getCardinal().getOpposite();
        if(world.getBlockState(pos.relative(ax)).getBlock() == AllBlocks.METAL_GIRDER.get()) {
            world.setBlock(pos, state.setValue(MODEL_TYPE, TransmissionNodeModelType.GIRDERED), Block.UPDATE_ALL);
            return;
        }
        Block facingBlock = world.getBlockState(pos.relative(ax)).getBlock();
        boolean faceFull = false;

        try {
            faceFull = Block.isFaceFull(facingBlock.getShape(state, (BlockGetter)world, pos, null), ax.getOpposite());
        } catch(Exception e) {} // sorry

        if(faceFull) {
            world.setBlock(pos, state.setValue(MODEL_TYPE, TransmissionNodeModelType.GROUNDED), Block.UPDATE_ALL);
            return;
        }

        world.setBlock(pos, state.setValue(MODEL_TYPE, TransmissionNodeModelType.BASE), Block.UPDATE_ALL);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level world = context.getLevel();
        SimpleOrientation cd = SimpleOrientation.cycleOrient(state.getValue(ORIENTATION));
        BlockState rotated = state.setValue(ORIENTATION, cd);

        if (!rotated.canSurvive(world, context.getClickedPos()))
			return InteractionResult.PASS;
            
        KineticBlockEntity.switchToBlockState(world, context.getClickedPos(), updateAfterWrenched(rotated, context));

        if (world.getBlockState(context.getClickedPos()) != state)
			playRotateSound(world, context.getClickedPos());

        return InteractionResult.SUCCESS;
    }

}
