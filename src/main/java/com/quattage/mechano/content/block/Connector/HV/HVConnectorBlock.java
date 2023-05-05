package com.quattage.mechano.content.block.Connector.HV;

import com.mrh0.createaddition.energy.IWireNode;
import com.mrh0.createaddition.shapes.CAShapes;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.registry.MechanoBlockEntities;
import com.simibubi.create.content.contraptions.wrench.IWrenchable;
import com.simibubi.create.foundation.block.ITE;
import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.state.StateManager.Builder;

public class HVConnectorBlock extends Block implements ITE<HVConnectorBlockEntity>, IWrenchable {
    public static final VoxelShaper CONNECTOR_SHAPE = CAShapes.shape(4, 0, 4, 12, 11, 12).forDirectional();
	public static final DirectionProperty FACING = Properties.FACING;
	private static final VoxelShape boxwe = Block.createCuboidShape(0,7,7,10,9,9);
	private static final VoxelShape boxsn = Block.createCuboidShape(7,7,0,9,9,10);
    
	public HVConnectorBlock(Settings properties) {
		super(properties);
		this.setDefaultState(this.getDefaultState().with(FACING, Direction.NORTH));
	}

	@Override
    public VoxelShape getOutlineShape(BlockState state, net.minecraft.world.BlockView world, BlockPos pos, ShapeContext context) {
        return CONNECTOR_SHAPE.get(state.get(FACING).getOpposite());
    }
	
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return MechanoBlockEntities.HIGH_VOLTAGE_CONNECTOR.create(pos, state);
	}

	@Override
	public Class<HVConnectorBlockEntity> getTileEntityClass() {
		return HVConnectorBlockEntity.class;
	}
	
	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}
	
	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		return this.getDefaultState().with(FACING, context.getSide().getOpposite());
	}

    @Override
	public void onBreak(World worldIn, BlockPos pos, BlockState state, PlayerEntity player) {
		super.onBreak(worldIn, pos, state, player);
		if(player.isCreative())
			return;
		BlockEntity te = worldIn.getBlockEntity(pos);
		if(te == null)
			return;
		if(!(te instanceof IWireNode))
			return;
		IWireNode cte = (IWireNode) te;
		
		cte.dropWires(worldIn);
	}

    @Override
	public ActionResult onSneakWrenched(BlockState state, ItemUsageContext c) {
		if(c.getPlayer().isCreative()) {
			Mechano.LOGGER.info("CREATIVE");
			return IWrenchable.super.onSneakWrenched(state, c);
		}
		BlockEntity te = c.getWorld().getBlockEntity(c.getBlockPos());
		if(te == null) {
			Mechano.LOGGER.info("BLOCKENTITY IS NULL");
			return IWrenchable.super.onSneakWrenched(state, c);
		}
		if(!(te instanceof IWireNode)) {
			Mechano.LOGGER.info("BLOCKENTITY IS NOT WIRENODE");
			return IWrenchable.super.onSneakWrenched(state, c);
		}
		IWireNode cte = (IWireNode) te;
		
		Mechano.LOGGER.info("WIRES DROPPED");
		cte.dropWires(c.getWorld(), c.getPlayer());
		return IWrenchable.super.onSneakWrenched(state, c);
	}

    @Override
	public void neighborUpdate(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
		BlockEntity tileentity = state.hasBlockEntity() ? worldIn.getBlockEntity(pos) : null;
		if(tileentity != null) {
			if(tileentity instanceof HVConnectorBlockEntity) {
				((HVConnectorBlockEntity)tileentity).updateCache();
			}
		}
		if (!state.canPlaceAt(worldIn, pos)) {
			dropStacks(state, worldIn, pos, tileentity);
			
			if(tileentity instanceof IWireNode)
				((IWireNode) tileentity).dropWires(worldIn);
			
			worldIn.removeBlock(pos, false);

			for (Direction direction : Direction.values())
				worldIn.updateNeighborsAlways(pos.offset(direction), this);
		}
	}

    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		Direction dir = state.get(FACING);
		return
				!VoxelShapes.matchesAnywhere(world.getBlockState(pos.offset(dir)).getSidesShape(world,pos.offset(dir)).getFace(dir.getOpposite()), boxwe, BooleanBiFunction.ONLY_SECOND) ||
				!VoxelShapes.matchesAnywhere(world.getBlockState(pos.offset(dir)).getSidesShape(world,pos.offset(dir)).getFace(dir.getOpposite()), boxsn, BooleanBiFunction.ONLY_SECOND) ||
				world.getBlockState(pos.offset(dir)).isSideSolidFullSquare(world, pos, dir.getOpposite());
	}

    @Override
	public BlockEntityType<? extends HVConnectorBlockEntity> getTileEntityType() {
		return MechanoBlockEntities.HIGH_VOLTAGE_CONNECTOR.get();
	}
	
	@Override
	public BlockState rotate(BlockState state, BlockRotation direction) {
		return state.with(FACING, direction.rotate(state.get(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.with(FACING, mirror.apply(state.get(FACING)));
	}

	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if(!world.isClient) {
			BlockEntity entity = world.getBlockEntity(pos);
			if(entity != null && !(newState.getBlock() instanceof HVConnectorBlock)) {
				if(entity instanceof HVConnectorBlockEntity castedEntity) {
					castedEntity.onBlockRemoved();
				}
			}
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}
}
