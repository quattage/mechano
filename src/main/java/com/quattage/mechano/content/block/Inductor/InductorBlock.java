package com.quattage.mechano.content.block.Inductor;

import com.mrh0.createaddition.shapes.CAShapes;
import com.quattage.mechano.registry.MechanoBlockEntities;
import com.simibubi.create.content.contraptions.wrench.IWrenchable;
import com.simibubi.create.foundation.block.ITE;
import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class InductorBlock extends BaseEntityBlock implements ITE<InductorBlockEntity>, IWrenchable {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final VoxelShaper MAIN_SHAPE = CAShapes.shape(0, 0, 0, 0, 0, 0).forDirectional();

    public InductorBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        return MAIN_SHAPE.get(state.getValue(FACING).getCounterClockWise());
    }
    

    @Override

    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @SuppressWarnings("deprecation")  // TOOD investigate
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborChanged(state, level, pos, sourceBlock, sourcePos, notify);
        InductorBlockEntity inductor = (InductorBlockEntity) level.getBlockEntity(pos);
        if (inductor.getTargetPos().equals(sourcePos)) {
            String sourceID = String.valueOf(Registry.BLOCK.getId(sourceBlock));
            if(sourceID.equals(inductor.getTargetID())) {
                ((LevelWriter) level).destroyBlock(pos, true);
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        InductorBlockEntity inductor = (InductorBlockEntity) level.getBlockEntity(pos);
        BlockPos possy = pos.relative(state.getValue(HorizontalDirectionalBlock.FACING));
        inductor.setTargetID(level.getBlockState(possy));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        BlockEntity newBE = new InductorBlockEntity(MechanoBlockEntities.INDUCTOR.get(), pos, state);
        return newBE;
    }
}
