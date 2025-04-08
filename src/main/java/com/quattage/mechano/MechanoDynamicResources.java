package com.quattage.mechano;

import com.quattage.mechano.content.block.power.alternator.rotor.AbstractRotorBlock;
import com.quattage.mechano.content.block.power.alternator.rotor.BigRotorBlockEntity;
import com.quattage.mechano.content.block.power.alternator.rotor.SmallRotorBlockEntity;
import com.quattage.mechano.content.block.power.alternator.slipRingShaft.SlipRingShaftBlock;
import com.quattage.mechano.content.block.power.alternator.slipRingShaft.SlipRingShaftBlockEntity;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.minecraft.world.level.block.state.BlockState;

public class MechanoDynamicResources {
    public static SimpleBlockEntityVisualizer.Factory<SmallRotorBlockEntity> ofSmallRotor() {
        return (ctx, be, pt) -> {
            BlockState state = be.getBlockState();
            AbstractRotorBlock.RotorModelType type = state.getValue(AbstractRotorBlock.MODEL_TYPE);
            PartialModel model = switch (type) {
                case SINGLE -> MechanoPartialModels.SMALL_ROTOR_SINGLE;
                case MIDDLE -> MechanoPartialModels.SMALL_ROTOR_MIDDLE;
                case END_A -> MechanoPartialModels.SMALL_ROTOR_END_A;
                case END_B -> MechanoPartialModels.SMALL_ROTOR_END_B;
            };
            return new SingleAxisRotatingVisual<>(ctx, be, pt, Models.partial(model));
        };
    }

    public static SimpleBlockEntityVisualizer.Factory<BigRotorBlockEntity> ofBigRotor() {
        return (ctx, be, pt) -> {
            BlockState state = be.getBlockState();
            AbstractRotorBlock.RotorModelType type = state.getValue(AbstractRotorBlock.MODEL_TYPE);
            PartialModel model = switch (type) {
                case SINGLE -> MechanoPartialModels.BIG_ROTOR_SINGLE;
                case MIDDLE -> MechanoPartialModels.BIG_ROTOR_MIDDLE;
                case END_A -> MechanoPartialModels.BIG_ROTOR_END_A;
                case END_B -> MechanoPartialModels.BIG_ROTOR_END_B;
            };
            return new SingleAxisRotatingVisual<>(ctx, be, pt, Models.partial(model));
        };
    }

    public static SimpleBlockEntityVisualizer.Factory<SlipRingShaftBlockEntity> ofSlipRingShaft() {
        return (ctx, be, pt) -> {
            BlockState state = be.getBlockState();
            SlipRingShaftBlock.CollectorBlockModelType type = state.getValue(SlipRingShaftBlock.MODEL_TYPE);
            PartialModel model = switch (type) {
                case BASE -> MechanoPartialModels.SLIP_RING_SHAFT_BASE;
                case ROTORED -> MechanoPartialModels.SLIP_RING_SHAFT_ROTORED;
                case BIG_ROTORED -> MechanoPartialModels.SLIP_RING_SHAFT_BIG_ROTORED;
            };
            return new SingleAxisRotatingVisual<>(ctx, be, pt, Models.partial(model));
        };
    }
}