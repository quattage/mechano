package com.quattage.mechano.content.block.Alternator.Rotor;

import java.util.Locale;

import com.quattage.mechano.registry.MechanoBlockEntities;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class RotorBlock extends RotatedPillarKineticBlock implements IBE<RotorBlockEntity> {

    public static final EnumProperty<WideBlockModelType> MODEL_TYPE = EnumProperty.create("model", WideBlockModelType.class);

    public enum WideBlockModelType implements StringRepresentable {
        SINGLE, MIDDLE, END;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        @Override
        public String toString() {
            return getSerializedName();
        }
    }

    @Override
    public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        
        
    }

    public RotorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

    @Override
    public Class<RotorBlockEntity> getBlockEntityClass() {
        return RotorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RotorBlockEntity> getBlockEntityType() {
        return MechanoBlockEntities.ROTOR.get();
    }    
}
