package com.quattage.mechano.content.block.power.transfer.voltometer;

import java.util.Locale;

import com.mrh0.createaddition.shapes.CAShapes;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.registry.MechanoBlockEntities;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class VoltometerBlock extends HorizontalDirectionalBlock implements IBE<VoltometerBlockEntity>, IWrenchable {
    
    public static final EnumProperty<VoltometerModelType> MODEL_TYPE = EnumProperty.create("model", VoltometerModelType.class);

    public static final VoxelShaper BASE = CAShapes
        .shape(1, 0, 1, 15, 2, 15)
        .add(2, 2, 2, 14, 14, 14)
        .add(-2.75, 4, 4, 18.75, 12, 12)
        .add(-4, 5.5, 5.5, 20, 10.5, 10.5)
        .forHorizontal(Direction.NORTH);

    public static final VoxelShaper SIDE = CAShapes
        .shape(1, 1, 0, 15, 15, 2)
        .add(2, 2, 2, 14, 14, 14)
        .add(-2.75, 4, 4, 18.75, 12, 12)
        .add(-4, 5.5, 5.5, 20, 10.5, 10.5)
        .forHorizontal(Direction.NORTH);

    public static final VoxelShaper INVERTED = CAShapes
        .shape(1, 14, 1, 15, 16, 15)
        .add(2, 2, 2, 14, 14, 14)
        .add(-2.75, 4, 4, 18.75, 12, 12)
        .add(-4, 5.5, 5.5, 20, 10.5, 10.5)
        .forHorizontal(Direction.NORTH);

    public enum VoltometerModelType implements StringRepresentable {
        BASE, SIDE, INVERTED;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    
        @Override
        public String toString() {
            return getSerializedName();
        }

        public static VoltometerModelType cycle(VoltometerModelType in) {
            int pos = in.ordinal();
            if(pos % 2 == 0) pos += 1;
            else pos -= 1;
            return VoltometerModelType.values()[pos];
        }
    }

    public VoltometerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(defaultBlockState()
            .setValue(FACING, Direction.NORTH)
            .setValue(MODEL_TYPE, VoltometerModelType.BASE));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        if(state.getValue(MODEL_TYPE) == VoltometerModelType.SIDE)
            return SIDE.get(facing.getOpposite());
        if(state.getValue(MODEL_TYPE) == VoltometerModelType.INVERTED)
            return INVERTED.get(facing);
        return BASE.get(facing);
    }

    @Override
    public Class<VoltometerBlockEntity> getBlockEntityClass() {
        return VoltometerBlockEntity.class;
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        return super.use(state, world, pos, player, hand, hit);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        BlockState out = defaultBlockState();

        if(context.getPlayer().isCrouching()) facing = facing.getOpposite(); 

        if(facing == Direction.UP) {
            facing = context.getHorizontalDirection().getOpposite();
            return out.setValue(FACING, facing).setValue(MODEL_TYPE, VoltometerModelType.BASE);
        }
        if(facing == Direction.DOWN) {
            facing = context.getHorizontalDirection().getOpposite();
            return out.setValue(FACING, facing).setValue(MODEL_TYPE, VoltometerModelType.INVERTED);
        }
        return out.setValue(FACING, facing).setValue(MODEL_TYPE, VoltometerModelType.SIDE);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING).add(MODEL_TYPE);
    }

    @Override
    public BlockEntityType<? extends VoltometerBlockEntity> getBlockEntityType() {
        return MechanoBlockEntities.VOLTOMETER.get();
    }
    
}
