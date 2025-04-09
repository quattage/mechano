package com.quattage.mechano;

import com.quattage.mechano.content.block.power.alternator.rotor.BigRotorBlockEntity;
import com.quattage.mechano.content.block.power.alternator.rotor.SmallRotorBlockEntity;
import com.quattage.mechano.content.block.power.alternator.slipRingShaft.SlipRingShaftBlock;
import com.quattage.mechano.content.block.power.alternator.slipRingShaft.SlipRingShaftBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MechanoDynamicResources {
    public static SimpleBlockEntityVisualizer.Factory<SmallRotorBlockEntity> ofSmallRotor() {
        return (ctx, be, pt) -> {
            BlockState state = be.getBlockState();
            Direction.Axis axis = state.getValue(RotatedPillarKineticBlock.AXIS);
            Direction forward  = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
            Direction backward = forward.getOpposite();
            Level world = be.getLevel();
            BlockPos pos = be.getBlockPos();
            boolean hasFront = world.getBlockEntity(pos.relative(forward))  instanceof SmallRotorBlockEntity;
            boolean hasRear  = world.getBlockEntity(pos.relative(backward)) instanceof SmallRotorBlockEntity;

            PartialModel model;
            if (hasFront && hasRear) {
                model = MechanoPartialModels.SMALL_ROTOR_MIDDLE;
            } else if (hasFront) {
                model = MechanoPartialModels.SMALL_ROTOR_END_A;
            } else if (hasRear) {
                model = MechanoPartialModels.SMALL_ROTOR_END_B;
            } else {
                model = MechanoPartialModels.SMALL_ROTOR_SINGLE;
            }

            return new SingleAxisRotatingVisual<>(ctx, be, pt, Models.partial(model));
        };
    }

    public static SimpleBlockEntityVisualizer.Factory<BigRotorBlockEntity> ofBigRotor() {
        return (ctx, be, pt) -> {
            BlockState state = be.getBlockState();
            Direction.Axis axis = state.getValue(RotatedPillarKineticBlock.AXIS);
            Direction forward  = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
            Direction backward = forward.getOpposite();
            Level world = be.getLevel();
            BlockPos pos = be.getBlockPos();
            boolean hasFront = world.getBlockEntity(pos.relative(forward))  instanceof BigRotorBlockEntity;
            boolean hasRear  = world.getBlockEntity(pos.relative(backward)) instanceof BigRotorBlockEntity;

            PartialModel model;
            if (hasFront && hasRear) {
                model = MechanoPartialModels.BIG_ROTOR_MIDDLE;
            } else if (hasFront) {
                model = MechanoPartialModels.BIG_ROTOR_END_A;
            } else if (hasRear) {
                model = MechanoPartialModels.BIG_ROTOR_END_B;
            } else {
                model = MechanoPartialModels.BIG_ROTOR_SINGLE;
            }

            return new SingleAxisRotatingVisual<>(ctx, be, pt, Models.partial(model));
        };
    }


    public static SimpleBlockEntityVisualizer.Factory<SlipRingShaftBlockEntity> ofSlipRingShaft() {
        return (ctx, be, pt) -> {
            BlockState state = be.getBlockState();
            Direction facing = state.getValue(SlipRingShaftBlock.FACING);
            boolean mirrored = facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE;

            SlipRingShaftBlock.CollectorBlockModelType type = state.getValue(SlipRingShaftBlock.MODEL_TYPE);
            PartialModel model = switch (type) {
                case BASE -> MechanoPartialModels.SLIP_RING_SHAFT_BASE;
                case ROTORED -> mirrored
                        ? MechanoPartialModels.SLIP_RING_SHAFT_ROTORED_MIRRORED
                        : MechanoPartialModels.SLIP_RING_SHAFT_ROTORED;
                case BIG_ROTORED -> mirrored
                        ? MechanoPartialModels.SLIP_RING_SHAFT_BIG_ROTORED_MIRRORED
                        : MechanoPartialModels.SLIP_RING_SHAFT_BIG_ROTORED;
            };

            return new SingleAxisRotatingVisual<>(ctx, be, pt, Models.partial(model));
        };
    }
}