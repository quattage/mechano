package com.quattage.mechano.content.block.Inductor;

import java.util.List;

import com.mrh0.createaddition.shapes.CAShapes;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.registry.MechanoBlockEntities;
import com.simibubi.create.content.contraptions.wrench.IWrenchable;
import com.simibubi.create.foundation.block.ITE;
import com.simibubi.create.foundation.utility.VoxelShaper;

import io.github.fabricators_of_create.porting_lib.block.NeighborChangeListeningBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.ModifiableWorld;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class InductorBlock extends BlockWithEntity implements ITE<InductorBlockEntity>, IWrenchable {

    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    public static final VoxelShaper MAIN_SHAPE = CAShapes.shape(4.0, 3.0, 1.0, 11.0, 13.0, 3.0)
                                                .add(1.5, 2.0, -11.8, 13.5, 14, 1.3)
                                                .add(0.0, 3.0, 3.0, 16.0, 13.0, 13.0)
                                                .forDirectional();

    public InductorBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, net.minecraft.world.BlockView world, BlockPos pos, ShapeContext context) {
        return MAIN_SHAPE.get(state.get(FACING).rotateYCounterclockwise());
    }

    @Override

    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @SuppressWarnings("deprecation")  // TOOD investigate
    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        InductorBlockEntity inductor = (InductorBlockEntity) world.getBlockEntity(pos);
        if (inductor.getTargetPos().equals(sourcePos)) {
            String sourceID = Registry.BLOCK.getId(sourceBlock).toString();
            if(sourceID.equals(inductor.getTargetID())) {
                ((ModifiableWorld) world).breakBlock(pos, true);
            }
        }
    }

    @Override
    public Class<InductorBlockEntity> getTileEntityClass() {
        return InductorBlockEntity.class;
    }

    @Override
	public BlockEntityType<? extends InductorBlockEntity> getTileEntityType() {
		return MechanoBlockEntities.INDUCTOR.get();
	}

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rot) {
        return state.with(FACING, rot.rotate(state.get(FACING)));
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return true;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState().with(FACING, context.getPlayerFacing());
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        InductorBlockEntity inductor = (InductorBlockEntity) world.getBlockEntity(pos);
        BlockPos possy = pos.offset(state.get(HorizontalFacingBlock.FACING));
        inductor.setTargetID(world.getBlockState(possy));
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        BlockEntity newBE = new InductorBlockEntity(MechanoBlockEntities.INDUCTOR.get(), pos, state);
        return newBE;
    }
}
