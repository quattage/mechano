package com.quattage.mechano.content.block.power.transfer.connector;

import java.util.Locale;

import com.mrh0.createaddition.shapes.CAShapes;
import com.quattage.mechano.content.block.power.transfer.adapter.CouplingNodeBlock;
import com.quattage.mechano.content.block.power.transfer.adapter.TransmissionNodeBlock;
import com.quattage.mechano.core.placement.ComplexDirection;
import com.quattage.mechano.registry.MechanoBlockEntities;
import com.quattage.mechano.registry.MechanoBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HeapConnectorBlock extends DirectionalBlock implements IBE<HeapConnectorBlockEntity> {
    

    public static final EnumProperty<HeapConnectorModelType> MODEL_TYPE = EnumProperty.create("model", HeapConnectorModelType.class);
    public static final VoxelShaper BASE_SHAPE = CAShapes.shape(5.5, 0, 5.5, 10.5, 15, 10.5).forDirectional();

    public enum HeapConnectorModelType implements StringRepresentable {
        BASE, GIRDERED, COUPLED;
    
        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    
        @Override
        public String toString() {
            return getSerializedName();
        }
    }


    public HeapConnectorBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.defaultBlockState()
            .setValue(FACING, Direction.NORTH)
            .setValue(MODEL_TYPE, HeapConnectorModelType.BASE));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
		Direction dir = state.getValue(FACING);
        VoxelShape checkBoxX = Block.box(0,7,7,10,9,9);
        VoxelShape checkBoxZ = Block.box(7,7,0,9,9,10);
		return
				!Shapes.joinIsNotEmpty(world.getBlockState(pos.relative(dir)).getBlockSupportShape(world,pos.relative(dir)).getFaceShape(dir.getOpposite()), checkBoxX, BooleanOp.ONLY_SECOND) ||
				!Shapes.joinIsNotEmpty(world.getBlockState(pos.relative(dir)).getBlockSupportShape(world,pos.relative(dir)).getFaceShape(dir.getOpposite()), checkBoxZ, BooleanOp.ONLY_SECOND) ||
				world.getBlockState(pos.relative(dir)).isFaceSturdy(world, pos, dir.getOpposite(), SupportType.CENTER) ||
                hasGirder(world, pos, state);
	}

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, world, pos, oldState, isMoving);
        Direction dir = state.getValue(FACING);
        BlockState under = world.getBlockState(pos.relative(dir));
        if(under.getBlock() == MechanoBlocks.COUPLING_NODE.get()) {
            world.setBlock(pos, state.setValue(MODEL_TYPE, HeapConnectorModelType.COUPLED), Block.UPDATE_ALL);
            return;
        }

        if(under.getBlock() == MechanoBlocks.TRANSMISSION_NODE.get()) {
            BlockState newLargeConnector = MechanoBlocks.HEAP_CONNECTOR_STACKED.get().defaultBlockState()
                .setValue(HeapConnectorStackedBlock.ORIENTATION, ComplexDirection.combine(
                    under.getValue(TransmissionNodeBlock.ORIENTATION).getCardinal().getOpposite(),
                    under.getValue(TransmissionNodeBlock.ORIENTATION).getOrient()
                ));
            world.setBlock(pos, newLargeConnector, Block.UPDATE_ALL);
            return;
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return BASE_SHAPE.get(state.getValue(FACING).getOpposite());
    }

    @Override
    public Class<HeapConnectorBlockEntity> getBlockEntityClass() {
        return HeapConnectorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends HeapConnectorBlockEntity> getBlockEntityType() {
        return MechanoBlockEntities.HEAP_CONNECTOR.get();
    }
    
    @Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction dir = context.getClickedFace().getOpposite();	
        BlockState out = this.defaultBlockState().setValue(FACING, dir);
        BlockPos pos = context.getClickedPos();
        if(context.getLevel().getBlockState(pos.relative(dir)).getBlock() == AllBlocks.METAL_GIRDER.get())
            return out.setValue(MODEL_TYPE, HeapConnectorModelType.GIRDERED);
		return out.setValue(MODEL_TYPE, HeapConnectorModelType.BASE);
	}

    
    private boolean hasGirder(LevelReader world, BlockPos pos, BlockState state) {
        Direction dir = state.getValue(FACING);
        return world.getBlockState(pos.relative(dir)).getBlock() == AllBlocks.METAL_GIRDER.get();
    }


    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos,
            boolean pIsMoving) {
        BlockEntity tileentity = state.hasBlockEntity() ? world.getBlockEntity(pos) : null;
		if(tileentity != null)
			if(tileentity instanceof HeapConnectorBlockEntity) ((HeapConnectorBlockEntity)tileentity).updateCache();

        if (!state.canSurvive(world, pos)) {
            world.destroyBlock(pos, true); 
            for (Direction direction : Direction.values())
                world.updateNeighborsAt(pos.relative(direction), this);
        }
    }


    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(FACING, MODEL_TYPE);
        super.createBlockStateDefinition(builder);
    }
}
