package com.quattage.mechano.content.block.power.alternator.rotor;

import com.quattage.mechano.content.block.power.alternator.collector.CollectorBlockEntity;
import com.quattage.mechano.content.block.power.alternator.stator.StatorBlock;
import com.quattage.mechano.content.block.power.alternator.stator.StatorBlockEntity;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.block.orientation.SimpleOrientation;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class RotorBlockEntity extends KineticBlockEntity {

    private final List<StatorBlockEntity> stators=new ArrayList<>();

    private CollectorBlockEntity collector;
    public RotorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        setLazyTickRate(20);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        if(clientPacket)
            updateStatorCount();
    }

    @Override
    public void initialize() {
        super.initialize();
        updateStatorCount();
    }

    @Override
	protected AABB createRenderBoundingBox() {
		return new AABB(worldPosition).inflate(1);
	}

    public void updateStatorCount() {
        Direction.Axis axis = getBlockState().getValue(RotorBlock.AXIS);

        BlockPos corner1 = calculateCorner(axis, 1);
        BlockPos corner2 = calculateCorner(axis, -1);

        updateStators(corner1, corner2);
    }

    private BlockPos calculateCorner(Direction.Axis axis, int offsetDirection) {
        return getBlockPos().offset(
                axis == Direction.Axis.X ? 0 : offsetDirection,
                axis == Direction.Axis.Y ? 0 : offsetDirection,
                axis == Direction.Axis.Z ? 0 : offsetDirection
        );
    }

    private void updateStators(BlockPos corner1, BlockPos corner2) {
        if (level == null)
            return;
        stators.clear();
        BlockPos.betweenClosed(corner1, corner2).forEach(pos -> {
            if (level.getBlockEntity(pos) instanceof StatorBlockEntity stator&&isValidStator(stator))
                stators.add(stator);
        });
        if(collector!=null)
            collector.updateRotorAndStatorCount();
        sendData();
    }

    private boolean isValidStator(StatorBlockEntity stator) {
        if(stator==null) return false;
        return hasSameAxis(stator) && isFacingRotor(stator);
    }

    private boolean hasSameAxis(StatorBlockEntity stator) {
        Direction.Axis statorAxis = DirectionTransformer.getForward(stator.getBlockState()).getAxis();
        Direction.Axis rotorAxis = DirectionTransformer.getForward(this.getBlockState()).getAxis();
        return statorAxis.equals(rotorAxis);
    }

    private boolean isFacingRotor(StatorBlockEntity stator) {
        StatorBlock.StatorBlockModelType modelType = stator.getBlockState().getValue(StatorBlock.MODEL_TYPE);
        SimpleOrientation orientation = stator.getBlockState().getValue(StatorBlock.ORIENTATION);
        BlockPos statorPos = stator.getBlockPos();
        Direction facing = orientation.getCardinal();
        Direction.Axis axis = orientation.getOrient();
        BlockPos rotorPos = cornerOffset(statorPos, facing, axis);

        if (modelType == StatorBlock.StatorBlockModelType.CORNER)
            return rotorPos.equals(getBlockPos());

        return statorPos.relative(orientation.getCardinal()).equals(getBlockPos());
    }

    private BlockPos cornerOffset(BlockPos statorPos, Direction facing, Direction.Axis axis) {
        BlockPos basePos = statorPos.relative(facing);
        int offset = DirectionTransformer.isPositive(facing) ? 1 : -1;
        return switch (facing.getAxis()) {
            case Z -> basePos.offset(0, offset, 0);
            case Y -> basePos.offset(axis== Direction.Axis.Z?-offset:0, 0, axis== Direction.Axis.X?-offset:0);
            case X -> basePos.offset(0, offset,0 );
        };
    }

    public void setCollector(CollectorBlockEntity collector) {
        this.collector = collector;
    }

    public int getStatorCount() {
        return stators.size();
    }
}
