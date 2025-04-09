package com.quattage.mechano;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

public class MechanoPartialModels {
    public static final PartialModel SMALL_ROTOR_SINGLE = PartialModel.of(
            new ResourceLocation(Mechano.MOD_ID, "block/rotor/small_rotor/single"));
    public static final PartialModel SMALL_ROTOR_MIDDLE = PartialModel.of(
            new ResourceLocation(Mechano.MOD_ID, "block/rotor/small_rotor/middle"));
    public static final PartialModel SMALL_ROTOR_END_A = PartialModel.of(
            new ResourceLocation(Mechano.MOD_ID, "block/rotor/small_rotor/end_a"));
    public static final PartialModel SMALL_ROTOR_END_B = PartialModel.of(
            new ResourceLocation(Mechano.MOD_ID, "block/rotor/small_rotor/end_b"));
    public static final PartialModel BIG_ROTOR_SINGLE = PartialModel.of(
            new ResourceLocation(Mechano.MOD_ID, "block/rotor/big_rotor/single"));
    public static final PartialModel BIG_ROTOR_MIDDLE = PartialModel.of(
            new ResourceLocation(Mechano.MOD_ID, "block/rotor/big_rotor/middle"));
    public static final PartialModel BIG_ROTOR_END_A = PartialModel.of(
            new ResourceLocation(Mechano.MOD_ID, "block/rotor/big_rotor/end_a"));
    public static final PartialModel BIG_ROTOR_END_B = PartialModel.of(
            new ResourceLocation(Mechano.MOD_ID, "block/rotor/big_rotor/end_b"));
    public static final PartialModel SLIP_RING_SHAFT_BASE = PartialModel.of(
            new ResourceLocation(Mechano.MOD_ID, "block/slip_ring_shaft/base"));
    public static final PartialModel SLIP_RING_SHAFT_ROTORED = PartialModel.of(
            new ResourceLocation(Mechano.MOD_ID, "block/slip_ring_shaft/rotored"));
    public static final PartialModel SLIP_RING_SHAFT_ROTORED_MIRRORED = PartialModel.of(
            new ResourceLocation(Mechano.MOD_ID, "block/slip_ring_shaft/rotored_mirrored"));
    public static final PartialModel SLIP_RING_SHAFT_BIG_ROTORED = PartialModel.of(
            new ResourceLocation(Mechano.MOD_ID, "block/slip_ring_shaft/big_rotored"));
    public static final PartialModel SLIP_RING_SHAFT_BIG_ROTORED_MIRRORED = PartialModel.of(
            new ResourceLocation(Mechano.MOD_ID, "block/slip_ring_shaft/big_rotored_mirrored"));
    public static void load() {}
}
