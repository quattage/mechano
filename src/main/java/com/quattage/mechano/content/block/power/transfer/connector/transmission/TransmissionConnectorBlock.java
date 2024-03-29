package com.quattage.mechano.content.block.power.transfer.connector.transmission;

import java.util.Locale;

import com.quattage.mechano.MechanoBlockEntities;
import com.quattage.mechano.MechanoBlocks;
import com.quattage.mechano.content.block.power.transfer.adapter.TransmissionNodeBlock;
import com.quattage.mechano.content.block.power.transfer.connector.transmission.stacked.ConnectorStackedTier0Block;
import com.quattage.mechano.core.block.CombinedOrientedBlock;
import com.quattage.mechano.core.block.DirectionTransformer;
import com.quattage.mechano.core.block.orientation.CombinedOrientation;
import com.quattage.mechano.core.util.ShapeBuilder;
import com.simibubi.create.AllBlocks;
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

public class TransmissionConnectorBlock extends DirectionalBlock implements IBE<TransmissionConnectorBlockEntity> {
    

    public static final EnumProperty<HeapConnectorModelType> MODEL_TYPE = EnumProperty.create("model", HeapConnectorModelType.class);
    public static final VoxelShaper BASE_SHAPE = ShapeBuilder.newShape(5.5, 0, 5.5, 10.5, 15, 10.5).defaultUp();

    private static final VoxelShape checkBoxX = Block.box(0,7,7,10,9,9);
    private static final VoxelShape checkBoxZ = Block.box(7,7,0,9,9,10);

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


    public TransmissionConnectorBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.defaultBlockState()
            .setValue(FACING, Direction.NORTH)
            .setValue(MODEL_TYPE, HeapConnectorModelType.BASE));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
		Direction dir = state.getValue(FACING);
        
		return hasSupport(world, pos, state) || world.getBlockState(pos.relative(dir.getOpposite())).isFaceSturdy(world, pos, dir, SupportType.CENTER) ||
			!Shapes.joinIsNotEmpty(world.getBlockState(pos.relative(dir)).getBlockSupportShape(world,pos.relative(dir)).getFaceShape(dir), checkBoxX, BooleanOp.ONLY_SECOND) ||
			!Shapes.joinIsNotEmpty(world.getBlockState(pos.relative(dir)).getBlockSupportShape(world,pos.relative(dir)).getFaceShape(dir), checkBoxZ, BooleanOp.ONLY_SECOND);
	}

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, world, pos, oldState, isMoving);
        Direction dir = state.getValue(FACING);
        BlockState under = world.getBlockState(pos.relative(dir.getOpposite()));
        if(under.getBlock() == MechanoBlocks.COUPLING_NODE.get()) {
            world.setBlock(pos, state.setValue(MODEL_TYPE, HeapConnectorModelType.COUPLED), 3);
            return;
        }

        if(under.getBlock() == MechanoBlocks.TRANSMISSION_NODE.get()) {
            if(DirectionTransformer.sharesLocalUp(state, under)) {
                CombinedOrientation facing = CombinedOrientation.convert(under.getValue(TransmissionNodeBlock.ORIENTATION));
                BlockState newLargeConnector = MechanoBlocks.CONNECTOR_STACKED_ZERO.get().defaultBlockState();
                world.setBlock(pos, newLargeConnector.setValue(ConnectorStackedTier0Block.ORIENTATION, facing), 3);
                return;
            }
        }
    }
    
    private Direction dirFromAxis(Direction.Axis ax) {
        if(ax == Direction.Axis.Y) return Direction.UP;
        if(ax == Direction.Axis.Z) return Direction.NORTH;
        return Direction.EAST;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return BASE_SHAPE.get(state.getValue(FACING));
    }

    @Override
    public Class<TransmissionConnectorBlockEntity> getBlockEntityClass() {
        return TransmissionConnectorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TransmissionConnectorBlockEntity> getBlockEntityType() {
        return MechanoBlockEntities.TRANSMISSION_CONNECTOR.get();
    }
    
    @Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction dir = context.getClickedFace();	
        BlockState out = this.defaultBlockState().setValue(FACING, dir);
        BlockPos pos = context.getClickedPos();
        if(context.getLevel().getBlockState(pos.relative(dir)).getBlock() == AllBlocks.METAL_GIRDER.get())
            return out.setValue(MODEL_TYPE, HeapConnectorModelType.GIRDERED);
		return out.setValue(MODEL_TYPE, HeapConnectorModelType.BASE);
	}

    private boolean hasSupport(LevelReader world, BlockPos pos, BlockState state) {
        Direction dir = state.getValue(FACING);
        Block behind = world.getBlockState(pos.relative(dir.getOpposite())).getBlock();
        return behind == AllBlocks.METAL_GIRDER.get() || 
            behind ==  MechanoBlocks.COUPLING_NODE.get() ||
            behind == MechanoBlocks.TRANSMISSION_NODE.get();
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos,
            boolean pIsMoving) {

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
