package com.quattage.experimental_tables.content.block;

import java.util.Locale;

import com.mrh0.createaddition.shapes.CAShapes;
import com.quattage.experimental_tables.content.block.entity.InductorBlockEntity;
import com.quattage.experimental_tables.registry.ModBlockEntities;
import com.quattage.experimental_tables.registry.ModBlocks;
import com.simibubi.create.content.contraptions.wrench.IWrenchable;
import com.simibubi.create.foundation.block.ITE;
import com.simibubi.create.foundation.utility.VoxelShaper;

import io.github.fabricators_of_create.porting_lib.block.NeighborChangeListeningBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;

public class InductorBlock extends BlockWithEntity implements ITE<InductorBlockEntity>, IWrenchable, NeighborChangeListeningBlock {

    public static final EnumProperty<InductorBlockModelType> MODEL_TYPE = EnumProperty.of("model", InductorBlockModelType.class);
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;

    public static final VoxelShaper MAIN_SHAPE = CAShapes.shape(0.0, 2.0, 0.0, 16.0, 14.0, 13.0).add(3.5, 2.0, -12.8, 15.5, 14.0, 0).forDirectional();

    public enum InductorBlockModelType implements StringIdentifiable {
        BASE, DUMMY;

        @Override
        public String asString() {
            return name().toLowerCase(Locale.ROOT);
        }

        @Override
        public String toString() {
            return asString();
        }
    }

    public InductorBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(MODEL_TYPE, InductorBlockModelType.BASE));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, net.minecraft.world.BlockView world, BlockPos pos, ShapeContext context) {
        return MAIN_SHAPE.get(state.get(FACING).rotateYCounterclockwise());
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState().with(FACING, context.getPlayerFacing());
    }

    @Override
    public void onNeighborChange(BlockState state, WorldView world, BlockPos pos, BlockPos sourcePos) {
        // TODO haha
    }

    @Override
    public Class<InductorBlockEntity> getTileEntityClass() {
        return InductorBlockEntity.class;
    }

    @Override
	public BlockEntityType<? extends InductorBlockEntity> getTileEntityType() {
		return ModBlockEntities.INDUCTOR.get();
	}
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, MODEL_TYPE);
    }

    @Override
    public float getAmbientOcclusionLightLevel(BlockState state, BlockView view, BlockPos pos) {
        return 1;
    }

    @Override
    public boolean hasSidedTransparency(BlockState state) {
        return true;
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
    public boolean isSideInvisible(BlockState state, BlockState stateFrom, Direction direction) {
        return true;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.INDUCTOR.get().instantiate(pos, state);
    }
}
